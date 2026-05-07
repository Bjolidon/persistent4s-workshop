package infrastructure

import domain.BankEvent
import persistent4s.EventCodec
import persistent4s.circe.CirceEventCodec
import cats.effect.{IO, Resource}
import persistent4s.postgres.PostgresConfig
import pureconfig.ConfigSource
import persistent4s.*
import persistent4s.monitoring.*
import domain.member.*
import domain.account.*
import domain.transaction.*
import persistent4s.postgres.PostgresModule
import org.typelevel.otel4s.trace.Tracer
import org.typelevel.otel4s.metrics.Meter
import skunk.Session
import skunk.TypingStrategy
import javax.management.monitor.Monitor

given Tracer[IO] = Tracer.Implicits.noop

given Meter[IO] = Meter.Implicits.noop

final case class BankModule private (
    val eventStore: EventStore[IO, BankEvent],
    val memberProjection: MemberProjection,
    val memberRepository: MemberRepository,
    val accountProjection: AccountProjection,
    val accountRepository: AccountRepository,
    val transactionProjection: TransactionProjection,
    val transactionRepository: TransactionRepository
)

object BankModule {

  val eventCodec: EventCodec[BankEvent] = CirceEventCodec.derived[BankEvent]

  def make(configPath: String): Resource[IO, BankModule] =
    for {
      // Step 4a: use PostgresModule.make to obtain eventStore and checkpointStore
      eventStoreComponents <- PostgresModule
        .make[IO, BankEvent](???, ???)
      eventStore = eventStoreComponents.eventStore
      checkpointStore = eventStoreComponents.checkpoint
      monitoring <- MonitoringServer.make(checkpointStore, eventStore.notify)
      config <- Resource.eval(loadConfig(configPath))
      sessionPool <- Session
        .Builder[IO]
        .withHost(config.host)
        .withPort(config.port)
        .withUserAndPassword(config.user, config.password)
        .withDatabase(config.database)
        .withTypingStrategy(TypingStrategy.SearchPath)
        .pooled(config.maxConnections)
      memberRepository = MemberRepository.make(sessionPool)
      memberProjection = MemberProjection.make(memberRepository)
      accountRepository = AccountRepository.make(sessionPool)
      accountProjection = AccountProjection.make(accountRepository)
      transactionRepository = TransactionRepository.make(sessionPool)
      transactionProjection = TransactionProjection.make(transactionRepository)
      // Step 4b: instantiate DefaultProjector and run the three projections in the background
      projector = DefaultProjector[IO, BankEvent](???, ???)
      _ <- projector.run(memberProjection).compile.drain.background
      _ <- projector.run(accountProjection).compile.drain.background
      _ <- projector.run(transactionProjection).compile.drain.background
    } yield new BankModule(
      eventStore,
      memberProjection,
      memberRepository,
      accountProjection,
      accountRepository,
      transactionProjection,
      transactionRepository
    )

  private def loadConfig(path: String): IO[PostgresConfig] =
    IO.delay(ConfigSource.default.at(path).load[PostgresConfig]).flatMap {
      case Right(config) => IO.pure(config)
      case Left(errors)  =>
        IO.raiseError(new RuntimeException(errors.prettyPrint()))
    }
}
