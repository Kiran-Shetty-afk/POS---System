# Brain Changelog

## v2026.04.22.1 - Historical seeder scaffolding
- Added a manual historical seeding module under `com.zosh.seed` with configurable volume, date span, reset mode, and deterministic random seed.
- Introduced seed idempotency tracking with `SeedRun` (`seed_runs` table) and scenario keys.
- Added base/foundation generators for stores, branches, categories, products, cashiers, customers, and inventory.
- Added historical order generation with branch behavior profiles, payment mix, repeat customers, and optional shift reports.
- Added refund scenario generation with sparse refunds and a recent anomaly window.
- Updated `Order` entity pre-persist behavior to preserve explicitly provided `createdAt`.
- Added default seed configuration keys under `app.seed.historical` in `application.yml`.

## v2026.04.22.2 - Runbook and cleanup
- Added `brain/002-historical-seeder-runbook.md` with concrete manual run commands and expected output checks.
- Removed an unused import in `HistoricalSeedService` as part of seed module cleanup.

## v2026.04.22.3 - Reset safety fix
- Fixed reset sequencing in `HistoricalSeedService` so store admin references are cleared before deleting seeded users, preventing foreign key integrity issues during `--seed.reset=true`.
