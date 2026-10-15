#!/usr/bin/env bash
# Called by CI while notification-smoke-db is available. Test-only databases.
set -euo pipefail
docker exec notification-smoke-db createdb -U postgres migration_target
client_bin="$RUNNER_TEMP/notification-pg-client"
mkdir -p "$client_bin"
for tool in psql pg_dump pg_restore; do
  {
    printf '%s\n' '#!/usr/bin/env bash'
    printf '%s\n' 'exec docker run --rm --network host --user "$(id -u):$(id -g)" -e PGHOST -e PGPORT -e PGDATABASE -e PGUSER -e PGPASSWORD -e PGSSLMODE -e PGCONNECT_TIMEOUT -v "$PWD:/workspace" -w /workspace postgres:16-alpine '"$tool"' "$@"'
  } > "$client_bin/$tool"
  chmod +x "$client_bin/$tool"
done
export PATH="$client_bin:$PATH"
export SOURCE_DATABASE_URL='postgresql://postgres:smoke-only-password@127.0.0.1:15432/notification_smoke'
export TARGET_DATABASE_URL='postgresql://postgres:smoke-only-password@127.0.0.1:15432/migration_target'
export MIGRATION_WRITES_PAUSED=true
python3 scripts/migrate-postgres.py
if python3 scripts/migrate-postgres.py; then
  echo 'ERROR: migration should refuse a non-empty destination'
  exit 1
fi
echo 'OK: migration preserves data and refuses a non-empty destination.'
