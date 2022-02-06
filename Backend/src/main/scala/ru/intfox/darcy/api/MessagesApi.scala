package ru.intfox.darcy.api

import org.http4s.HttpRoutes
import ru.intfox.darcy.services.MessageDescriptorsStorage
import cats.effect.IO
import io.circe.generic.semiauto._
import io.circe.{ Encoder, Json }
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.io._
import MessagesApi.Model._
import com.google.protobuf.DescriptorProtos.{ DescriptorProto, FieldDescriptorProto }
import com.google.protobuf.Descriptors.FieldDescriptor

import com.google.protobuf.Descriptors
import scala.jdk.CollectionConverters._

import cats.implicits._

class MessagesApi(messagesStorage: MessageDescriptorsStorage) {
  private val logger = org.log4s.getLogger

  def getMessages(): IO[List[Message]] = for {
    messages <- messagesStorage.getAllMessagesDescriptor()
    messages <- messages.map(message => messageFromDescriptor(message.descriptor, message.name)).sequence
  } yield messages

  private def messageFromDescriptor(descriptor: DescriptorProto, name: String): IO[Message] = for {
    fields <- List.from(descriptor.getFieldList.asScala).map(fieldFromDescriptor).sequence
    message <- IO(Message(
      name = name,
      fields = fields
    ))
  } yield message

  private def fieldFromDescriptor(descriptor: FieldDescriptorProto): IO[Field] = descriptor.getType match {
    case FieldDescriptorProto.Type.TYPE_MESSAGE => for {
      message <- messagesStorage.getMessageByName(descriptor.getTypeName.drop(1))
      field <- messageFromDescriptor(message.descriptor, descriptor.getName)
    } yield field
    case FieldDescriptorProto.Type.TYPE_BOOL => Scalar(descriptor.getName, BooleanType).pure[IO]
    case FieldDescriptorProto.Type.TYPE_DOUBLE => Scalar(descriptor.getName, FloatType).pure[IO]
    case FieldDescriptorProto.Type.TYPE_FLOAT => Scalar(descriptor.getName, FloatType).pure[IO]
    case FieldDescriptorProto.Type.TYPE_INT32 => Scalar(descriptor.getName, IntegerType).pure[IO]
    case FieldDescriptorProto.Type.TYPE_INT64 => Scalar(descriptor.getName, IntegerType).pure[IO]
    case FieldDescriptorProto.Type.TYPE_STRING => Scalar(descriptor.getName, StringType).pure[IO]
    case _ => Unsupported.pure[IO]
  }
}

object MessagesApi {
  object Model {
    implicit val typeEncoder = Encoder.instance[Type] {
      case StringType => Json.fromString("string")
      case IntegerType => Json.fromString("integer")
      case FloatType => Json.fromString("float")
      case BooleanType => Json.fromString("boolean")
      case RepeatedType => Json.fromString("repeated")
    }
    implicit val scalarEncoder = deriveEncoder[Scalar]
    implicit val messageEncoder = new Encoder[Message] {
      override def apply(a: Message): Json = Json.obj(
        ("name", a.name.asJson),
        ("fields", a.fields.map{
          case m: Message => apply(m)
          case s: Scalar => s.asJson
        }.asJson)
      )
    }
    implicit val fieldEncoder = Encoder.instance[Field] {
      case msg @ Message(_, _) => msg.asJson
      case scalar @ Scalar(_, _) => scalar.asJson
      case Unsupported => Json.obj(("type", Json.fromString("unsupported")))
    }
    implicit val messagesResponseEncoder = deriveEncoder[MessagesResponse]

    case class Message(name: String, fields: List[Field]) extends Field

    sealed trait Field
    case class Scalar(name: String, `type`: Type) extends Field

    sealed trait Type
    object StringType extends Type
    object IntegerType extends Type
    object FloatType extends Type
    object BooleanType extends Type
    object RepeatedType extends Type

    object Unsupported extends Field

    case class MessagesResponse(messages: List[Message])
  }

  import Model._
  def apply(messagesDescriptorStorage: MessageDescriptorsStorage): HttpRoutes[IO] = {
    val messagesApi = new MessagesApi(messagesDescriptorStorage)
    HttpRoutes.of{
      case GET -> Root => Ok(messagesApi.getMessages().map(MessagesResponse))
    }
  }

}