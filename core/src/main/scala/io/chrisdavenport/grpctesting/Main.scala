package io.chrisdavenport.grpctesting

import cats.syntax.all._
import cats.effect._
import com.comcast.ip4s._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.ember.client.EmberClientBuilder
import Hello.{HelloReply, HelloRequest}
import org.http4s._

object ServerExample extends IOApp {
  val greeter = new Greeter[IO] {
    def sayHello(request: HelloRequest, ctx: Headers): IO[HelloReply] =
      IO.println(request) >>
      IO(HelloReply(request.name))

    def sayHelloAlot(request: Hello.HelloRequest, ctx: Headers): fs2.Stream[cats.effect.IO,Hello.HelloReply] =
      fs2.Stream.eval(IO.println(request)).drain ++
      fs2.Stream(Hello.HelloReply(request.name))
        .covary[IO]
        .repeat
        .take(5)

    def sayHelloTiny(request: fs2.Stream[cats.effect.IO,Hello.HelloRequest], ctx: Headers): cats.effect.IO[Hello.HelloReply] =
      request.take(1).compile.to(List).map(_.headOption.fold(Hello.HelloReply("Unknown"))(req => Hello.HelloReply(req.name)))

    def sayHelloToInfinity(request: fs2.Stream[cats.effect.IO,Hello.HelloRequest], ctx: Headers): fs2.Stream[cats.effect.IO,Hello.HelloReply] =
      request.map(r => Hello.HelloReply(r.name))
  }

  val routeFallback = HttpRoutes.of[IO]{
    case req => IO(println(req)).as(Response[IO](Status.NotFound))
  }

  val app = Greeter.service(greeter) <+> routeFallback

  def run(args: List[String]): IO[ExitCode] = {
    EmberServerBuilder.default[IO]
      .withHttp2
      .withHost(host"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(org.http4s.server.middleware.Logger.httpApp(true,false)(app.orNotFound))
      .build
      .useForever
      .as(ExitCode.Success)
  }

  

}

object ClientExample extends IOApp {
  import org.http4s.implicits._
  def run(args: List[String]): IO[ExitCode] = {
    EmberClientBuilder.default[IO]
      .withHttp2
      .build
      .use{ iclient =>
        val client = org.http4s.client.middleware.Logger(true, true, logAction = {s: String => IO.println(s)}.some)(iclient)
        val greeter = Greeter.client(client, uri"http://localhost:9999")

        greeter.sayHelloTiny(fs2.Stream(HelloRequest("Chris"), HelloRequest("Sarah")), Headers())
          // .compile
          // .toList
          .flatTap(IO.println)

      }
  }.as(ExitCode.Success)
}