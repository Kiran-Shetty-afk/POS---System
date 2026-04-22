# Build Fix: Lombok on JDK 24

## Problem
Historical seeding command failed before application startup because Maven compilation failed across DTO/entity mapper layers with missing methods like:
- `builder()`
- getters/setters (`getEmail`, `getProduct`, etc.)
- required constructors for `@RequiredArgsConstructor` classes

## Root Cause
Lombok was configured with an invalid Maven scope (`annotationProcessor`) and processor behavior was not explicitly wired for the compiler setup.

## Fix Applied
- Set Lombok dependency scope to `provided`.
- Set explicit Lombok version `1.18.42`.
- Added `maven-compiler-plugin` config with `annotationProcessorPaths` for Lombok.

## Verification
- `./mvnw clean compile` returns `BUILD SUCCESS`.
- Historical seeding startup command runs and logs:
  - `Historical seeding requested. volume=SMALL, days=90, reset=false, randomSeed=42`
  - application starts successfully on port 5000.
