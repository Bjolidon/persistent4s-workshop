# Step 0 — Add persistent4s dependencies

## What to do

Open `build.sbt` and add the persistent4s library dependencies.

The library is split into focused modules:

| Module | Purpose |
|--------|---------|
| `persistent4s-core` | Core traits: `Event`, `CommandHandler`, `Projection`, `EventStore` |
| `persistent4s-postgres` | PostgreSQL-backed event store via `PostgresModule` |
| `persistent4s-circe` | JSON event serialization via `CirceEventCodec` |
| `persistent4s-monitoring` | Monitoring interface available on port 9090 |

All modules are published under groupId `io.github.antoniojimeneznieto` at version `0.2.1`.

## Hint

In sbt, a dependency looks like:
```scala
"groupId" %% "module" % "version"
```
The `%%` means sbt appends the Scala version automatically (e.g. `persistent4s-core_3`).

Once added, run `sbt compile` to check the dependencies resolve. You will see compilation errors about undefined types (`MemberRegistered`, `AccountCreated`, etc.) — these are expected and will be resolved in step 1.
