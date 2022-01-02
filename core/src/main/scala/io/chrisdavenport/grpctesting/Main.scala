package io.chrisdavenport.grpctesting

import cats.syntax.all._
import cats.effect._
import com.comcast.ip4s._
import org.http4s.ember.server.EmberServerBuilder
import Hello.{HelloReply, HelloRequest}
import org.http4s._

object Main extends IOApp {
  val greeter = new Greeter[IO] {
    def sayHello(request: HelloRequest): IO[HelloReply] = 
      IO.println(request) >>
      IO(HelloReply(request.name))
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