# Step 2 — Implement command handlers

## Concept

A `CommandHandler[C, S, E]` describes how to process a command. The library:
1. Reads events from the store that match the handler's declared `tags`
2. Folds them into a state using `evolve`
3. Validates the command against the state with `validate`
4. Appends the new events from `decide`

This is where **Dynamic Consistency Boundaries (DCB)** come in. The `tags` method declares exactly which event streams to read — there is no implicit aggregate boundary like in traditional event sourcing. The library reads all events across the declared tags and builds the state atomically.

Unlike aggregate-per-command patterns, a handler can read across any number of streams — you declare them explicitly. This makes cross-cutting consistency checks (e.g., validating that both a member and an account exist before processing a transaction) straightforward and transactional.

## Example — `RegisterMemberHandler`

`RegisterMember.scala` is already implemented as a reference. It is the simplest handler: no prior events are needed to register a member, so `tags` returns `Set.empty`, `initial` is `Unit`, and `evolve` is a no-op.

```scala
object RegisterMemberHandler extends CommandHandler[RegisterMember, Unit, BankEvent] {

  override def tags(command: RegisterMember): Set[Tag] = Set.empty

  override def initial: Unit = ()

  override def evolve(command: RegisterMember, state: Unit, event: BankEvent): Unit = ()

  override def validate(state: Unit, command: RegisterMember): Either[Throwable, Unit] =
    Right(())

  override def decide(state: Unit, command: RegisterMember): List[(Set[Tag], BankEvent)] =
    List(
      (Set(Tag("member", command.memberId.toString)),
       MemberRegistered(command.memberId, command.name, command.birthDate))
    )
}
```

## What to do

Open the remaining 5 handler files and implement the 5 methods in each:

| Method | Purpose |
|--------|---------|
| `tags(command)` | Which event streams to read — **the DCB declaration** |
| `initial` | Zero state before any events are replayed |
| `evolve(command, state, event)` | Fold one event into the state |
| `validate(state, command)` | Return `Left(error)` to reject, `Right(())` to accept |
| `decide(state, command)` | Return `List[(Set[Tag], Event)]` to append |

## DCB spotlights

- `CreateAccountHandler.tags` → reads the member's stream to verify the member exists before creating an account
- `TransferHandler.tags` → reads **both** account streams atomically to check balances and closed status

## Verify

Run `sbt test`. All command handler specs should pass when your implementation is correct.
