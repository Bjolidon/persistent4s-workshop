package domain.account.commands

import munit.FunSuite
import java.util.UUID
import java.time.LocalDateTime
import domain.*
import persistent4s.Tag

class CloseAccountHandlerSpec extends FunSuite:

  val accountId = UUID.fromString("00000000-0000-0000-0000-000000000001")
  val command   = CloseAccount(accountId)
  val initial   = CloseAccountHandler.initial

  test("initial state has accountExists and accountClosed = false"):
    assertEquals(initial, CloseAccountState(accountExists = false, accountClosed = false))

  test("evolve sets accountExists on AccountCreated for the right account"):
    val event    = AccountCreated(accountId, UUID.randomUUID(), "main", LocalDateTime.now())
    val newState = CloseAccountHandler.evolve(command, initial, event)
    assertEquals(newState.accountExists, true)

  test("evolve ignores AccountCreated for a different account"):
    val event    = AccountCreated(UUID.randomUUID(), UUID.randomUUID(), "other", LocalDateTime.now())
    val newState = CloseAccountHandler.evolve(command, initial, event)
    assertEquals(newState.accountExists, false)

  test("evolve sets accountClosed on AccountClosed for the right account"):
    val state    = CloseAccountState(accountExists = true, accountClosed = false)
    val event    = AccountClosed(accountId, LocalDateTime.now())
    val newState = CloseAccountHandler.evolve(command, state, event)
    assertEquals(newState.accountClosed, true)

  test("validate fails when account does not exist"):
    assert(CloseAccountHandler.validate(initial, command).isLeft)

  test("validate fails when account is already closed"):
    val state = CloseAccountState(accountExists = true, accountClosed = true)
    assert(CloseAccountHandler.validate(state, command).isLeft)

  test("validate succeeds when account exists and is open"):
    val state = CloseAccountState(accountExists = true, accountClosed = false)
    assertEquals(CloseAccountHandler.validate(state, command), Right(()))

  test("decide emits AccountClosed with account tag"):
    val state            = CloseAccountState(accountExists = true, accountClosed = false)
    val results          = CloseAccountHandler.decide(state, command)
    assertEquals(results.size, 1)
    val (tags, event)    = results.head
    assert(tags.contains(Tag("account", accountId.toString)))
    event match
      case AccountClosed(id, _) => assertEquals(id, accountId)
      case _                    => fail(s"Expected AccountClosed, got $event")
