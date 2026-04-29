# Step 0 — Add persistent4s dependencies

## What to do

Open `build.sbt` and add the persistent4s library dependencies.

The library is split into focused modules:

| Module | Purpose |
|--------|---------|
| `persistent4s-core` | Core traits: `Event`, `CommandHandler`, `Projection`, `EventStore` |
| `persistent4s-postgres` | PostgreSQL-backed event store via `PostgresModule` |
| `persistent4s-circe` | JSON event serialization via `CirceEventCodec` |
| `persistent4s-kafka` | Kafka event notification (used to trigger projections) |
| `persistent4s-testkit` | Test utilities (% Test scope) |

All modules are published under `io.github.antoniojimeneznieto` at version `0.2.0`.

## Hint

In sbt, a dependency looks like:
```scala
"groupId" %% "artifactId" % "version"
```
The `%%` means sbt appends the Scala version automatically (e.g. `persistent4s-core_3`).

Once added, run `sbt compile` to verify.
