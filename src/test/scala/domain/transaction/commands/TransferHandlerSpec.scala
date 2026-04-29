package domain.transaction.commands

import munit.FunSuite
import java.util.UUID
import java.time.LocalDateTime
import domain.*
import persistent4s.Tag

class TransferHandlerSpec extends FunSuite:

  val debitId       = UUID.fromString("00000000-0000-0000-0000-000000000001")
  val creditId      = UUID.fromString("00000000-0000-0000-0000-000000000002")
  val transactionId = UUID.fromString("00000000-0000-0000-0000-000000000003")
  val command       = Transfer(transactionId, debitId, creditId, 100)
  val initial       = TransferHandler.initial

  test("initial state is fully empty"):
    assertEquals(initial, TransferState(false, false, 0, false, false))

  test("tags declares both account streams (DCB)"):
    val tags = TransferHandler.tags(command)
    assert(tags.contains(Tag("account", debitId.toString)))
    assert(tags.contains(Tag("account", creditId.toString)))

  test("evolve sets debitAccountExists on AccountCreated for debit account"):
    val event    = AccountCreated(debitId, UUID.randomUUID(), "debit", LocalDateTime.now())
    val newState = TransferHandler.evolve(command, initial, event)
    assertEquals(newState.debitAccountExists, true)
    assertEquals(newState.creditAccountExists, false)

  test("evolve sets creditAccountExists on AccountCreated for credit account"):
    val event    = AccountCreated(creditId, UUID.randomUUID(), "credit", LocalDateTime.now())
    val newState = TransferHandler.evolve(command, initial, event)
    assertEquals(newState.creditAccountExists, true)
    assertEquals(newState.debitAccountExists, false)

  test("evolve increases debitAccountBalance on Deposited to debit account"):
    val state    = TransferState(true, true, 0, false, false)
    val event    = Deposited(UUID.randomUUID(), debitId, 200, LocalDateTime.now())
    val newState = TransferHandler.evolve(command, state, event)
    assertEquals(newState.debitAccountBalance, 200)

  test("evolve decreases debitAccountBalance on Withdrawn from debit account"):
    val state    = TransferState(true, true, 200, false, false)
    val event    = Withdrawn(UUID.randomUUID(), debitId, 50, LocalDateTime.now())
    val newState = TransferHandler.evolve(command, state, event)
    assertEquals(newState.debitAccountBalance, 150)

  test("evolve sets debitAccountClosed on AccountClosed for debit account"):
    val state    = TransferState(true, true, 200, false, false)
    val event    = AccountClosed(debitId, LocalDateTime.now())
    val newState = TransferHandler.evolve(command, state, event)
    assertEquals(newState.debitAccountClosed, true)

  test("evolve sets creditAccountClosed on AccountClosed for credit account"):
    val state    = TransferState(true, true, 200, false, false)
    val event    = AccountClosed(creditId, LocalDateTime.now())
    val newState = TransferHandler.evolve(command, state, event)
    assertEquals(newState.creditAccountClosed, true)

  test("validate fails when debit and credit accounts are the same"):
    val sameId  = UUID.randomUUID()
    val cmd     = Transfer(transactionId, sameId, sameId, 100)
    val state   = TransferState(true, true, 200, false, false)
    assert(TransferHandler.validate(state, cmd).isLeft)

  test("validate fails when debit account does not exist"):
    val state = TransferState(false, true, 200, false, false)
    assert(TransferHandler.validate(state, command).isLeft)

  test("validate fails when credit account does not exist"):
    val state = TransferState(true, false, 200, false, false)
    assert(TransferHandler.validate(state, command).isLeft)

  test("validate fails when debit account is closed"):
    val state = TransferState(true, true, 200, true, false)
    assert(TransferHandler.validate(state, command).isLeft)

  test("validate fails when credit account is closed"):
    val state = TransferState(true, true, 200, false, true)
    assert(TransferHandler.validate(state, command).isLeft)

  test("validate fails when amount is zero"):
    val state = TransferState(true, true, 200, false, false)
    assert(TransferHandler.validate(state, Transfer(transactionId, debitId, creditId, 0)).isLeft)

  test("validate fails when debit balance is insufficient"):
    val state = TransferState(true, true, 50, false, false)
    assert(TransferHandler.validate(state, command).isLeft)

  test("validate succeeds when all conditions are met"):
    val state = TransferState(true, true, 200, false, false)
    assertEquals(TransferHandler.validate(state, command), Right(()))

  test("decide emits Transferred with both account tags"):
    val state            = TransferState(true, true, 200, false, false)
    val results          = TransferHandler.decide(state, command)
    assertEquals(results.size, 1)
    val (tags, event)    = results.head
    assert(tags.contains(Tag("account", debitId.toString)))
    assert(tags.contains(Tag("account", creditId.toString)))
    event match
      case Transferred(id, dId, cId, amount, _) =>
        assertEquals(id, transactionId)
        assertEquals(dId, debitId)
        assertEquals(cId, creditId)
        assertEquals(amount, 100)
      case _ => fail(s"Expected Transferred, got $event")
