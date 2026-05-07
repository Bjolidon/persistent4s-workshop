package application

import domain.transaction.TransactionRepository
import cats.effect.IO
import bank.api.*
import bank.api.TransactionType as ApiTransactionType
import domain.transaction.TransactionType as DomainTransactionType
import persistent4s.EventStore
import domain.BankEvent
import java.util.UUID
import domain.transaction.commands.*
import smithy4s.time.Timestamp
import java.time.ZoneId

class TransactionServiceImpl(repository: TransactionRepository)(using
    EventStore[IO, BankEvent]
) extends TransactionService[IO] {

  override def withdraw(accountId: UUID, amount: Int): IO[WithdrawOutput] =
    for {
      transactionId <- IO(UUID.randomUUID())
      _ <- WithdrawHandler
        .run[IO](Withdraw(transactionId, accountId, amount))
        .adaptError { case e => ValidationError(e.getMessage) }
    } yield WithdrawOutput(transactionId)

  override def transfer(
      debitAccountId: UUID,
      creditAccountId: UUID,
      amount: Int
  ): IO[TransferOutput] =
    for {
      transactionId <- IO(UUID.randomUUID())
      _ <- TransferHandler
        .run[IO](
          Transfer(transactionId, debitAccountId, creditAccountId, amount)
        )
        .adaptError { case e => ValidationError(e.getMessage) }
    } yield TransferOutput(transactionId)

  override def deposit(accountId: UUID, amount: Int): IO[DepositOutput] =
    for {
      transactionId <- IO(UUID.randomUUID())
      _ <- DepositHandler
        .run[IO](Deposit(transactionId, accountId, amount))
        .adaptError { case e => ValidationError(e.getMessage) }
    } yield DepositOutput(transactionId)

  override def getTransaction(
      transactionId: UUID
  ): IO[GetTransactionOutput] = repository
    .find(transactionId)
    .map {
      case Some(transaction) =>
        GetTransactionOutput(
          Transaction(
            transactionId = transaction.transactionId,
            debitAccountId = transaction.debitAccountId,
            creditAccountId = transaction.creditAccountId,
            transactionType =
              TransactionTimeMapper.toApi(transaction.transactionType),
            amount = transaction.amount,
            timestamp = Timestamp
              .fromOffsetDateTime(
                transaction.timestamp
                  .atZone(ZoneId.of("Europe/Zurich"))
                  .toOffsetDateTime()
              )
          )
        )
      case None =>
        throw new NoSuchElementException(
          s"Transaction not found: $transactionId"
        )
    }

}

object TransactionTimeMapper:
  def toApi(t: DomainTransactionType): ApiTransactionType = t match
    case DomainTransactionType.Deposit =>
      ApiTransactionType.DEPOSIT
    case DomainTransactionType.Withdrawal =>
      ApiTransactionType.WITHDRAWAL
    case DomainTransactionType.Transfer =>
      ApiTransactionType.TRANSFER

  def toDomain(
      t: ApiTransactionType
  ): DomainTransactionType = t match
    case ApiTransactionType.DEPOSIT =>
      DomainTransactionType.Deposit
    case ApiTransactionType.WITHDRAWAL =>
      DomainTransactionType.Withdrawal
    case ApiTransactionType.TRANSFER =>
      DomainTransactionType.Transfer
