package infrastructure

import cats.effect.IOApp
import cats.effect.IO
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.*
import scala.concurrent.duration.*

object BankServer extends IOApp.Simple {

  override def run: IO[Unit] =
    BankModule.make("persistent4s.postgres").use { module =>
      BankRoutes.make(module).use { routes =>
        EmberServerBuilder
          .default[IO]
          .withHost(host"0.0.0.0")
          .withPort(port"8686")
          .withShutdownTimeout(5.seconds)
          .withHttpApp(routes.orNotFound)
          .build
          .evalTap(_ =>
            IO.println(
              "Server started — Swagger UI: http://localhost:8686/docs"
            )
          )
          .useForever
      }
    }

}
