package domain.transaction.commands

import domain.BankEvent
import persistent4s.CommandHandler
import persistent4s.Tag
import java.util.UUID
import java.time.LocalDateTime
import domain.account.*
import domain.transaction.*
import domain.*

final case class Deposit(
    transactionId: UUID,
    accountId: UUID,
    amount: Int
)

final case class DepositState(
    accountExists: Boolean,
    accountClosed: Boolean
)

object DepositHandler extends CommandHandler[Deposit, DepositState, BankEvent] {

  override def tags(command: Deposit): Set[Tag] = Set(
    Tag("account", command.accountId.toString)
  )

  override def initial: DepositState = DepositState(
    accountExists = false,
    accountClosed = false
  )

  override def evolve(
      command: Deposit,
      state: DepositState,
      event: BankEvent
  ): DepositState = event match {
    case AccountCreated(accountId, _, _, _) if accountId == command.accountId =>
      state.copy(accountExists = true)
    case AccountClosed(accountId, _) if accountId == command.accountId =>
      state.copy(accountClosed = true)
    case _ => state
  }

  override def validate(
      state: DepositState,
      command: Deposit
  ): Either[Throwable, Unit] =
    if (!state.accountExists) Left(new Exception("Account does not exist"))
    else if (state.accountClosed)
      Left(new Exception("Account is closed"))
    else if (command.amount <= 0)
      Left(new Exception("Deposit amount must be positive"))
    else Right(())

  override def decide(
      state: DepositState,
      command: Deposit
  ): List[(Set[Tag], BankEvent)] =
    List(
      (
        Set(Tag("account", command.accountId.toString)),
        Deposited(
          command.transactionId,
          command.accountId,
          command.amount,
          LocalDateTime.now()
        )
      )
    )

}
