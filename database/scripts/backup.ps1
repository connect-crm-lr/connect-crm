# Backup do banco do Connect CRM (RNF-004) - Windows / PowerShell.
#
# Gera um arquivo .sql com todos os dados atuais em database\backups\,
# com data e hora no nome (ex.: connect_crm_2026-10-26_1530.sql).
#
# Uso (com o container connect-crm-db rodando):
#   powershell -ExecutionPolicy Bypass -File .\database\scripts\backup.ps1

. (Join-Path $PSScriptRoot 'comum.ps1')

$banco   = Ler-Env 'POSTGRES_DB'
$usuario = Ler-Env 'POSTGRES_USER'
Verificar-Banco $usuario $banco

$nome          = "{0}_{1}.sql" -f $banco, (Get-Date -Format 'yyyy-MM-dd_HHmm')
$destino       = Join-Path $DirBackups $nome
$tempContainer = "/tmp/$nome"

New-Item -ItemType Directory -Force -Path $DirBackups | Out-Null
if (Test-Path -LiteralPath $destino) {
    Sair-ComErro "ja existe um backup com o nome $nome. Aguarde um minuto e rode de novo."
}

Write-Host "Gerando backup do banco '$banco' (container $Container)..."

try {
    # O pg_dump roda DENTRO do container e conecta pelo socket local, que nao pede senha:
    # por isso a senha do .env nao e usada nem aparece aqui.
    #   --clean --if-exists         o arquivo apaga cada objeto antes de recria-lo, entao a
    #                               restauracao substitui o que estiver no banco.
    #   --no-owner --no-privileges  o backup nao fica preso ao usuario que o gerou.
    # O arquivo e gerado dentro do container e copiado com "docker cp": redirecionar a saida
    # com ">" no PowerShell trocaria a codificacao e estragaria os acentos.
    docker exec $Container pg_dump -U $usuario -d $banco --clean --if-exists --no-owner --no-privileges --encoding=UTF8 -f $tempContainer
    if ($LASTEXITCODE -ne 0) { Sair-ComErro "o pg_dump falhou. Veja a mensagem acima." }

    docker cp "${Container}:$tempContainer" $destino | Out-Null
    if ($LASTEXITCODE -ne 0) { Sair-ComErro "nao foi possivel copiar o backup para $destino." }
}
finally {
    # Apaga o arquivo temporario de dentro do container, mesmo se algo der errado no meio.
    docker exec $Container rm -f $tempContainer | Out-Null
}

$tamanho = (Get-Item -LiteralPath $destino).Length
if ($tamanho -eq 0) { Sair-ComErro "o arquivo de backup ficou vazio: $destino" }

Write-Host ("Backup concluido: database\backups\{0} ({1:N0} KB)" -f $nome, [math]::Ceiling($tamanho / 1KB)) -ForegroundColor Green
