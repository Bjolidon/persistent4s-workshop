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

  override def filter: Set[EventTypeName] =
    Set(
      EventTypeName.of[Deposited],
      EventTypeName.of[Withdrawn],
      EventTypeName.of[Transferred]
    )

  override def resolveKeys(
      event: EventEnvelope[BankEvent]
  ): List[UUID] = event.payload match
    case Deposited(transactionId, accountId, amount, timestamp) =>
      List(transactionId)
    case Withdrawn(transactionId, accountId, amount, timestamp) =>
      List(transactionId)
    case Transferred(
          transactionId,
          debitAccountId,
          creditAccountId,
          amount,
          timestamp
        ) =>
      List(transactionId)
    case _ => Nil

  override def handle(
      state: Option[Transaction],
      event: EventEnvelope[BankEvent]
  ): IO[Option[Transaction]] = (state, event.payload) match
    case (
          None,
          Deposited(transactionId, accountId, amount, timestamp)
        ) =>
      IO.pure(
        Some(
          Transaction(
            transactionId = transactionId,
            debitAccountId = None,
            creditAccountId = Some(accountId),
            transactionType = TransactionType.Deposit,
            amount = amount,
            timestamp = timestamp
          )
        )
      )
    case (
          None,
          Withdrawn(transactionId, accountId, amount, timestamp)
        ) =>
      IO.pure(
        Some(
          Transaction(
            transactionId = transactionId,
            debitAccountId = Some(accountId),
            creditAccountId = None,
            transactionType = TransactionType.Withdrawal,
            amount = amount,
            timestamp = timestamp
          )
        )
      )
    case (
          None,
          Transferred(
            transactionId,
            debitAccountId,
            creditAccountId,
            amount,
            timestamp
          )
        ) =>
      IO.pure(
        Some(
          Transaction(
            transactionId = transactionId,
            debitAccountId = Some(debitAccountId),
            creditAccountId = Some(creditAccountId),
            transactionType = TransactionType.Transfer,
            amount = amount,
            timestamp = timestamp
          )
        )
      )
    case _ =>
      IO.raiseError(
        new RuntimeException(
          s"Failed to apply event: ${event.payload} to state: $state"
        )
      )

}

object TransactionProjection {
  def make(repository: TransactionRepository): TransactionProjection =
    new TransactionProjection(repository)
}
