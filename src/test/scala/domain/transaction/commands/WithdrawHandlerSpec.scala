package domain.transaction.commands

import munit.FunSuite
import java.util.UUID
import java.time.LocalDateTime
import domain.*
import persistent4s.Tag

class WithdrawHandlerSpec extends FunSuite:

  val accountId     = UUID.fromString("00000000-0000-0000-0000-000000000001")
  val transactionId = UUID.fromString("00000000-0000-0000-0000-000000000002")
  val command       = Withdraw(transactionId, accountId, 50)
  val initial       = WithdrawHandler.initial

  test("initial state has accountExists = false, balance = 0, accountClosed = false"):
    assertEquals(initial, WithdrawState(accountExists = false, balance = 0, accountClosed = false))

  test("evolve sets accountExists on AccountCreated"):
    val event    = AccountCreated(accountId, UUID.randomUUID(), "main", LocalDateTime.now())
    val newState = WithdrawHandler.evolve(command, initial, event)
    assertEquals(newState.accountExists, true)

  test("evolve increases balance on Deposited"):
    val state    = WithdrawState(accountExists = true, balance = 0, accountClosed = false)
    val event    = Deposited(UUID.randomUUID(), accountId, 100, LocalDateTime.now())
    val newState = WithdrawHandler.evolve(command, state, event)
    assertEquals(newState.balance, 100)

  test("evolve decreases balance on Withdrawn"):
    val state    = WithdrawState(accountExists = true, balance = 100, accountClosed = false)
    val event    = Withdrawn(UUID.randomUUID(), accountId, 30, LocalDateTime.now())
    val newState = WithdrawHandler.evolve(command, state, event)
    assertEquals(newState.balance, 70)

  test("evolve decreases balance on Transferred when account is debit side"):
    val state    = WithdrawState(accountExists = true, balance = 200, accountClosed = false)
    val event    = Transferred(UUID.randomUUID(), accountId, UUID.randomUUID(), 50, LocalDateTime.now())
    val newState = WithdrawHandler.evolve(command, state, event)
    assertEquals(newState.balance, 150)

  test("evolve increases balance on Transferred when account is credit side"):
    val state    = WithdrawState(accountExists = true, balance = 100, accountClosed = false)
    val event    = Transferred(UUID.randomUUID(), UUID.randomUUID(), accountId, 50, LocalDateTime.now())
    val newState = WithdrawHandler.evolve(command, state, event)
    assertEquals(newState.balance, 150)

  test("evolve sets accountClosed on AccountClosed"):
    val state    = WithdrawState(accountExists = true, balance = 100, accountClosed = false)
    val event    = AccountClosed(accountId, LocalDateTime.now())
    val newState = WithdrawHandler.evolve(command, state, event)
    assertEquals(newState.accountClosed, true)

  test("validate fails when account does not exist"):
    assert(WithdrawHandler.validate(initial, command).isLeft)

  test("validate fails when account is closed"):
    val state = WithdrawState(accountExists = true, balance = 200, accountClosed = true)
    assert(WithdrawHandler.validate(state, command).isLeft)

  test("validate fails when amount is zero"):
    val state = WithdrawState(accountExists = true, balance = 200, accountClosed = false)
    assert(WithdrawHandler.validate(state, Withdraw(transactionId, accountId, 0)).isLeft)

  test("validate fails when balance is insufficient"):
    val state = WithdrawState(accountExists = true, balance = 30, accountClosed = false)
    assert(WithdrawHandler.validate(state, command).isLeft)

  test("validate succeeds when balance is sufficient"):
    val state = WithdrawState(accountExists = true, balance = 200, accountClosed = false)
    assertEquals(WithdrawHandler.validate(state, command), Right(()))

  test("decide emits Withdrawn with correct data and account tag"):
    val state            = WithdrawState(accountExists = true, balance = 200, accountClosed = false)
    val results          = WithdrawHandler.decide(state, command)
    assertEquals(results.size, 1)
    val (tags, event)    = results.head
    assert(tags.contains(Tag("account", accountId.toString)))
    event match
      case Withdrawn(id, aId, amount, _) =>
        assertEquals(id, transactionId)
        assertEquals(aId, accountId)
        assertEquals(amount, 50)
      case _ => fail(s"Expected Withdrawn, got $event")
