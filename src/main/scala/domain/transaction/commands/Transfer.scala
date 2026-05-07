package domain.transaction.commands

import domain.BankEvent
import persistent4s.CommandHandler
import persistent4s.Tag
import java.util.UUID
import java.time.LocalDateTime
import domain.account.*
import domain.transaction.*
import domain.*

final case class Transfer(
    transactionId: UUID,
    debitAccountId: UUID,
    creditAccountId: UUID,
    amount: Int
)

final case class TransferState(
    debitAccountExists: Boolean,
    creditAccountExists: Boolean,
    debitAccountBalance: Int,
    debitAccountClosed: Boolean,
    creditAccountClosed: Boolean
)

object TransferHandler
    extends CommandHandler[Transfer, TransferState, BankEvent] {

  override def tags(command: Transfer): Set[Tag] = ???

  override def initial: TransferState = ???

  override def evolve(
      command: Transfer,
      state: TransferState,
      event: BankEvent
  ): TransferState = ???

  override def validate(
      state: TransferState,
      command: Transfer
  ): Either[Throwable, Unit] = ???

  override def decide(
      state: TransferState,
      command: Transfer
  ): List[(Set[Tag], BankEvent)] = ???

}
