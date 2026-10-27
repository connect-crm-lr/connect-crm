# Funcoes usadas pelos scripts backup.ps1 e restaurar.ps1 (Windows / PowerShell).
# Este arquivo nao e executado sozinho: ele e carregado com ". comum.ps1".
# Os textos ficam sem acento para o Windows PowerShell 5.1 ler o arquivo sem erro de codificacao.

# Nome do container definido no database/docker-compose.yml.
$Container   = 'connect-crm-db'
$DirDatabase = Split-Path -Parent $PSScriptRoot
$ArquivoEnv  = Join-Path $DirDatabase '.env'
$DirBackups  = Join-Path $DirDatabase 'backups'

function Sair-ComErro([string]$Mensagem) {
    Write-Host "ERRO: $Mensagem" -ForegroundColor Red
    exit 1
}

# Le uma variavel do database/.env.
# Ignora linhas vazias e comentarios, aceita espacos antes do nome e aspas no valor.
function Ler-Env([string]$Chave) {
    if (-not (Test-Path -LiteralPath $ArquivoEnv)) {
        Sair-ComErro "arquivo $ArquivoEnv nao encontrado. Crie-o a partir do database/.env.example."
    }
    $valor = $null
    foreach ($linha in Get-Content -LiteralPath $ArquivoEnv -Encoding UTF8) {
        $texto = $linha.Trim()
        if ($texto -eq '' -or $texto.StartsWith('#')) { continue }
        $i = $texto.IndexOf('=')
        if ($i -lt 1) { continue }
        if ($texto.Substring(0, $i).Trim() -eq $Chave) {
            $valor = $texto.Substring($i + 1).Trim().Trim('"').Trim("'")
        }
    }
    if ([string]::IsNullOrEmpty($valor)) {
        Sair-ComErro "a variavel $Chave nao esta definida em $ArquivoEnv."
    }
    return $valor
}

# Confere se o Docker responde, se o container esta rodando e se o banco aceita conexoes.
function Verificar-Banco([string]$Usuario, [string]$Banco) {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Sair-ComErro "comando 'docker' nao encontrado. O Docker Desktop esta aberto?"
    }
    # Erros de comandos externos nao devem interromper o script aqui: o codigo de saida e conferido abaixo.
    $preferencia = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $rodando = docker inspect -f '{{.State.Running}}' $Container 2>$null
    $ErrorActionPreference = $preferencia
    if ($rodando -ne 'true') {
        Sair-ComErro "o container $Container nao esta rodando. Suba o banco com 'docker compose up -d' na pasta database/."
    }
    docker exec $Container pg_isready -q -U $Usuario -d $Banco
    if ($LASTEXITCODE -ne 0) {
        Sair-ComErro "o banco ainda nao esta aceitando conexoes. Aguarde alguns segundos e tente de novo."
    }
}
