# persistent4s Workshop Design

Date: 2026-04-29

## Context

A half-day workshop for colleagues who are familiar with event sourcing and the Axon framework. The goal is to discover the persistent4s library API and its Dynamic Consistency Boundary (DCB) model, where developers explicitly declare which event streams a command or projection cares about — rather than relying on implicit framework wiring as in Axon.

The vehicle is a banking example (Members, Accounts, Transactions) that is already fully implemented. Participants rebuild the persistent4s-specific parts from scratch.

---

## Branch & Tag Strategy

- `master` — complete working solution, serves as reference
- `workshop` — starting point that participants clone; persistent4s-specific code replaced with `???`
- `step-N-solution` — tagged commit after each step, participants can check out if stuck

---

## Steps

### Step 0 — Add persistent4s dependencies (`build.sbt`)

**What's blanked:** the 5 persistent4s `libraryDependencies` lines are removed.

**Task:** add the correct coordinates for:
- `persistent4s-core`
- `persistent4s-postgres`
- `persistent4s-circe`
- `persistent4s-kafka`
- `persistent4s-testkit` (% Test)

**Goal:** participants see the modular structure of the library before writing any code.

**Tests:** none — compiler confirms correctness at step 1.

---

### Step 1 — Define events (`BankEvent.scala`)

**What's blanked:** entire file body (only `import persistent4s.Event` remains as a hint).

**Task:**
- Create a `sealed trait BankEvent extends Event`
- Define sub-hierarchies: `MemberEvents`, `AccountEvents`, `TransactionEvents`
- Define all 6 case classes: `MemberRegistered`, `AccountCreated`, `AccountClosed`, `Deposited`, `Withdrawn`, `Transferred`
- Derive `Encoder` and `Decoder` for each (needed by `persistent4s-circe`)

**DCB relevance:** events are the atoms of the event stream. Their type names are later used in `EventFilter` to declare what a projection listens to.

**Tests:** none — compiler confirms correctness at step 2.

---

### Step 2 — Implement command handlers (6 files)

Files: `RegisterMemberHandler`, `CreateAccountHandler`, `CloseAccountHandler`, `DepositHandler`, `WithdrawHandler`, `TransferHandler`

**What's blanked per handler:** `tags`, `initial`, `evolve`, `validate`, `decide`. The command and state case class definitions stay.

**Task:** implement the 5 methods of `CommandHandler[Cmd, State, E]`:
- `tags` — declare which event streams to read (DCB: this is the key method)
- `initial` — the zero state before any events are replayed
- `evolve` — fold one event into the current state
- `validate` — reject the command if the state says it's invalid
- `decide` — produce the list of `(tags, event)` pairs to append

**DCB spotlight:**
- `RegisterMemberHandler.tags` returns `Set.empty` — no prior events needed
- `CreateAccountHandler.tags` uses `Tag("member", memberId)` — reads member stream to verify existence
- `TransferHandler.tags` declares both account streams — reads two independent streams atomically

**Tests:** `RegisterMemberHandlerSpec`, `CreateAccountHandlerSpec`, `CloseAccountHandlerSpec`, `DepositHandlerSpec`, `WithdrawHandlerSpec`, `TransferHandlerSpec`

Each spec tests:
- `evolve`: feed a sequence of events, assert resulting state
- `validate`: assert `Right(())` for valid state and `Left(error)` for each invalid case
- `decide`: assert the correct event type and tags are produced

---

### Step 3 — Implement projections (3 files)

Files: `MemberProjection`, `AccountProjection`, `TransactionProjection`

**What's blanked per projection:** `filter`, `resolveKeys`, `fetchStates`, `persistStates`, `handle`.

**Task:** implement the methods of `Projection[F, E, K, V]`:
- `filter` — declare which event types this projection subscribes to (DCB read side)
- `resolveKeys` — extract the state key(s) from an event envelope
- `fetchStates` — load current states from the repository (delegates to `repository.findMany`)
- `handle` — apply one event to one state, return the new state
- `persistStates` — save updated states (delegates to `repository.persistMany`)

**DCB spotlight:**
- `AccountProjection.filter` includes transaction event types (`Deposited`, `Withdrawn`, `Transferred`) even though it's an account projection — cross-aggregate event consumption is explicit and visible
- `AccountProjection.resolveKeys` returns two UUIDs for `Transferred` (both accounts are affected)

**Tests:** `MemberProjectionSpec`, `AccountProjectionSpec`, `TransactionProjectionSpec`

Each spec tests:
- `filter`: assert the correct `EventTypeName`s are present
- `resolveKeys`: assert correct UUID(s) extracted per event type
- `handle`: assert state transitions (e.g. `None` + `AccountCreated` → initial `Account`, `Some(account)` + `Deposited` → balance updated)

---

### Step 4 — Wire the module (`BankModule.scala`)

**What's blanked:**
- `PostgresModule.make[IO, BankEvent](eventCodec, configPath)` call
- `DefaultProjector[IO, BankEvent](eventStore, checkpointStore)` instantiation
- The three `projector.run(projection).compile.drain.background` calls

**Task:** wire the event store and projector using the persistent4s postgres module, then register all three projections to run in the background.

**Goal:** understand how the library ties together — event store, checkpoint store, and continuous projection runners.

**Tests:** none — validated by running the app end-to-end.

---

## README / Guidance Files

- **`README.md`** (root): domain description (banking app), how to start Docker (`docker-compose up`), step structure overview, link to Swagger UI once running
- **`STEP-0.md` through `STEP-4.md`**: one file per step, committed at the corresponding tag
  - One paragraph explaining the concept relative to Axon equivalents
  - Exact task description
  - A DCB-specific hint pointing to the interesting cross-aggregate case

---

## What Is Pre-Filled (Not Workshop Material)

- Skunk repositories (`MemberRepository`, `AccountRepository`, `TransactionRepository`)
- Smithy API definitions and generated code
- Application service implementations (`AccountServiceImpl`, etc.)
- Infrastructure wiring for HTTP (`BankRoutes`, `BankServer`)
- `docker-compose.yml` and `application.conf`
- `infrastructure.Repository` trait

---

## File Layout After Implementation

```
src/
  main/scala/
    domain/
      BankEvent.scala                        ← Step 1
      member/commands/RegisterMember.scala   ← Step 2
      account/commands/CreateAccount.scala   ← Step 2
      account/commands/CloseAccount.scala    ← Step 2
      transaction/commands/Deposit.scala     ← Step 2
      transaction/commands/Withdraw.scala    ← Step 2
      transaction/commands/Transfer.scala    ← Step 2
      member/MemberProjection.scala          ← Step 3
      account/AccountProjection.scala        ← Step 3
      transaction/TransactionProjection.scala← Step 3
  infrastructure/
      BankModule.scala                       ← Step 4
  test/scala/
    RegisterMemberHandlerSpec.scala          ← Step 2 tests
    CreateAccountHandlerSpec.scala           ← Step 2 tests
    CloseAccountHandlerSpec.scala            ← Step 2 tests
    DepositHandlerSpec.scala                 ← Step 2 tests
    WithdrawHandlerSpec.scala                ← Step 2 tests
    TransferHandlerSpec.scala                ← Step 2 tests
    MemberProjectionSpec.scala               ← Step 3 tests
    AccountProjectionSpec.scala              ← Step 3 tests
    TransactionProjectionSpec.scala          ← Step 3 tests
```
