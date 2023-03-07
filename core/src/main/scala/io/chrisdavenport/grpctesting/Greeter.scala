package io.chrisdavenport.grpctesting

import cats._
import cats.syntax.all._
import cats.data.Kleisli
import cats.effect._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import scodec.bits.ByteVector
import Hello.HelloRequest
import org.http4s.headers.`Content-Type`
import org.typelevel.ci._
import org.http4s.ember.core.h2._

/*
  rpc sayHello (HelloRequest) returns (HelloReply) {}
  rpc sayHelloAlot (HelloRequest) returns (stream HelloReply) {}
  rpc sayHelloTiny (stream HelloRequest) returns (HelloReply) {}
  rpc sayHelloToInfinity (stream HelloRequest) returns (stream HelloReply) {}
*/
trait Greeter[F[_]]{
  def sayHello(request: _root_.Hello.HelloRequest): F[_root_.Hello.HelloReply]
  def sayHelloAlot(request: _root_.Hello.HelloRequest): fs2.Stream[F, _root_.Hello.HelloReply]
  def sayHelloTiny(request: fs2.Stream[F, _root_.Hello.HelloRequest]): F[_root_.Hello.HelloReply]
  def sayHelloToInfinity(request: fs2.Stream[F, _root_.Hello.HelloRequest]): fs2.Stream[F, _root_.Hello.HelloReply]
}


object Greeter {

  def service[F[_]: Async](greeter: Greeter[F]): _root_.org.http4s.HttpRoutes[F] = {
    val dsl = Http4sDsl[F]; import dsl._

    HttpRoutes.of[F]{
      case ireq@method -> Root / "Greeter" / "sayHello" =>
        method match {
          case Method.POST =>
            for {
              _ <- Sync[F].delay(println(s"Made it to sayHello route $ireq"))
              arr <- ireq.body.compile.to(Array)
              trailers <- ireq.attributes.lookup(org.http4s.Message.Keys.TrailerHeaders[F]).sequence
              _ = println(trailers)
              bv = ByteVector.view(arr)
              message <- LengthPrefixedMessage.codec.decodeValue(bv.bits).toEither.leftMap(err => new RuntimeException(err.messageWithContext)).liftTo[F]
              reqT <- Sync[F].delay(_root_.Hello.HelloRequest.validate(message.message.toArray))
              req <- reqT.liftTo[F]
              resp <- greeter.sayHello(req)
              outArrI <- Sync[F].delay(_root_.Hello.HelloReply.toByteArray(resp))
              outArr <- LengthPrefixedMessage.codec.encode(
                LengthPrefixedMessage(false, ByteVector.view(outArrI))
              ).toEither
                .leftMap(err => new RuntimeException(err.messageWithContext))
                .liftTo[F]
                .map(_.bytes)
              trailers = Headers(
                "grpc-status" -> "0"
              ).pure[F]
              ct <- `Content-Type`.parse("application/grpc+proto").liftTo[F]
              out = {
                _root_.org.http4s.Response[F](_root_.org.http4s.Status.Ok, ireq.httpVersion)
                  .withEntity(outArr)
                  .withContentType(ct)
                  .putHeaders(org.http4s.headers.Trailer(cats.data.NonEmptyList.of(CIString("grpc-status"))))
                  .withTrailerHeaders(trailers)
              }
            } yield out
          case _ => NotFound()
        }
      case ireq@method -> Root / "Greeter" / "sayHelloAlot" =>
        method match {
          case Method.POST =>
            for {
              _ <- Sync[F].delay(println(s"Made it to sayHelloAlot route $ireq"))

              arr <- ireq.body.compile.to(Array)
              bv = ByteVector.view(arr)
              message <- LengthPrefixedMessage.codec.decodeValue(bv.bits).toEither.leftMap(err => new RuntimeException(err.messageWithContext)).liftTo[F]
              reqT <- Sync[F].delay(_root_.Hello.HelloRequest.validate(message.message.toArray))
              req <- reqT.liftTo[F]


              s =
                greeter.sayHelloAlot(req)
                .map(reply => LengthPrefixedMessage(false, ByteVector.view(_root_.Hello.HelloReply.toByteArray(reply))))
                .through(fs2.interop.scodec.StreamEncoder.many(LengthPrefixedMessage.codec).toPipeByte)

              trailers = Headers(
                "grpc-status" -> "0"
              ).pure[F]
              ct <- `Content-Type`.parse("application/grpc+proto").liftTo[F]
              out = {
                _root_.org.http4s.Response[F](_root_.org.http4s.Status.Ok, ireq.httpVersion)
                  .withBodyStream(s)
                  .withContentType(ct)
                  .putHeaders(org.http4s.headers.Trailer(cats.data.NonEmptyList.of(CIString("grpc-status"))))
                  .withTrailerHeaders(trailers)
              }
            } yield out
          case _ => NotFound()
        }

      case ireq@method -> Root / "Greeter" / "sayHelloTiny" =>
        method match {
          case Method.POST =>
            for {
              _ <- Sync[F].delay(println(s"Made it to sayHelloTiny route $ireq"))
              s = ireq.body
                .through(fs2.interop.scodec.StreamDecoder.many(LengthPrefixedMessage.codec).toPipeByte)
                .evalMap(lpm => Sync[F].delay(_root_.Hello.HelloRequest.validate(lpm.message.toArray)))
                .evalMap(_.liftTo[F])
              resp <- greeter.sayHelloTiny(s)
              outArrI <- Sync[F].delay(_root_.Hello.HelloReply.toByteArray(resp))
              outArr <- LengthPrefixedMessage.codec.encode(
                LengthPrefixedMessage(false, ByteVector.view(outArrI))
              ).toEither
                .leftMap(err => new RuntimeException(err.messageWithContext))
                .liftTo[F]
                .map(_.bytes)

              trailers = Headers(
                "grpc-status" -> "0"
              ).pure[F]
              ct <- `Content-Type`.parse("application/grpc+proto").liftTo[F]
              out = {
                _root_.org.http4s.Response[F](_root_.org.http4s.Status.Ok, ireq.httpVersion)
                  .withEntity(outArr)
                  .withContentType(ct)
                  .putHeaders(org.http4s.headers.Trailer(cats.data.NonEmptyList.of(CIString("grpc-status"))))
                  .withTrailerHeaders(trailers)
              }
            } yield out
          case _ => NotFound()
        }

      case ireq@method -> Root / "Greeter" / "sayHelloToInfinity" =>
        method match {
          case Method.POST =>
            for {
              _ <- Sync[F].delay(println(s"Made it to sayHelloToInfinity route $ireq"))
              s = ireq.body
                .through(fs2.interop.scodec.StreamDecoder.many(LengthPrefixedMessage.codec).toPipeByte)
                .evalMap(lpm => Sync[F].delay(_root_.Hello.HelloRequest.validate(lpm.message.toArray)))
                .evalMap(_.liftTo[F])
                .through(greeter.sayHelloToInfinity)
                .map(reply => LengthPrefixedMessage(false, ByteVector.view(_root_.Hello.HelloReply.toByteArray(reply))))
                .through(fs2.interop.scodec.StreamEncoder.many(LengthPrefixedMessage.codec).toPipeByte)

              trailers = Headers(
                "grpc-status" -> "0"
              ).pure[F]
              ct <- `Content-Type`.parse("application/grpc+proto").liftTo[F]
              out = {
                _root_.org.http4s.Response[F](_root_.org.http4s.Status.Ok, ireq.httpVersion)
                  .withBodyStream(s)
                  .withContentType(ct)
                  .putHeaders(org.http4s.headers.Trailer(cats.data.NonEmptyList.of(CIString("grpc-status"))))
                  .withTrailerHeaders(trailers)
              }
            } yield out
          case _ => NotFound()
        }
    }
  }

  def client[F[_]: Async](client: _root_.org.http4s.client.Client[F], baseUri: _root_.org.http4s.Uri): Greeter[F] = new Greeter[F]{
    def sayHello(request: _root_.Hello.HelloRequest): F[_root_.Hello.HelloReply] = {
      val encodedRequest = for {
        outArrI <- Sync[F].delay(_root_.Hello.HelloRequest.toByteArray(request))
        outArr <- LengthPrefixedMessage.codec.encode(
          LengthPrefixedMessage(false, ByteVector.view(outArrI))
        ).toEither
          .leftMap(err => new RuntimeException(err.messageWithContext))
          .liftTo[F]
          .map(_.bytes)
      } yield outArr
      val req = _root_.org.http4s.Request[F](_root_.org.http4s.Method.POST, baseUri / "Greeter" / "sayHello" )

      (
        `Content-Type`.parse("application/grpc+proto").liftTo[F],
        encodedRequest
      ).tupled.flatMap{ case (contentType, encoded) =>
        client.run(
          _root_.org.http4s.Request[F](_root_.org.http4s.Method.POST, baseUri / "Greeter" / "sayHello" )
            .withEntity(encoded)
            .withContentType(contentType)
            .withAttribute(H2Keys.Http2PriorKnowledge, ())

        ).use{ resp =>

          for {
              arr <- resp.body.compile.to(Array)
              bv = ByteVector.view(arr)
              message <- LengthPrefixedMessage.codec.decodeValue(bv.bits).toEither.leftMap(err => new RuntimeException(err.messageWithContext)).liftTo[F]
              reqT <- Sync[F].delay(_root_.Hello.HelloReply.validate(message.message.toArray))
              req <- reqT.liftTo[F]
          } yield req
        }
      }
    }

    def sayHelloAlot(request: Hello.HelloRequest): fs2.Stream[F,Hello.HelloReply] = ???
    def sayHelloTiny(request: fs2.Stream[F,Hello.HelloRequest]): F[Hello.HelloReply] = ???
    def sayHelloToInfinity(request: fs2.Stream[F,Hello.HelloRequest]): fs2.Stream[F,Hello.HelloReply] = ???

  }

  case class LengthPrefixedMessage(compressed: Boolean, message: ByteVector)
  object LengthPrefixedMessage {
    import scodec._
    import scodec.bits._
    import scodec.codecs._
    val codec: scodec.Codec[LengthPrefixedMessage] = (uint8.xmap[Boolean](_ === 1, { case true => 1; case false => 0 }) :: variableSizeBytesLong(uint32, bytes)).as[LengthPrefixedMessage]

  }

}