package domain.transaction.commands

import domain.BankEvent
import persistent4s.CommandHandler
import persistent4s.Tag
import java.util.UUID
import java.time.LocalDateTime
import domain.account.*
import domain.transaction.*
import domain.*

final case class Withdraw(
    transactionId: UUID,
    accountId: UUID,
    amount: Int
)

final case class WithdrawState(
    accountExists: Boolean,
    balance: Int,
    accountClosed: Boolean
)

object WithdrawHandler
    extends CommandHandler[Withdraw, WithdrawState, BankEvent] {

  override def tags(command: Withdraw): Set[Tag] = Set(
    Tag("account", command.accountId.toString)
  )

  override def initial: WithdrawState = WithdrawState(
    accountExists = false,
    balance = 0,
    accountClosed = false
  )

  override def evolve(
      command: Withdraw,
      state: WithdrawState,
      event: BankEvent
  ): WithdrawState = event match {
    case AccountCreated(accountId, _, _, _) if accountId == command.accountId =>
      state.copy(accountExists = true)
    case Deposited(_, accountId, amount, _) if accountId == command.accountId =>
      state.copy(balance = state.balance + amount)
    case Withdrawn(_, accountId, amount, _) if accountId == command.accountId =>
      state.copy(balance = state.balance - amount)
    case Transferred(_, debitAccountId, creditAccountId, amount, _)
        if debitAccountId == command.accountId =>
      state.copy(balance = state.balance - amount)
    case Transferred(_, debitAccountId, creditAccountId, amount, _)
        if creditAccountId == command.accountId =>
      state.copy(balance = state.balance + amount)
    case AccountClosed(accountId, _) if accountId == command.accountId =>
      state.copy(accountClosed = true)
    case _ => state
  }

  override def validate(
      state: WithdrawState,
      command: Withdraw
  ): Either[Throwable, Unit] =
    if (!state.accountExists) Left(new Exception("Account does not exist"))
    else if (state.accountClosed)
      Left(new Exception("Account is closed"))
    else if (command.amount <= 0)
      Left(new Exception("Withdraw amount must be positive"))
    else if (state.balance < command.amount)
      Left(new Exception("Insufficient balance"))
    else Right(())

  override def decide(
      state: WithdrawState,
      command: Withdraw
  ): List[(Set[Tag], BankEvent)] =
    List(
      (
        Set(Tag("account", command.accountId.toString)),
        Withdrawn(
          command.transactionId,
          command.accountId,
          command.amount,
          LocalDateTime.now()
        )
      )
    )

}
