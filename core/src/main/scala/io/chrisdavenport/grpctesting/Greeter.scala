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

trait Greeter[F[_]]{
  def sayHello(request: _root_.Hello.HelloRequest): F[_root_.Hello.HelloReply]
}

object Greeter {
  def service[F[_]: Async](greeter: Greeter[F]): _root_.org.http4s.HttpRoutes[F] = {
    val dsl = Http4sDsl[F]; import dsl._

    HttpRoutes.of[F]{
      case req@method -> Root / "Greeter" / "sayHello" => 
        method match {
          case Method.POST => 
            for {
              _ <- Sync[F].delay(println(s"Made it to our route $req"))
              arr <- req.body.compile.to(Array)
              bv = ByteVector.view(arr)
              compressed = bv(0) == 1
              size = bv.slice(1, 4).toInt(false)
              _ = println(s"Size was $size")
              rest = bv.drop(5).toArray
              reqT <- Sync[F].delay(_root_.Hello.HelloRequest.validate(rest))
              req <- reqT.liftTo[F]
              resp <- greeter.sayHello(req)
              outArrI <- Sync[F].delay(_root_.Hello.HelloReply.toByteArray(resp))
              outArr = Array[Byte](0) ++ ByteVector.fromInt(outArrI.size).toArray ++ outArrI
              trailers = Headers(
                "grpc-status" -> "0"
              ).pure[F]
              ct <- `Content-Type`.parse("application/grpc+proto").liftTo[F]
              out <- Ok(outArr).map(
                _.withContentType(ct)
                  .putHeaders(org.http4s.headers.Trailer(cats.data.NonEmptyList.of(CIString("grpc-status"))))
                  .withTrailerHeaders(trailers)
              )
            } yield out
          case _ => Ok()
        }
      
    }
  }
}