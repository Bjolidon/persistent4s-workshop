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

  override def filter: Set[EventTypeName] = Set(
    EventTypeName.of[AccountCreated],
    EventTypeName.of[AccountClosed],
    EventTypeName.of[Deposited],
    EventTypeName.of[Withdrawn],
    EventTypeName.of[Transferred]
  )

  override def resolveKeys(event: EventEnvelope[BankEvent]): List[UUID] =
    event.payload match {
      case AccountCreated(accountId, _, _, _) => List(accountId)
      case AccountClosed(accountId, _)        => List(accountId)
      case Deposited(_, accountId, _, _)      => List(accountId)
      case Withdrawn(_, accountId, _, _)      => List(accountId)
      case Transferred(_, debitAccountId, creditAccountId, _, _) =>
        List(debitAccountId, creditAccountId)
      case _ => Nil
    }

  override def handle(
      state: Option[Account],
      event: EventEnvelope[BankEvent]
  ): IO[Option[Account]] =
    (state, event.payload) match
      case (None, AccountCreated(accountId, memberId, name, createdAt)) =>
        IO.pure(
          Some(
            Account(
              accountId = accountId,
              memberId = memberId,
              name = name,
              balance = 0,
              createdAt = createdAt,
              closedAt = None
            )
          )
        )
      case (Some(s), AccountClosed(accountId, closedAt)) =>
        IO.pure(Some(s.copy(closedAt = Some(closedAt))))
      case (Some(s), Deposited(_, accountId, amount, _)) =>
        IO.pure(Some(s.copy(balance = s.balance + amount)))
      case (Some(s), Withdrawn(_, accountId, amount, _)) =>
        IO.pure(Some(s.copy(balance = s.balance - amount)))
      case (
            Some(s),
            Transferred(_, debitAccountId, creditAccountId, amount, _)
          ) if s.accountId == debitAccountId =>
        IO.pure(Some(s.copy(balance = s.balance - amount)))
      case (
            Some(s),
            Transferred(_, debitAccountId, creditAccountId, amount, _)
          ) if s.accountId == creditAccountId =>
        IO.pure(Some(s.copy(balance = s.balance + amount)))
      case _ =>
        IO.raiseError(
          new RuntimeException(
            s"Failed to apply event: ${event.payload} to state: $state"
          )
        )

}

object AccountProjection {
  def make(repository: AccountRepository): AccountProjection =
    new AccountProjection(repository)
}
