# Step 2 — Implement command handlers

## Concept

A `CommandHandler[C, S, E]` describes how to process a command. The library:
1. Reads events from the store that match the handler's declared `tags`
2. Folds them into a state using `evolve`
3. Validates the command against the state with `validate`
4. Appends the new events from `decide`

This is where **Dynamic Consistency Boundaries (DCB)** come in. The `tags` method declares exactly which event streams to read — there is no implicit aggregate boundary. The library reads all events across the declared tags and builds the state atomically.

In Axon, a command handler is bound to one aggregate instance. In persistent4s, a command can read across any number of streams — you declare them explicitly.

## What to do

Open each of the 6 handler files and implement the 5 methods:

| Method | Purpose |
|--------|---------|
| `tags(command)` | Which event streams to read — **the DCB declaration** |
| `initial` | Zero state before any events are replayed |
| `evolve(command, state, event)` | Fold one event into the state |
| `validate(state, command)` | Return `Left(error)` to reject, `Right(())` to accept |
| `decide(state, command)` | Return `List[(Set[Tag], Event)]` to append |

## DCB spotlights

- `RegisterMemberHandler.tags` → `Set.empty` — no prior events needed to register a member
- `CreateAccountHandler.tags` → reads the member's stream to verify the member exists before creating an account
- `TransferHandler.tags` → reads **both** account streams atomically to check balances and closed status

## Verify

Run `sbt test`. All command handler specs should pass when your implementation is correct.
