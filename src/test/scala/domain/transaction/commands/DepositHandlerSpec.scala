package domain.transaction.commands

import munit.FunSuite
import java.util.UUID
import java.time.LocalDateTime
import domain.*
import persistent4s.Tag

class DepositHandlerSpec extends FunSuite:

  val accountId = UUID.fromString("00000000-0000-0000-0000-000000000001")
  val transactionId = UUID.fromString("00000000-0000-0000-0000-000000000002")
  val command = Deposit(transactionId, accountId, 100)
  val initial = DepositHandler.initial

  test("initial state has accountExists and accountClosed = false"):
    assertEquals(
      initial,
      DepositState(accountExists = false, accountClosed = false)
    )

  test("evolve sets accountExists on AccountCreated for the right account"):
    val event =
      AccountCreated(accountId, UUID.randomUUID(), "main", LocalDateTime.now())
    val newState = DepositHandler.evolve(command, initial, event)
    assertEquals(newState.accountExists, true)

  test("evolve sets accountClosed on AccountClosed for the right account"):
    val state = DepositState(accountExists = true, accountClosed = false)
    val event = AccountClosed(accountId, LocalDateTime.now())
    val newState = DepositHandler.evolve(command, state, event)
    assertEquals(newState.accountClosed, true)

  test("validate fails when account does not exist"):
    assert(DepositHandler.validate(initial, command).isLeft)

  test("validate fails when account is closed"):
    val state = DepositState(accountExists = true, accountClosed = true)
    assert(DepositHandler.validate(state, command).isLeft)

  test("validate fails when amount is zero"):
    val state = DepositState(accountExists = true, accountClosed = false)
    assert(
      DepositHandler
        .validate(state, Deposit(transactionId, accountId, 0))
        .isLeft
    )

  test("validate fails when amount is negative"):
    val state = DepositState(accountExists = true, accountClosed = false)
    assert(
      DepositHandler
        .validate(state, Deposit(transactionId, accountId, -50))
        .isLeft
    )

  test("validate succeeds for valid state and positive amount"):
    val state = DepositState(accountExists = true, accountClosed = false)
    assertEquals(DepositHandler.validate(state, command), Right(()))

  test("decide emits Deposited with correct data and account tag"):
    val state = DepositState(accountExists = true, accountClosed = false)
    val results = DepositHandler.decide(state, command)
    assertEquals(results.size, 1)
    val (tags, event) = results.head
    assert(tags.contains(Tag("account", accountId.toString)))
    event match
      case Deposited(id, aId, amount, _) =>
        assertEquals(id, transactionId)
        assertEquals(aId, accountId)
        assertEquals(amount, 100)
      case _ => fail(s"Expected Deposited, got $event")
