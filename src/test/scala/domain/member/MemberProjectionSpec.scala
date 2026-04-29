package domain.member

import munit.FunSuite
import java.util.UUID
import java.time.{Instant, LocalDate}
import cats.effect.{IO, Resource}
import cats.effect.unsafe.implicits.global
import skunk.Session
import persistent4s.{Event, EventEnvelope, EventFilter, EventMetadata, EventTypeName, Tag}
import domain.*

class MemberProjectionSpec extends FunSuite:

  val dummyPool: Resource[IO, Session[IO]] =
    Resource.eval(IO.raiseError(new RuntimeException("not a real DB")))
  val projection: MemberProjection = MemberProjection.make(MemberRepository.make(dummyPool))

  def envelope[A <: Event](payload: A): EventEnvelope[A] =
    EventEnvelope(
      metadata = EventMetadata(0L, Set.empty, EventTypeName.fromInstance(payload), Instant.now()),
      payload  = payload
    )

  test("filter includes only MemberRegistered"):
    val filter = projection.filter
    assert(filter.eventTypes.contains(EventTypeName.of[MemberRegistered]))
    assertEquals(filter.eventTypes.size, 1)

  test("resolveKeys extracts memberId from MemberRegistered"):
    val memberId = UUID.randomUUID()
    val keys     = projection.resolveKeys(envelope(MemberRegistered(memberId, "Alice", LocalDate.of(1990, 1, 1))))
    assertEquals(keys, List(memberId))

  test("resolveKeys returns Nil for unrelated events"):
    val keys = projection.resolveKeys(envelope(AccountCreated(UUID.randomUUID(), UUID.randomUUID(), "x", java.time.LocalDateTime.now())))
    assertEquals(keys, Nil)

  test("handle creates a new Member from None + MemberRegistered"):
    val memberId = UUID.randomUUID()
    val event    = MemberRegistered(memberId, "Alice", LocalDate.of(1990, 1, 1))
    val result   = projection.handle(None, envelope(event)).unsafeRunSync()
    assertEquals(result, Some(Member(memberId, "Alice", LocalDate.of(1990, 1, 1))))

  test("handle raises error when state already exists for MemberRegistered"):
    val memberId       = UUID.randomUUID()
    val existingMember = Member(memberId, "Alice", LocalDate.of(1990, 1, 1))
    val event          = MemberRegistered(memberId, "Alice", LocalDate.of(1990, 1, 1))
    intercept[RuntimeException]:
      projection.handle(Some(existingMember), envelope(event)).unsafeRunSync()
