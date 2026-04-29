package domain.account.commands

import munit.FunSuite
import java.util.UUID
import java.time.{LocalDate, LocalDateTime}
import domain.*
import persistent4s.Tag

class CreateAccountHandlerSpec extends FunSuite:

  val memberId   = UUID.fromString("00000000-0000-0000-0000-000000000001")
  val accountId  = UUID.fromString("00000000-0000-0000-0000-000000000002")
  val command    = CreateAccount(accountId, memberId, "Savings")
  val initial    = CreateAccountHandler.initial

  test("initial state has memberExists = false"):
    assertEquals(initial, AccountCreatedState(memberExists = false))

  test("evolve sets memberExists when MemberRegistered matches memberId"):
    val event    = MemberRegistered(memberId, "Alice", LocalDate.of(1990, 1, 1))
    val newState = CreateAccountHandler.evolve(command, initial, event)
    assertEquals(newState.memberExists, true)

  test("evolve ignores MemberRegistered for a different memberId"):
    val event    = MemberRegistered(UUID.randomUUID(), "Bob", LocalDate.of(1990, 1, 1))
    val newState = CreateAccountHandler.evolve(command, initial, event)
    assertEquals(newState.memberExists, false)

  test("validate fails when member does not exist"):
    assert(CreateAccountHandler.validate(initial, command).isLeft)

  test("validate succeeds when member exists"):
    val state = AccountCreatedState(memberExists = true)
    assertEquals(CreateAccountHandler.validate(state, command), Right(()))

  test("decide emits AccountCreated with member and account tags"):
    val state            = AccountCreatedState(memberExists = true)
    val results          = CreateAccountHandler.decide(state, command)
    assertEquals(results.size, 1)
    val (tags, event)    = results.head
    assert(tags.exists(_.category == "member"))
    assert(tags.exists(_.category == "account"))
    event match
      case AccountCreated(_, mId, name, _) =>
        assertEquals(mId, memberId)
        assertEquals(name, "Savings")
      case _ => fail(s"Expected AccountCreated, got $event")
