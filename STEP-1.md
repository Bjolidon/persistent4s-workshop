# Step 1 — Define events

## Concept

In persistent4s, events are plain Scala case classes that extend the `Event` marker trait (from `persistent4s-core`). They are the facts that get stored in the event store.

For this exercice, we use circe for JSON serialization. Each event must derive `io.circe.Encoder` and `io.circe.Decoder` so the `persistent4s-circe` module can store and retrieve them.

## What to do

Open `src/main/scala/domain/BankEvent.scala`.

Define the following hierarchy:

```
BankEvent (sealed trait, extends Event)
├── MemberEvents (sealed trait)
│   └── MemberRegistered(memberId: UUID, name: String, birthDate: LocalDate)
├── AccountEvents (sealed trait)
│   ├── AccountCreated(accountId: UUID, memberId: UUID, name: String, createdAt: LocalDateTime)
│   └── AccountClosed(accountId: UUID, closedAt: LocalDateTime)
└── TransactionEvents (sealed trait)
    ├── Deposited(transactionId: UUID, accountId: UUID, amount: Int, timestamp: LocalDateTime)
    ├── Withdrawn(transactionId: UUID, accountId: UUID, amount: Int, timestamp: LocalDateTime)
    └── Transferred(transactionId: UUID, debitAccountId: UUID, creditAccountId: UUID, amount: Int, timestamp: LocalDateTime)
```

Each case class needs: `derives Encoder, Decoder`

## DCB note

The sub-hierarchies (`MemberEvents`, `AccountEvents`, `TransactionEvents`) are not required by the library — they are a domain modelling choice. What matters is that all events extend `BankEvent extends Event`.

## Verify

`sbt compile` should succeed.
