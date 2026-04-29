package domain.transaction

import munit.FunSuite
import java.util.UUID
import java.time.{Instant, LocalDateTime}
import cats.effect.{IO, Resource}
import cats.effect.unsafe.implicits.global
import skunk.Session
import persistent4s.{Event, EventEnvelope, EventFilter, EventMetadata, EventTypeName, Tag}
import domain.*

class TransactionProjectionSpec extends FunSuite:

  val dummyPool: Resource[IO, Session[IO]] =
    Resource.eval(IO.raiseError(new RuntimeException("not a real DB")))
  val projection: TransactionProjection =
    TransactionProjection.make(TransactionRepository.make(dummyPool))

  def envelope[A <: Event](payload: A): EventEnvelope[A] =
    EventEnvelope(
      metadata = EventMetadata(0L, Set.empty, EventTypeName.fromInstance(payload), Instant.now()),
      payload  = payload
    )

  test("filter includes Deposited, Withdrawn, Transferred"):
    val types = projection.filter.eventTypes
    assert(types.contains(EventTypeName.of[Deposited]))
    assert(types.contains(EventTypeName.of[Withdrawn]))
    assert(types.contains(EventTypeName.of[Transferred]))
    assertEquals(types.size, 3)

  test("resolveKeys extracts transactionId from Deposited"):
    val txId = UUID.randomUUID()
    val keys = projection.resolveKeys(envelope(Deposited(txId, UUID.randomUUID(), 100, LocalDateTime.now())))
    assertEquals(keys, List(txId))

  test("resolveKeys extracts transactionId from Withdrawn"):
    val txId = UUID.randomUUID()
    val keys = projection.resolveKeys(envelope(Withdrawn(txId, UUID.randomUUID(), 100, LocalDateTime.now())))
    assertEquals(keys, List(txId))

  test("resolveKeys extracts transactionId from Transferred"):
    val txId = UUID.randomUUID()
    val keys = projection.resolveKeys(envelope(Transferred(txId, UUID.randomUUID(), UUID.randomUUID(), 100, LocalDateTime.now())))
    assertEquals(keys, List(txId))

  test("handle creates a deposit Transaction from None + Deposited"):
    val txId      = UUID.randomUUID()
    val accountId = UUID.randomUUID()
    val now       = LocalDateTime.now()
    val event     = Deposited(txId, accountId, 200, now)
    val result    = projection.handle(None, envelope(event)).unsafeRunSync()
    assertEquals(
      result,
      Some(Transaction(txId, debitAccountId = None, creditAccountId = Some(accountId), TransactionType.Deposit, 200, now))
    )

  test("handle creates a withdrawal Transaction from None + Withdrawn"):
    val txId      = UUID.randomUUID()
    val accountId = UUID.randomUUID()
    val now       = LocalDateTime.now()
    val event     = Withdrawn(txId, accountId, 150, now)
    val result    = projection.handle(None, envelope(event)).unsafeRunSync()
    assertEquals(
      result,
      Some(Transaction(txId, debitAccountId = Some(accountId), creditAccountId = None, TransactionType.Withdrawal, 150, now))
    )

  test("handle creates a transfer Transaction from None + Transferred"):
    val txId     = UUID.randomUUID()
    val debitId  = UUID.randomUUID()
    val creditId = UUID.randomUUID()
    val now      = LocalDateTime.now()
    val event    = Transferred(txId, debitId, creditId, 300, now)
    val result   = projection.handle(None, envelope(event)).unsafeRunSync()
    assertEquals(
      result,
      Some(Transaction(txId, debitAccountId = Some(debitId), creditAccountId = Some(creditId), TransactionType.Transfer, 300, now))
    )
