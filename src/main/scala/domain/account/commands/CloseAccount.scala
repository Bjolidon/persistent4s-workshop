package domain.account.commands

import domain.BankEvent
import persistent4s.CommandHandler
import persistent4s.Tag
import java.time.LocalDateTime
import java.util.UUID
import domain.account.*
import domain.*

final case class CloseAccount(
    accountId: UUID
)

final case class CloseAccountState(
    accountExists: Boolean,
    accountClosed: Boolean
)

object CloseAccountHandler
    extends CommandHandler[CloseAccount, CloseAccountState, BankEvent] {

  override def tags(command: CloseAccount): Set[Tag] = Set(
    Tag("account", command.accountId.toString)
  )

  override def initial: CloseAccountState =
    CloseAccountState(accountExists = false, accountClosed = false)

  override def evolve(
      command: CloseAccount,
      state: CloseAccountState,
      event: BankEvent
  ): CloseAccountState = event match {
    case AccountCreated(accountId, _, _, _) if accountId == command.accountId =>
      state.copy(accountExists = true)
    case AccountClosed(accountId, _) if accountId == command.accountId =>
      state.copy(accountClosed = true)
    case _ => state
  }

  override def validate(
      state: CloseAccountState,
      command: CloseAccount
  ): Either[Throwable, Unit] =
    if (!state.accountExists) Left(new Exception("Account does not exist"))
    else if (state.accountClosed)
      Left(new Exception("Account is already closed"))
    else Right(())

  override def decide(
      state: CloseAccountState,
      command: CloseAccount
  ): List[(Set[Tag], BankEvent)] = {
    val now = LocalDateTime.now()
    List(
      (
        Set(Tag("account", command.accountId.toString)),
        AccountClosed(command.accountId, now)
      )
    )
  }
}
