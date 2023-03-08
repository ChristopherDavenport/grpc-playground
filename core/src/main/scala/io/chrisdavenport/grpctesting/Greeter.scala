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
import Hello.HelloReply

/*
  rpc sayHello (HelloRequest) returns (HelloReply) {}
  rpc sayHelloAlot (HelloRequest) returns (stream HelloReply) {}
  rpc sayHelloTiny (stream HelloRequest) returns (HelloReply) {}
  rpc sayHelloToInfinity (stream HelloRequest) returns (stream HelloReply) {}
*/
trait Greeter[F[_]]{
  def sayHello(request: _root_.Hello.HelloRequest, ctx: Headers): F[_root_.Hello.HelloReply]
  def sayHelloAlot(request: _root_.Hello.HelloRequest, ctx: Headers): fs2.Stream[F, _root_.Hello.HelloReply]
  def sayHelloTiny(request: fs2.Stream[F, _root_.Hello.HelloRequest], ctx: Headers): F[_root_.Hello.HelloReply]
  def sayHelloToInfinity(request: fs2.Stream[F, _root_.Hello.HelloRequest], ctx: Headers): fs2.Stream[F, _root_.Hello.HelloReply]
}


object Greeter {

  def service[F[_]: Async](greeter: Greeter[F]): HttpRoutes[F] = {
    org.http4s.grpc.ServerGrpc.unaryToUnary(
      org.http4s.grpc.codecs.ScalaPb.codecForGenerated(Hello.HelloRequest),
      org.http4s.grpc.codecs.ScalaPb.codecForGenerated(Hello.HelloReply),
      "Greeter",
      "sayHello"
    )(greeter.sayHello) <+>
    org.http4s.grpc.ServerGrpc.unaryToStream(
      org.http4s.grpc.codecs.ScalaPb.codecForGenerated(Hello.HelloRequest),
      org.http4s.grpc.codecs.ScalaPb.codecForGenerated(Hello.HelloReply),
      "Greeter",
      "sayHelloAlot"
    )(greeter.sayHelloAlot) <+>
    org.http4s.grpc.ServerGrpc.streamToUnary(
      org.http4s.grpc.codecs.ScalaPb.codecForGenerated(Hello.HelloRequest),
      org.http4s.grpc.codecs.ScalaPb.codecForGenerated(Hello.HelloReply),
      "Greeter",
      "sayHelloTiny"
    )(greeter.sayHelloTiny)
    org.http4s.grpc.ServerGrpc.streamToStream(
      org.http4s.grpc.codecs.ScalaPb.codecForGenerated(Hello.HelloRequest),
      org.http4s.grpc.codecs.ScalaPb.codecForGenerated(Hello.HelloReply),
      "Greeter",
      "sayHelloToInfinity"
    )(greeter.sayHelloToInfinity)
  }

  def client[F[_]: Async](client: _root_.org.http4s.client.Client[F], baseUri: _root_.org.http4s.Uri): Greeter[F] = new Greeter[F]{
    def sayHello(request: HelloRequest, ctx: Headers): F[HelloReply] =
      org.http4s.grpc.ClientGrpc.unaryToUnary(
        org.http4s.grpc.codecs.ScalaPb.codecForGenerated(Hello.HelloRequest),
        org.http4s.grpc.codecs.ScalaPb.codecForGenerated(Hello.HelloReply),
        "Greeter",
        "sayHello"
      )(client, baseUri)(request, ctx)

    def sayHelloAlot(request: HelloRequest, ctx: Headers): fs2.Stream[F,HelloReply] =
      org.http4s.grpc.ClientGrpc.unaryToStream(
        org.http4s.grpc.codecs.ScalaPb.codecForGenerated(Hello.HelloRequest),
        org.http4s.grpc.codecs.ScalaPb.codecForGenerated(Hello.HelloReply),
        "Greeter",
        "sayHelloAlot"
      )(client, baseUri)(request, ctx)

    
    def sayHelloTiny(request: fs2.Stream[F,HelloRequest], ctx: Headers): F[HelloReply] =
      org.http4s.grpc.ClientGrpc.streamToUnary(
        org.http4s.grpc.codecs.ScalaPb.codecForGenerated(Hello.HelloRequest),
        org.http4s.grpc.codecs.ScalaPb.codecForGenerated(Hello.HelloReply),
        "Greeter",
        "sayHelloTiny"
      )(client, baseUri)(request, ctx)

    
    def sayHelloToInfinity(request: fs2.Stream[F,HelloRequest], ctx: Headers): fs2.Stream[F,HelloReply] =
      org.http4s.grpc.ClientGrpc.streamToStream(
        org.http4s.grpc.codecs.ScalaPb.codecForGenerated(Hello.HelloRequest),
        org.http4s.grpc.codecs.ScalaPb.codecForGenerated(Hello.HelloReply),
        "Greeter",
        "sayHelloToInfinity"
      )(client, baseUri)(request, ctx)

  }


}