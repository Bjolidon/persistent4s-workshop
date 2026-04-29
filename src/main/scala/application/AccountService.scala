package application

import domain.account.AccountRepository
import cats.effect.IO
import bank.api.*
import java.util.UUID
import java.time.ZoneOffset
import smithy4s.time.Timestamp
import persistent4s.EventStore
import domain.BankEvent
import java.time.ZoneId
import domain.account.commands.*

class AccountServiceImpl(repository: AccountRepository)(using
    EventStore[IO, BankEvent]
) extends AccountService[IO] {

  override def getAccount(accountId: UUID): IO[GetAccountOutput] = repository
    .find(accountId)
    .map {
      case Some(account) =>
        GetAccountOutput(
          Account(
            accountId = account.accountId,
            memberId = account.memberId,
            name = account.name,
            balance = account.balance,
            createdAt = Timestamp
              .fromOffsetDateTime(
                account.createdAt
                  .atZone(ZoneId.of("Europe/Zurich"))
                  .toOffsetDateTime()
              ),
            closedAt = account.closedAt
              .map(closed =>
                Timestamp
                  .fromOffsetDateTime(
                    closed.atZone(ZoneId.of("Europe/Zurich")).toOffsetDateTime()
                  )
              )
          )
        )
      case None =>
        throw new NoSuchElementException(s"Account not found: $accountId")
    }

  override def closeAccount(accountId: UUID): IO[CloseAccountOutput] =
    for {
      _ <- CloseAccountHandler.run[IO](CloseAccount(accountId))
    } yield CloseAccountOutput()

  override def createAccount(
      memberId: UUID,
      name: String
  ): IO[CreateAccountOutput] =
    for {
      id <- IO(UUID.randomUUID())
      _ <- CreateAccountHandler
        .run[IO](CreateAccount(id, memberId, name))
        .adaptError { case e => ValidationError(e.getMessage) }
    } yield CreateAccountOutput(id)

  override def getAccountsByMember(
      memberId: UUID
  ): IO[GetAccountsByMemberOutput] = repository
    .findByMemberId(memberId)
    .map(accounts =>
      GetAccountsByMemberOutput(
        accounts.map(account =>
          Account(
            accountId = account.accountId,
            memberId = account.memberId,
            name = account.name,
            balance = account.balance,
            createdAt = Timestamp
              .fromOffsetDateTime(
                account.createdAt
                  .atZone(ZoneId.of("Europe/Zurich"))
                  .toOffsetDateTime()
              ),
            closedAt = account.closedAt
              .map(closed =>
                Timestamp
                  .fromOffsetDateTime(
                    closed.atZone(ZoneId.of("Europe/Zurich")).toOffsetDateTime()
                  )
              )
          )
        )
      )
    )

}
