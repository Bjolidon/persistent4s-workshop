package domain.account.commands

import domain.BankEvent
import persistent4s.CommandHandler
import persistent4s.Tag
import java.time.LocalDateTime
import java.util.UUID
import domain.*
import domain.account.*

final case class CreateAccount(
    accountId: UUID,
    memberId: UUID,
    name: String
)

final case class AccountCreatedState(
    memberExists: Boolean
)

object CreateAccountHandler
    extends CommandHandler[CreateAccount, AccountCreatedState, BankEvent] {

  override def tags(command: CreateAccount): Set[Tag] = ???

  override def initial: AccountCreatedState = ???

  override def evolve(
      command: CreateAccount,
      state: AccountCreatedState,
      event: BankEvent
  ): AccountCreatedState = ???

  override def validate(
      state: AccountCreatedState,
      command: CreateAccount
  ): Either[Throwable, Unit] = ???

  override def decide(
      state: AccountCreatedState,
      command: CreateAccount
  ): List[(Set[Tag], BankEvent)] = ???

}
