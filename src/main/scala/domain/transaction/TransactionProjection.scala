package domain.transaction

import persistent4s.Projection
import cats.effect.IO
import domain.BankEvent
import java.util.UUID
import persistent4s.EventEnvelope
import persistent4s.EventFilter
import persistent4s.EventTypeName
import domain.*

final class TransactionProjection private (
    protected val repository: TransactionRepository
) extends Projection[IO, BankEvent, UUID, Transaction] {

  override def name: String = "TransactionProjection"

  override def filter: Set[EventTypeName] = ???

  override def resolveKeys(
      event: EventEnvelope[BankEvent]
  ): List[UUID] = ???

  override def handle(
      state: Option[Transaction],
      event: EventEnvelope[BankEvent]
  ): IO[Option[Transaction]] = ???

}

object TransactionProjection {
  def make(repository: TransactionRepository): TransactionProjection =
    new TransactionProjection(repository)
}
