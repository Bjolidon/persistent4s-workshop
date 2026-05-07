package domain.account

import persistent4s.Projection
import cats.effect.IO
import domain.BankEvent
import java.util.UUID
import persistent4s.EventEnvelope
import persistent4s.EventFilter
import persistent4s.EventTypeName
import domain.*

final class AccountProjection private (
    protected val repository: AccountRepository
) extends Projection[IO, BankEvent, UUID, Account] {

  override def name: String = "AccountProjection"

  override def filter: Set[EventTypeName] = ???

  override def resolveKeys(event: EventEnvelope[BankEvent]): List[UUID] = ???

  override def handle(
      state: Option[Account],
      event: EventEnvelope[BankEvent]
  ): IO[Option[Account]] = ???

}

object AccountProjection {
  def make(repository: AccountRepository): AccountProjection =
    new AccountProjection(repository)
}
