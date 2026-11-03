#!/usr/bin/env bash
# Backup do banco do Connect CRM (RNF-004) — Linux / WSL.
#
# Gera um arquivo .sql com todos os dados atuais em database/backups/,
# com data e hora no nome (ex.: connect_crm_2026-10-26_1530.sql).
#
# Uso (com o container connect-crm-db rodando):
#   bash database/scripts/backup.sh
set -euo pipefail

# shellcheck source=database/scripts/comum.sh
source "$(dirname "${BASH_SOURCE[0]}")/comum.sh"

BANCO="$(ler_env POSTGRES_DB)"
USUARIO="$(ler_env POSTGRES_USER)"
verificar_banco "$USUARIO" "$BANCO"

NOME="${BANCO}_$(date +%Y-%m-%d_%H%M).sql"
DESTINO="$DIR_BACKUPS/$NOME"
TEMP_CONTAINER="/tmp/$NOME"

mkdir -p "$DIR_BACKUPS"
[ ! -e "$DESTINO" ] || erro "já existe um backup com o nome $NOME. Aguarde um minuto e rode de novo."

# Apaga o arquivo temporário de dentro do container, mesmo se algo der errado no meio.
trap 'docker exec "$CONTAINER" rm -f "$TEMP_CONTAINER" >/dev/null 2>&1 || true' EXIT

echo "Gerando backup do banco '$BANCO' (container $CONTAINER)..."

# O pg_dump roda DENTRO do container e conecta pelo socket local, que não pede senha:
# por isso a senha do .env não é usada nem aparece aqui.
#   --clean --if-exists         o arquivo apaga cada objeto antes de recriá-lo, então a
#                               restauração substitui o que estiver no banco.
#   --no-owner --no-privileges  o backup não fica preso ao usuário que o gerou.
# O arquivo é gerado dentro do container e copiado com "docker cp" para não passar
# pelo terminal (evita problema de codificação de caracteres).
docker exec "$CONTAINER" pg_dump -U "$USUARIO" -d "$BANCO" \
  --clean --if-exists --no-owner --no-privileges --encoding=UTF8 -f "$TEMP_CONTAINER"
docker cp "$CONTAINER:$TEMP_CONTAINER" "$DESTINO" >/dev/null

[ -s "$DESTINO" ] || erro "o arquivo de backup ficou vazio: $DESTINO"

echo "Backup concluído: database/backups/$NOME ($(du -h "$DESTINO" | cut -f1))"
