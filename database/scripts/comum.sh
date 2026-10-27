# Funções usadas pelos scripts backup.sh e restaurar.sh (Linux / WSL).
# Este arquivo não é executado sozinho: ele é carregado com "source".

# Nome do container definido no database/docker-compose.yml.
CONTAINER="connect-crm-db"

DIR_SCRIPTS="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIR_DATABASE="$(dirname "$DIR_SCRIPTS")"
ARQUIVO_ENV="$DIR_DATABASE/.env"
DIR_BACKUPS="$DIR_DATABASE/backups"

erro() {
  echo "ERRO: $*" >&2
  exit 1
}

# Lê uma variável do database/.env.
# O arquivo é lido linha a linha (não é executado com "source"), aceita espaços
# antes do nome, aspas no valor e quebra de linha do Windows (CRLF).
ler_env() {
  local chave="$1" linha valor
  [ -f "$ARQUIVO_ENV" ] || erro "arquivo $ARQUIVO_ENV não encontrado. Crie-o a partir do database/.env.example."
  linha="$(grep -E "^[[:space:]]*${chave}[[:space:]]*=" "$ARQUIVO_ENV" | tail -n 1 || true)"
  valor="$(printf '%s' "${linha#*=}" | tr -d '\r' \
    | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' -e 's/^"\(.*\)"$/\1/' -e "s/^'\(.*\)'$/\1/")"
  [ -n "$valor" ] || erro "a variável $chave não está definida em $ARQUIVO_ENV."
  printf '%s' "$valor"
}

# Confere se o Docker responde, se o container está rodando e se o banco aceita conexões.
verificar_banco() {
  local usuario="$1" banco="$2" rodando
  command -v docker >/dev/null 2>&1 || erro "comando 'docker' não encontrado."
  rodando="$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null || true)"
  [ "$rodando" = "true" ] || erro "o container $CONTAINER não está rodando. Suba o banco com 'docker compose up -d' na pasta database/."
  docker exec "$CONTAINER" pg_isready -q -U "$usuario" -d "$banco" \
    || erro "o banco ainda não está aceitando conexões. Aguarde alguns segundos e tente de novo."
}
