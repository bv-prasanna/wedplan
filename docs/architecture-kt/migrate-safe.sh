#!/usr/bin/env bash
# WedFlow operator workflow: read-only plan by default, Flyway-only apply.
# Never executes a caller-supplied shell command, flyway clean, baseline or repair.
set -euo pipefail
umask 077

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SQL_DIR="$ROOT/backend/src/main/resources/db/migration"
MODE="${1:-plan}"

[[ -d "$SQL_DIR" ]] || { echo "No migration directory" >&2; exit 2; }
[[ "$MODE" =~ ^(plan|info|validate|migrate|export-schema)$ ]] || {
  echo "Usage: bash docs/architecture-kt/migrate-safe.sh plan|info|validate|migrate|export-schema" >&2
  exit 2
}

mapfile -t files < <(find "$SQL_DIR" -maxdepth 1 -type f -name 'V*__*.sql' -printf '%f\n' | sort -V)
declare -A seen=()
for file in "${files[@]}"; do
  version="${file%%__*}"
  [[ "$version" =~ ^V[0-9]+$ ]] || { echo "Invalid migration name" >&2; exit 3; }
  if [[ -n "${seen[$version]:-}" ]]; then
    echo "Duplicate Flyway version $version" >&2; exit 3
  fi
  seen[$version]="$file"
done

if [[ "$MODE" == "plan" ]]; then
  echo "WedFlow source-only migration inventory: ${#files[@]} versioned SQL files"
  printf '%s\n' "${files[@]}"
  echo "READ-ONLY. Compare against the target's flyway_schema_history; this is not a live DB diff."
  exit 0
fi

if [[ "$MODE" == "export-schema" ]]; then
  : "${PGHOST:?Set verified PostgreSQL host}"
  : "${PGDATABASE:?Set verified database name}"
  : "${PGUSER:?Set a read-only inspection user}"
  command -v pg_dump >/dev/null || { echo "Install PostgreSQL pg_dump" >&2; exit 2; }
  command -v psql >/dev/null || { echo "Install PostgreSQL psql" >&2; exit 2; }
  OUT_DIR="${SCHEMA_EXPORT_DIR:-$ROOT/schema-export}"
  mkdir -p "$OUT_DIR"
  stamp="$(date -u +%Y%m%dT%H%M%SZ)"
  pg_dump --schema-only --no-owner --no-privileges --file "$OUT_DIR/wedflow-$stamp-schema.sql"
  psql -X -v ON_ERROR_STOP=1 -q -c \
    "COPY (SELECT installed_rank,version,description,checksum,success,installed_on FROM flyway_schema_history ORDER BY installed_rank) TO STDOUT WITH CSV HEADER" \
    > "$OUT_DIR/wedflow-$stamp-flyway-history.csv"
  chmod 600 "$OUT_DIR/wedflow-$stamp-schema.sql" "$OUT_DIR/wedflow-$stamp-flyway-history.csv"
  echo "DDL-only schema and Flyway metadata saved in the operator-selected directory."
  echo "Review SQL defaults and metadata before sharing; never commit these exports."
  exit 0
fi

command -v flyway >/dev/null || { echo "Approved Flyway CLI is required" >&2; exit 2; }
: "${FLYWAY_URL:?Set approved PostgreSQL JDBC URL}"
: "${FLYWAY_USER:?Set approved migration user}"
: "${FLYWAY_PASSWORD:?Supply secret without logging}"
[[ "$FLYWAY_URL" == jdbc:postgresql://* ]] || { echo "Only PostgreSQL migration targets are supported" >&2; exit 2; }
opts=("-locations=filesystem:$SQL_DIR" "-cleanDisabled=true" "-baselineOnMigrate=false")

if [[ "$MODE" == "migrate" ]]; then
  [[ "${MIGRATION_APPROVAL:-}" == "APPROVED" ]] || { echo "Explicit migration approval required" >&2; exit 4; }
  [[ "${RESTORE_TESTED:-}" == "true" ]] || { echo "Verified backup restoration required" >&2; exit 4; }
  : "${BACKUP_REFERENCE:?Record an approved backup reference}"
  : "${RELEASE_SHA:?Record immutable release git SHA}"
  command -v git >/dev/null || { echo "Git is required for a release SHA guard" >&2; exit 2; }
  [[ "$(git -C "$ROOT" rev-parse HEAD)" == "$RELEASE_SHA" ]] || {
    echo "Release SHA mismatch; refused" >&2; exit 4;
  }
  [[ -z "$(git -C "$ROOT" status --porcelain -- backend/src/main/resources/db/migration)" ]] || {
    echo "Uncommitted migration changes; refused" >&2; exit 4;
  }
  if [[ "${APP_ENV:-}" == "production" ]] &&
       [[ "$FLYWAY_URL" == *localhost* || "$FLYWAY_URL" == *127.0.0.1* ]]; then
    echo "Production migration target must not be localhost" >&2; exit 4
  fi
  echo "Approved Flyway validation at release SHA $RELEASE_SHA; backup reference recorded."
  flyway "${opts[@]}" validate
  flyway "${opts[@]}" migrate
  echo "Flyway migrate returned successfully. Verify schema, health and financial workflows."
  exit 0
fi

flyway "${opts[@]}" "$MODE"
