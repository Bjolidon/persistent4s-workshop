# Step 4 — Wire the module

## Concept

`BankModule` assembles the full application using two persistent4s components:

**`PostgresModule`** — provides the event store and checkpoint store backed by PostgreSQL:
```scala
PostgresModule.make[IO, BankEvent](eventCodec, configPath)
```

**`DefaultProjector`** — reads new events from the event store and feeds them to projections:
```scala
val projector = DefaultProjector[IO, BankEvent](eventStore, checkpointStore)
projector.run(myProjection).compile.drain.background
```

Each `projector.run(projection)` returns a stream that runs continuously in the background, calling the projection's methods for every new event. The checkpoint store tracks progress so projections resume from where they left off after a restart.

## What to do

Open `src/main/scala/infrastructure/BankModule.scala` and fill in the three `???` blocks:

1. Call `PostgresModule.make` to get the event store components
2. Instantiate `DefaultProjector` using the event store and checkpoint store
3. Call `projector.run(...)` for each of the three projections

## Verify

Start Docker (`docker-compose -f src/main/resources/docker-compose.yml up -d`), then run:
```
sbt run
```
Open http://localhost:8080/docs and exercise the API:
- Register a member
- Create an account for that member
- Deposit into the account
- Check the account balance
- Transfer between two accounts
