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

  override def tags(command: Deposit): Set[Tag] = ???

  override def initial: DepositState = ???

  override def evolve(
      command: Deposit,
      state: DepositState,
      event: BankEvent
  ): DepositState = ???

  override def validate(
      state: DepositState,
      command: Deposit
  ): Either[Throwable, Unit] = ???

  override def decide(
      state: DepositState,
      command: Deposit
  ): List[(Set[Tag], BankEvent)] = ???

}
