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

  override def tags(command: Transfer): Set[Tag] = Set(
    Tag("account", command.debitAccountId.toString),
    Tag("account", command.creditAccountId.toString)
  )

  override def initial: TransferState = TransferState(
    debitAccountExists = false,
    creditAccountExists = false,
    debitAccountBalance = 0,
    debitAccountClosed = false,
    creditAccountClosed = false
  )

  override def evolve(
      command: Transfer,
      state: TransferState,
      event: BankEvent
  ): TransferState =
    event match {
      case AccountCreated(accountId, _, _, _)
          if accountId == command.debitAccountId =>
        state.copy(debitAccountExists = true)
      case AccountCreated(accountId, _, _, _)
          if accountId == command.creditAccountId =>
        state.copy(creditAccountExists = true)
      case Deposited(_, accountId, amount, _)
          if accountId == command.debitAccountId =>
        state.copy(debitAccountBalance = state.debitAccountBalance + amount)
      case Withdrawn(_, accountId, amount, _)
          if accountId == command.debitAccountId =>
        state.copy(debitAccountBalance = state.debitAccountBalance - amount)
      case Transferred(_, debitAccountId, creditAccountId, amount, _) =>
        if (debitAccountId == command.debitAccountId) then
          state.copy(
            debitAccountBalance = state.debitAccountBalance - amount
          )
        else if (creditAccountId == command.debitAccountId) then
          state.copy(
            debitAccountBalance = state.debitAccountBalance + amount
          )
        else state
      case AccountClosed(accountId, _) if accountId == command.debitAccountId =>
        state.copy(debitAccountClosed = true)
      case AccountClosed(accountId, _)
          if accountId == command.creditAccountId =>
        state.copy(creditAccountClosed = true)
      case _ => state
    }

  override def validate(
      state: TransferState,
      command: Transfer
  ): Either[Throwable, Unit] = {
    if (command.debitAccountId == command.creditAccountId)
      Left(new Exception("Debit and credit accounts must be different"))
    else if (!state.debitAccountExists)
      Left(new Exception("Debit account does not exist"))
    else if (!state.creditAccountExists)
      Left(new Exception("Credit account does not exist"))
    else if (state.debitAccountClosed)
      Left(new Exception("Debit account is closed"))
    else if (state.creditAccountClosed)
      Left(new Exception("Credit account is closed"))
    else if (command.amount <= 0)
      Left(new Exception("Transaction amount must be positive"))
    else if (state.debitAccountBalance < command.amount)
      Left(new Exception("Insufficient balance in debit account"))
    else Right(())
  }

  override def decide(
      state: TransferState,
      command: Transfer
  ): List[(Set[Tag], BankEvent)] = {
    List(
      (
        Set(
          Tag("account", command.debitAccountId.toString),
          Tag("account", command.creditAccountId.toString)
        ),
        Transferred(
          command.transactionId,
          command.debitAccountId,
          command.creditAccountId,
          command.amount,
          LocalDateTime.now()
        )
      )
    )
  }

}
