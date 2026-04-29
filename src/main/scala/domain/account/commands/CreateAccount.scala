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

  override def tags(command: CreateAccount): Set[Tag] = Set(
    Tag("member", command.memberId.toString)
  )

  override def initial: AccountCreatedState =
    AccountCreatedState(memberExists = false)

  override def evolve(
      command: CreateAccount,
      state: AccountCreatedState,
      event: BankEvent
  ): AccountCreatedState = event match {
    case MemberRegistered(id, _, _) if id == command.memberId =>
      state.copy(memberExists = true)
    case _ => state
  }

  override def validate(
      state: AccountCreatedState,
      command: CreateAccount
  ): Either[Throwable, Unit] =
    if (!state.memberExists) Left(new Exception("Member does not exist"))
    else Right(())

  override def decide(
      state: AccountCreatedState,
      command: CreateAccount
  ): List[(Set[Tag], BankEvent)] = {
    val id = UUID.randomUUID()
    List(
      (
        Set(
          Tag("member", command.memberId.toString),
          Tag("account", id.toString)
        ),
        AccountCreated(id, command.memberId, command.name, LocalDateTime.now())
      )
    )
  }

}
