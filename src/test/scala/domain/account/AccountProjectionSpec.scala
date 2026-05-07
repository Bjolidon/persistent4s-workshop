package domain.account

import munit.FunSuite
import java.util.UUID
import java.time.{Instant, LocalDateTime}
import cats.effect.{IO, Resource}
import cats.effect.unsafe.implicits.global
import skunk.Session
import persistent4s.{
  Event,
  EventEnvelope,
  EventFilter,
  EventMetadata,
  EventTypeName,
  Tag
}
import domain.*

class AccountProjectionSpec extends FunSuite:

  val dummyPool: Resource[IO, Session[IO]] =
    Resource.eval(IO.raiseError(new RuntimeException("not a real DB")))
  val projection: AccountProjection =
    AccountProjection.make(AccountRepository.make(dummyPool))

  def envelope[A <: Event](payload: A): EventEnvelope[A] =
    EventEnvelope(
      metadata = EventMetadata(
        0L,
        Set.empty,
        EventTypeName.fromInstance(payload),
        Instant.now()
      ),
      payload = payload
    )

  test(
    "filter includes AccountCreated, AccountClosed, Deposited, Withdrawn, Transferred"
  ):
    val types = projection.filter
    assert(types.contains(EventTypeName.of[AccountCreated]))
    assert(types.contains(EventTypeName.of[AccountClosed]))
    assert(types.contains(EventTypeName.of[Deposited]))
    assert(types.contains(EventTypeName.of[Withdrawn]))
    assert(types.contains(EventTypeName.of[Transferred]))
    assertEquals(types.size, 5)

  test("resolveKeys extracts accountId from AccountCreated"):
    val accountId = UUID.randomUUID()
    val keys = projection.resolveKeys(
      envelope(
        AccountCreated(
          accountId,
          UUID.randomUUID(),
          "main",
          LocalDateTime.now()
        )
      )
    )
    assertEquals(keys, List(accountId))

  test(
    "resolveKeys extracts both account IDs from Transferred (DCB read side)"
  ):
    val debitId = UUID.randomUUID()
    val creditId = UUID.randomUUID()
    val keys = projection.resolveKeys(
      envelope(
        Transferred(
          UUID.randomUUID(),
          debitId,
          creditId,
          100,
          LocalDateTime.now()
        )
      )
    )
    assertEquals(keys.toSet, Set(debitId, creditId))

  test("handle creates an Account from None + AccountCreated"):
    val accountId = UUID.randomUUID()
    val memberId = UUID.randomUUID()
    val now = LocalDateTime.now()
    val event = AccountCreated(accountId, memberId, "Savings", now)
    val result = projection.handle(None, envelope(event)).unsafeRunSync()
    assertEquals(
      result,
      Some(Account(accountId, memberId, "Savings", 0, now, None))
    )

  test("handle sets closedAt on AccountClosed"):
    val accountId = UUID.randomUUID()
    val now = LocalDateTime.now()
    val account = Account(accountId, UUID.randomUUID(), "main", 0, now, None)
    val event = AccountClosed(accountId, now)
    val result =
      projection.handle(Some(account), envelope(event)).unsafeRunSync()
    assertEquals(result.flatMap(_.closedAt), Some(now))

  test("handle increases balance on Deposited"):
    val accountId = UUID.randomUUID()
    val account = Account(
      accountId,
      UUID.randomUUID(),
      "main",
      100,
      LocalDateTime.now(),
      None
    )
    val event = Deposited(UUID.randomUUID(), accountId, 50, LocalDateTime.now())
    val result =
      projection.handle(Some(account), envelope(event)).unsafeRunSync()
    assertEquals(result.map(_.balance), Some(150))

  test("handle decreases balance on Withdrawn"):
    val accountId = UUID.randomUUID()
    val account = Account(
      accountId,
      UUID.randomUUID(),
      "main",
      100,
      LocalDateTime.now(),
      None
    )
    val event = Withdrawn(UUID.randomUUID(), accountId, 30, LocalDateTime.now())
    val result =
      projection.handle(Some(account), envelope(event)).unsafeRunSync()
    assertEquals(result.map(_.balance), Some(70))

  test("handle decreases balance on Transferred when account is debit side"):
    val accountId = UUID.randomUUID()
    val account = Account(
      accountId,
      UUID.randomUUID(),
      "main",
      200,
      LocalDateTime.now(),
      None
    )
    val event = Transferred(
      UUID.randomUUID(),
      accountId,
      UUID.randomUUID(),
      80,
      LocalDateTime.now()
    )
    val result =
      projection.handle(Some(account), envelope(event)).unsafeRunSync()
    assertEquals(result.map(_.balance), Some(120))

  test("handle increases balance on Transferred when account is credit side"):
    val accountId = UUID.randomUUID()
    val account = Account(
      accountId,
      UUID.randomUUID(),
      "main",
      100,
      LocalDateTime.now(),
      None
    )
    val event = Transferred(
      UUID.randomUUID(),
      UUID.randomUUID(),
      accountId,
      80,
      LocalDateTime.now()
    )
    val result =
      projection.handle(Some(account), envelope(event)).unsafeRunSync()
    assertEquals(result.map(_.balance), Some(180))
