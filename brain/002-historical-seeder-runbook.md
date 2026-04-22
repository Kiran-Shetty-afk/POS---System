# Historical Seeder Runbook

## Preconditions
- Backend database is reachable by the Spring Boot app.
- You are running from project root.
- Use manual trigger only; seeder is inactive unless `--seed.historical=true`.

## Commands
- Small preset (90 days):
  - `./mvnw spring-boot:run -Dspring-boot.run.arguments="--seed.historical=true --seed.volume=small --seed.days=90 --seed.reset=false --seed.random=42 --seed.shifts=true"`
- Medium preset (180 days):
  - `./mvnw spring-boot:run -Dspring-boot.run.arguments="--seed.historical=true --seed.volume=medium --seed.days=180 --seed.reset=false --seed.random=42 --seed.shifts=true"`
- Reset and reseed:
  - `./mvnw spring-boot:run -Dspring-boot.run.arguments="--seed.historical=true --seed.volume=medium --seed.days=180 --seed.reset=true --seed.random=42 --seed.shifts=true"`

## Expected Output Signals
- Scenario key logged (`historical:<volume>:<days>:<seed>`).
- Counts for stores, branches, products, customers, cashiers, orders, refunds.
- Branch revenue ranking printed.
- Refund ratio displayed.

## Notes
- Rerunning the same scenario key without reset is idempotent and skipped.
- Reset mode removes demo seed records (`Demo Retail*`, `@seed.local`) and clears `seed_runs`.
