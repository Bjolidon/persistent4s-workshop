package infrastructure

import persistent4s.EventStore
import domain.BankEvent
import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all.*
import org.http4s.HttpRoutes
import smithy4s.http4s.SimpleRestJsonBuilder
import application.*
import smithy4s.http4s.swagger.docs
import bank.api.*

object BankRoutes {

  def make(module: BankModule): Resource[IO, HttpRoutes[IO]] =
    given EventStore[IO, BankEvent] = module.eventStore

    for {
      memberRoutes <- SimpleRestJsonBuilder
        .routes(MemberServiceImpl(module.memberRepository))
        .resource
      accountRoutes <- SimpleRestJsonBuilder
        .routes(AccountServiceImpl(module.accountRepository))
        .resource
      transactionRoutes <- SimpleRestJsonBuilder
        .routes(TransactionServiceImpl(module.transactionRepository))
        .resource
      docsRoutes = docs[IO](
        MemberService,
        AccountService,
        TransactionService
      )
    } yield memberRoutes <+> accountRoutes <+> transactionRoutes <+> docsRoutes

}
