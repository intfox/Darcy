package ru.intfox.darcy.api

import cats.effect.IO
import com.google.protobuf.DescriptorProtos.{FileDescriptorProto, FileDescriptorSet}
import fs2.io.file.Files
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import ru.intfox.darcy.services.MessageDescriptorsStorage

import java.io.FileInputStream
import java.nio.file.Path
import scala.sys.process._
import scala.util.Random


class ProtoApi(messagesStorage: MessageDescriptorsStorage) {
  private val logger = org.log4s.getLogger

  def postProto(protoFile: fs2.Stream[IO, Byte]): IO[Unit] = for {
    tmpProtoFileName <- IO(Random.nextInt(1000000) + ".proto")
    _ <- IO(logger.debug(s"Save file to $tmpProtoFileName start"))
    _ <- Files[IO].writeAll(Path.of(s"tmp/$tmpProtoFileName"))(protoFile).compile.drain
    _ <- IO(logger.debug(s"Save file to $tmpProtoFileName finally"))
    fileDescriptor <- parseProto(tmpProtoFileName)
    _ <- messagesStorage.createFromFileDescriptor(fileDescriptor)
  } yield ()

  def parseProto(tmpProtoFileName: String): IO[FileDescriptorProto] = for {
    _ <- IO(s"protoc tmp/$tmpProtoFileName --descriptor_set_out tmp/$tmpProtoFileName.binary".!)
    fileDescriptor <- IO{
      val inputStream = new FileInputStream(s"tmp/$tmpProtoFileName.binary")
      FileDescriptorSet.parseFrom(inputStream).getFile(0)
    }
  } yield fileDescriptor
}

object ProtoApi {
  def apply(messagesStorage: MessageDescriptorsStorage): HttpRoutes[IO] = {
    val apiTest = new ProtoApi(messagesStorage)
    HttpRoutes.of[IO] {
      case req @ POST -> Root => Ok(apiTest.postProto(req.body))
    }
  }
}