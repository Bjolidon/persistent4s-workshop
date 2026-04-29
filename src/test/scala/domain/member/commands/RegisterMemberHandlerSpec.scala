package domain.member.commands

import munit.FunSuite
import java.util.UUID
import java.time.LocalDate
import domain.*
import persistent4s.Tag

class RegisterMemberHandlerSpec extends FunSuite:

  val memberId = UUID.fromString("00000000-0000-0000-0000-000000000001")
  val command  = RegisterMember(memberId, "Alice", LocalDate.of(1990, 1, 1))

  test("initial state is Unit"):
    assertEquals(RegisterMemberHandler.initial, ())

  test("evolve ignores all events"):
    val event  = MemberRegistered(UUID.randomUUID(), "Bob", LocalDate.of(1985, 6, 15))
    val result = RegisterMemberHandler.evolve(command, (), event)
    assertEquals(result, ())

  test("validate always succeeds"):
    assertEquals(RegisterMemberHandler.validate((), command), Right(()))

  test("decide emits MemberRegistered with member tag"):
    val results      = RegisterMemberHandler.decide((), command)
    assertEquals(results.size, 1)
    val (tags, event) = results.head
    assert(tags.contains(Tag("member", memberId.toString)))
    event match
      case MemberRegistered(id, name, birthDate) =>
        assertEquals(id, memberId)
        assertEquals(name, "Alice")
        assertEquals(birthDate, LocalDate.of(1990, 1, 1))
      case _ => fail(s"Expected MemberRegistered, got $event")
