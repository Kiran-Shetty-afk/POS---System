# Historical Sales Seeder Notes

## Purpose
Create realistic demo analytics data spanning past weeks/months with relational consistency:
- branch-aware orders
- cashier-to-branch consistency
- store/category/product alignment
- repeat customers
- sparse but demonstrable refunds

## Trigger
Run manually with CLI arguments:
- `--seed.historical=true`
- `--seed.volume=small|medium|large`
- `--seed.days=90|180`
- `--seed.reset=true|false`
- `--seed.random=<number>`
- `--seed.shifts=true|false`

## Current Behavior
- `HistoricalSeedCommand` only executes when `--seed.historical=true`.
- `HistoricalSeedService` supports rerun safety through `seed_runs` scenario keys.
- `--seed.reset=true` clears previously seeded `Demo Retail` data and `@seed.local` users/customers.
- Seeder emits a post-run summary with branch revenue and refund ratio.
- Seeded products include deterministic stock image URLs via Picsum (`picsum.photos/seed/...`) for better UI realism.
- Seeded customers use generic human names (e.g., John, Joseph, Mary combinations) instead of `Customer <n>` labels.

## Important Implementation Detail
`Order.onCreate()` now preserves explicit `createdAt`; it only auto-fills when null. This enables backdated historical orders for trend charts.
