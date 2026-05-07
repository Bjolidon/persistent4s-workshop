# Step 2 — Implement command handlers

## Concept

A `CommandHandler[C, S, E]` describes how to process a command. The library:
1. Reads events from the store that match the handler's declared `tags`
2. Folds them into a state using `evolve`
3. Validates the command against the state with `validate`
4. Appends the new events from `decide`

This is where **Dynamic Consistency Boundaries (DCB)** come in. The `tags` method declares exactly which event streams to read — there is no implicit aggregate boundary like in traditional event sourcing. The library reads all events across the declared tags and builds the state atomically.

Unlike aggregate-per-command patterns, a handler can read across any number of streams — you declare them explicitly. This makes cross-cutting consistency checks (e.g., validating that both a member and an account exist before processing a transaction) straightforward and transactional.

## Example — `CloseAccountHandler`

`CloseAccount.scala` is already implemented as a reference. It reads a single account stream to check whether the account exists and is not already closed before closing it.

```scala
object CloseAccountHandler extends CommandHandler[CloseAccount, CloseAccountState, BankEvent] {

  override def tags(command: CloseAccount): Set[Tag] =
    Set(Tag("account", command.accountId.toString))

  override def initial: CloseAccountState =
    CloseAccountState(accountExists = false, accountClosed = false)

  override def evolve(command: CloseAccount, state: CloseAccountState, event: BankEvent): CloseAccountState =
    event match {
      case AccountCreated(accountId, _, _, _) if accountId == command.accountId =>
        state.copy(accountExists = true)
      case AccountClosed(accountId, _) if accountId == command.accountId =>
        state.copy(accountClosed = true)
      case _ => state
    }

  override def validate(state: CloseAccountState, command: CloseAccount): Either[Throwable, Unit] =
    if (!state.accountExists) Left(new Exception("Account does not exist"))
    else if (state.accountClosed) Left(new Exception("Account is already closed"))
    else Right(())

  override def decide(state: CloseAccountState, command: CloseAccount): List[(Set[Tag], BankEvent)] =
    List(
      (Set(Tag("account", command.accountId.toString)),
       AccountClosed(command.accountId, LocalDateTime.now()))
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

```
sbt "testOnly *HandlerSpec"
```
All command handler specs should pass when your implementation is correct.
