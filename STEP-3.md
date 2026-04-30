# Step 3 — Implement projections

## Concept

A `Projection[F, E, K, V]` builds and maintains a read model by consuming events from the store.

The library calls the projection methods in this sequence for each event:
1. `filter` — checked first; the event is skipped if it does not match
2. `resolveKeys` — extract the state keys affected by this event
3. `fetchStates` — load current states for those keys from the repository
4. `handle` — compute the new state for each key
5. `persistStates` — save updated states back to the repository

DCB on the read side means `filter` is explicit: you declare exactly which event types the projection subscribes to using `EventTypeName.of[T]`. A projection can subscribe to events from multiple logical aggregates — this is what makes read models flexible in a DCB system.

## Example — `MemberProjection`

`MemberProjection.scala` is already implemented as a reference. It subscribes to a single event type and builds the `Member` read model.

```scala
override def filter: EventFilter =
  EventFilter(Set(EventTypeName.of[MemberRegistered]))

override def resolveKeys(event: EventEnvelope[BankEvent]): List[UUID] =
  event.payload match
    case MemberRegistered(memberId, _, _) => List(memberId)
    case _                                => Nil

override def fetchStates(keys: List[UUID]): IO[Map[UUID, Option[Member]]] =
  repository.findMany(keys)

override def handle(state: Option[Member], event: EventEnvelope[BankEvent]): IO[Option[Member]] =
  (state, event.payload) match
    case (None, MemberRegistered(memberId, name, birthDate)) =>
      IO.pure(Some(Member(memberId, name, birthDate)))
    case _ =>
      IO.raiseError(new RuntimeException(s"Unexpected: $state / ${event.payload}"))

override def persistStates(states: Map[UUID, Option[Member]]): IO[Unit] =
  repository.persistMany(states)
```

## What to do

Open the remaining 2 projection files and implement all 5 methods in each.

For `fetchStates` and `persistStates`, delegate directly to the repository:
```scala
override def fetchStates(keys: List[UUID]): IO[Map[UUID, Option[Account]]] =
  repository.findMany(keys)

override def persistStates(states: Map[UUID, Option[Account]]): IO[Unit] =
  repository.persistMany(states)
```

## DCB spotlights

- `AccountProjection.filter` includes `Deposited`, `Withdrawn`, `Transferred` — transaction events affect account balances, so the account projection must subscribe to them even though they "belong" to a different domain concept
- `AccountProjection.resolveKeys` for `Transferred` returns **two** UUIDs — both the debit and credit accounts need their state updated from a single event

## Verify

```
sbt "testOnly *ProjectionSpec"
```
All projection specs should pass when your implementation is correct.
