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

  override def tags(command: Withdraw): Set[Tag] = ???

  override def initial: WithdrawState = ???

  override def evolve(
      command: Withdraw,
      state: WithdrawState,
      event: BankEvent
  ): WithdrawState = ???

  override def validate(
      state: WithdrawState,
      command: Withdraw
  ): Either[Throwable, Unit] = ???

  override def decide(
      state: WithdrawState,
      command: Withdraw
  ): List[(Set[Tag], BankEvent)] = ???

}
