package ru.intfox.darcy.services

import com.google.protobuf.DescriptorProtos.{DescriptorProto, FileDescriptorProto}
import cats.effect.IO

import scala.collection.concurrent.TrieMap
import cats.implicits._
import ru.intfox.darcy.services.MessageDescriptorsStorage.Message

trait MessageDescriptorsStorage {
  def createFromFileDescriptor(descriptor: FileDescriptorProto): IO[Unit]
  def getAllMessagesDescriptor(): IO[List[Message]]
  def getMessageByName(name: String): IO[Message]
}

object MessageDescriptorsStorage {
  def runtime(): MessageDescriptorsStorage = new MessageDescriptorsStorage {

    private val logger = org.log4s.getLogger
    private val messages = TrieMap[String, DescriptorProto]()

    override def createFromFileDescriptor(descriptor: FileDescriptorProto): IO[Unit] = for {
      _ <- IO {
        descriptor.getMessageTypeList.forEach { messageType =>
          logger.debug(s"Add message descriptor: ${messageType.getName} -> $messageType")
          messages.addOne(s"${descriptor.getPackage}.${messageType.getName}", messageType)
        }
      }
    } yield ()

    override def getAllMessagesDescriptor(): IO[List[Message]] = IO(messages.toList.map(msg => Message(msg._1, msg._2)))

    override def getMessageByName(name: String): IO[Message] = IO(messages.get(name)).flatMap(descriptor => descriptor.map(Message(name, _).pure[IO]).getOrElse(IO.raiseError(new Throwable(s"message by name $name not found"))))
  }

  case class Message(name: String, descriptor: DescriptorProto)
}
