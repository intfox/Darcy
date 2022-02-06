package ru.intfox.darcy

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.server.Router
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits._
import ru.intfox.darcy.services.MessageDescriptorsStorage
import ru.intfox.darcy.api.{MessagesApi, ProtoApi}

import scala.concurrent.ExecutionContext.global

object Main extends IOApp {

  val messagesDescriptorStorage = MessageDescriptorsStorage.runtime()
  val routes = Router("/api/proto" -> ProtoApi(messagesDescriptorStorage), "/api/messages" -> MessagesApi(messagesDescriptorStorage)).orNotFound

  override def run(args: List[String]): IO[ExitCode] = for {
    _ <- BlazeServerBuilder[IO](global)
      .withHttpApp(routes)
      .bindHttp(8080, "localhost")
      .serve
      .compile
      .drain
  } yield ExitCode.Success
}
