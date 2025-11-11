#!/usr/bin/env bash
set -euo pipefail

CONTAINER=pg-local
DB_USER=app
DB_PASSWORD=app
DB_NAME=app
MAX_RETRIES=60
SLEEP=1

# fail fast if container doesn't exist
if ! docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER}$"; then
  echo "Container ${CONTAINER} not found. Start it with: docker compose -f src/main/resources/docker-compose.yml up -d"
  exit 1
fi

# wait for postgres to be ready
echo "Waiting for ${CONTAINER} to be ready..."
retries=0
until docker exec "${CONTAINER}" pg_isready -U "${DB_USER}" -d "${DB_NAME}" >/dev/null 2>&1; do
  ((retries++))
  if [ "${retries}" -ge "${MAX_RETRIES}" ]; then
    echo "Timed out waiting for Postgres after ${MAX_RETRIES} seconds"
    docker logs "${CONTAINER}" --tail 100 || true
    exit 2
  fi
  sleep "${SLEEP}"
done
echo "Postgres is ready."

# run SQL to create tables and seed data (idempotent)
# pass PGPASSWORD so psql doesn't prompt
docker exec -i -e PGPASSWORD="${DB_PASSWORD}" "${CONTAINER}" psql -U "${DB_USER}" -d "${DB_NAME}" <<'SQL'
BEGIN;

-- === Schema that matches the Java repo/tools ===

CREATE TABLE IF NOT EXISTS customers (
  id           SERIAL PRIMARY KEY,
  email        TEXT NOT NULL UNIQUE,
  name         TEXT NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS orders (
  id           SERIAL PRIMARY KEY,
  customer_id  INT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  total_cents  BIGINT NOT NULL CHECK (total_cents >= 0)
);

-- Helpful indexes for your queries
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders (created_at);
CREATE INDEX IF NOT EXISTS idx_orders_customer_id_created_at ON orders (customer_id, created_at);

-- === Seed customers (idempotent) ===
INSERT INTO customers (email, name)
VALUES
  ('alice@example.com', 'Alice Example'),
  ('bob@example.com',   'Bob Example')
ON CONFLICT (email) DO NOTHING;

-- === Seed a few orders with dates in January 2025 (idempotent) ===
-- Alice
INSERT INTO orders (customer_id, created_at, total_cents)
SELECT c.id, ts, amt
FROM customers c
JOIN (
  VALUES
    (TIMESTAMPTZ '2025-01-02 10:00:00+00',  8799),
    (TIMESTAMPTZ '2025-01-05 15:30:00+00', 12999),
    (TIMESTAMPTZ '2025-01-12 09:15:00+00',  2599)
) v(ts, amt) ON TRUE
WHERE c.email = 'alice@example.com'
  AND NOT EXISTS (
    SELECT 1 FROM orders o WHERE o.customer_id = c.id AND o.created_at = v.ts AND o.total_cents = v.amt
  );

-- Bob
INSERT INTO orders (customer_id, created_at, total_cents)
SELECT c.id, ts, amt
FROM customers c
JOIN (
  VALUES
    (TIMESTAMPTZ '2025-01-03 12:00:00+00',  4599),
    (TIMESTAMPTZ '2025-01-20 18:45:00+00', 18999)
) v(ts, amt) ON TRUE
WHERE c.email = 'bob@example.com'
  AND NOT EXISTS (
    SELECT 1 FROM orders o WHERE o.customer_id = c.id AND o.created_at = v.ts AND o.total_cents = v.amt
  );

COMMIT;
SQL

echo "Seeding complete."