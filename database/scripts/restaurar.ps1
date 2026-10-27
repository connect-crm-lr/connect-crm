# Restauracao do banco do Connect CRM a partir de um backup (RNF-004) - Windows / PowerShell.
#
# ATENCAO: a restauracao SOBRESCREVE os dados atuais do banco.
#
# Uso (com o container connect-crm-db rodando):
#   powershell -ExecutionPolicy Bypass -File .\database\scripts\restaurar.ps1 <arquivo.sql> [-Sim]
#
#   <arquivo.sql>  caminho do backup, ou so o nome de um arquivo de database\backups\
#   -Sim           nao pede confirmacao

param(
    [Parameter(Position = 0)][string]$Arquivo,
    [switch]$Sim
)

. (Join-Path $PSScriptRoot 'comum.ps1')

if ([string]::IsNullOrWhiteSpace($Arquivo)) {
    Write-Host "Uso: powershell -ExecutionPolicy Bypass -File .\database\scripts\restaurar.ps1 <arquivo.sql> [-Sim]"
    Write-Host ""
    Write-Host "Backups disponiveis em database\backups\ (mais recente primeiro):"
    if (Test-Path -LiteralPath $DirBackups) {
        Get-ChildItem -LiteralPath $DirBackups -Filter '*.sql' | Sort-Object LastWriteTime -Descending | ForEach-Object { $_.Name }
    }
    exit 1
}

# Aceita so o nome do arquivo quando ele esta em database\backups\.
if (-not (Test-Path -LiteralPath $Arquivo -PathType Leaf) -and (Test-Path -LiteralPath (Join-Path $DirBackups $Arquivo) -PathType Leaf)) {
    $Arquivo = Join-Path $DirBackups $Arquivo
}
if (-not (Test-Path -LiteralPath $Arquivo -PathType Leaf)) { Sair-ComErro "arquivo de backup nao encontrado: $Arquivo" }
$Arquivo = (Resolve-Path -LiteralPath $Arquivo).Path
if ((Get-Item -LiteralPath $Arquivo).Length -eq 0) { Sair-ComErro "o arquivo de backup esta vazio: $Arquivo" }
if (-not (Select-String -LiteralPath $Arquivo -Pattern 'PostgreSQL database dump' -SimpleMatch -Quiet)) {
    Sair-ComErro "o arquivo nao parece ser um backup gerado pelo pg_dump: $Arquivo"
}

$banco   = Ler-Env 'POSTGRES_DB'
$usuario = Ler-Env 'POSTGRES_USER'
Verificar-Banco $usuario $banco

# Com o back-end ou o pgAdmin conectados, a restauracao ficaria parada esperando
# as tabelas serem liberadas. Por isso ela so roda sem outras conexoes no banco.
$consulta = "SELECT COALESCE(NULLIF(application_name, ''), 'sem nome') FROM pg_stat_activity " +
            "WHERE datname = current_database() AND backend_type = 'client backend' AND pid <> pg_backend_pid()"
$conexoes = @(docker exec $Container psql -U $usuario -d $banco -At -c $consulta)
if ($LASTEXITCODE -ne 0) { Sair-ComErro "nao foi possivel consultar as conexoes abertas no banco." }
$conexoes = @($conexoes | Where-Object { $_ -ne '' })
if ($conexoes.Count -gt 0) {
    Write-Host "Ha outras conexoes abertas no banco '$banco':" -ForegroundColor Yellow
    $conexoes | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
    Sair-ComErro "pare o back-end e feche o pgAdmin (ou outra ferramenta) antes de restaurar."
}

Write-Host "Backup:  $Arquivo"
Write-Host "Destino: banco '$banco' no container $Container"
Write-Host "ATENCAO: todos os dados atuais do banco serao SUBSTITUIDOS pelos dados do backup." -ForegroundColor Yellow
if (-not $Sim) {
    $resposta = Read-Host "Digite SIM para continuar"
    if ($resposta -cne 'SIM') {
        Write-Host "Restauracao cancelada. Nada foi alterado."
        exit 1
    }
}

$tempContainer = "/tmp/restaurar_" + (Split-Path -Leaf $Arquivo)

Write-Host "Restaurando..."
try {
    # O arquivo e copiado para dentro do container com "docker cp": passar o conteudo
    # por pipe no PowerShell trocaria a codificacao e estragaria os acentos.
    docker cp $Arquivo "${Container}:$tempContainer" | Out-Null
    if ($LASTEXITCODE -ne 0) { Sair-ComErro "nao foi possivel copiar o backup para o container." }

    # --single-transaction + ON_ERROR_STOP: se qualquer comando falhar, nada e gravado
    # e o banco continua como estava antes.
    # A saida normal (SET, CREATE TABLE...) e descartada; os erros continuam aparecendo.
    docker exec $Container psql -U $usuario -d $banco -q -v ON_ERROR_STOP=1 --single-transaction -f $tempContainer | Out-Null
    if ($LASTEXITCODE -ne 0) { Sair-ComErro "a restauracao falhou e foi desfeita; o banco continua como estava antes." }
}
finally {
    # Apaga o arquivo temporario de dentro do container, mesmo se algo der errado no meio.
    docker exec $Container rm -f $tempContainer | Out-Null
}

Write-Host "Restauracao concluida." -ForegroundColor Green
