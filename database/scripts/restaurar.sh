#!/usr/bin/env bash
# Restauração do banco do Connect CRM a partir de um backup (RNF-004) — Linux / WSL.
#
# ATENÇÃO: a restauração SOBRESCREVE os dados atuais do banco.
#
# Uso (com o container connect-crm-db rodando):
#   bash database/scripts/restaurar.sh <arquivo.sql> [--sim]
#
#   <arquivo.sql>  caminho do backup, ou só o nome de um arquivo de database/backups/
#   --sim          não pede confirmação
set -euo pipefail

# shellcheck source=database/scripts/comum.sh
source "$(dirname "${BASH_SOURCE[0]}")/comum.sh"

ARQUIVO=""
CONFIRMADO="nao"
for arg in "$@"; do
  case "$arg" in
    --sim) CONFIRMADO="sim" ;;
    -*) erro "opção desconhecida: $arg" ;;
    *) ARQUIVO="$arg" ;;
  esac
done

if [ -z "$ARQUIVO" ]; then
  echo "Uso: bash database/scripts/restaurar.sh <arquivo.sql> [--sim]"
  echo
  echo "Backups disponíveis em database/backups/ (mais recente primeiro):"
  ls -1t "$DIR_BACKUPS"/*.sql 2>/dev/null | xargs -r -n1 basename || true
  exit 1
fi

# Aceita só o nome do arquivo quando ele está em database/backups/.
if [ ! -f "$ARQUIVO" ] && [ -f "$DIR_BACKUPS/$ARQUIVO" ]; then
  ARQUIVO="$DIR_BACKUPS/$ARQUIVO"
fi
[ -f "$ARQUIVO" ] || erro "arquivo de backup não encontrado: $ARQUIVO"
[ -s "$ARQUIVO" ] || erro "o arquivo de backup está vazio: $ARQUIVO"
grep -q -m 1 "PostgreSQL database dump" "$ARQUIVO" \
  || erro "o arquivo não parece ser um backup gerado pelo pg_dump: $ARQUIVO"

BANCO="$(ler_env POSTGRES_DB)"
USUARIO="$(ler_env POSTGRES_USER)"
verificar_banco "$USUARIO" "$BANCO"

# Com o back-end ou o pgAdmin conectados, a restauração ficaria parada esperando
# as tabelas serem liberadas. Por isso ela só roda sem outras conexões no banco.
CONEXOES="$(docker exec "$CONTAINER" psql -U "$USUARIO" -d "$BANCO" -At -c \
  "SELECT COALESCE(NULLIF(application_name, ''), 'sem nome') FROM pg_stat_activity
   WHERE datname = current_database() AND backend_type = 'client backend'
     AND pid <> pg_backend_pid()")"
if [ -n "$CONEXOES" ]; then
  echo "Há outras conexões abertas no banco '$BANCO':" >&2
  printf '%s\n' "$CONEXOES" | sed 's/^/  - /' >&2
  erro "pare o back-end e feche o pgAdmin (ou outra ferramenta) antes de restaurar."
fi

echo "Backup:  $ARQUIVO"
echo "Destino: banco '$BANCO' no container $CONTAINER"
echo "ATENÇÃO: todos os dados atuais do banco serão SUBSTITUÍDOS pelos dados do backup."
if [ "$CONFIRMADO" != "sim" ]; then
  read -r -p "Digite SIM para continuar: " resposta
  [ "$resposta" = "SIM" ] || { echo "Restauração cancelada. Nada foi alterado."; exit 1; }
fi

TEMP_CONTAINER="/tmp/restaurar_$(basename "$ARQUIVO")"
trap 'docker exec "$CONTAINER" rm -f "$TEMP_CONTAINER" >/dev/null 2>&1 || true' EXIT

echo "Restaurando..."
docker cp "$ARQUIVO" "$CONTAINER:$TEMP_CONTAINER" >/dev/null

# --single-transaction + ON_ERROR_STOP: se qualquer comando falhar, nada é gravado
# e o banco continua como estava antes.
# A saída normal (SET, CREATE TABLE...) é descartada; os erros continuam aparecendo.
docker exec "$CONTAINER" psql -U "$USUARIO" -d "$BANCO" -q \
  -v ON_ERROR_STOP=1 --single-transaction -f "$TEMP_CONTAINER" >/dev/null \
  || erro "a restauração falhou e foi desfeita; o banco continua como estava antes."

echo "Restauração concluída."
