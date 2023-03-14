package io.chrisdavenport.grpcplayground

import cats.effect._
import cats.syntax.all._
import org.http4s.Headers
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits._
import com.google.protobuf.ByteString
import scodec.bits.ByteVector
import com.comcast.ip4s.Port
import java.time.Instant

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = EmberClientBuilder.default[IO].withHttp2.build.use{
    iclient =>
    val client = org.http4s.client.middleware.Logger(true, true, logAction = {(s: String) => IO.println(s)}.some)(iclient)

    IO.unit.flatTap(IO.println) >>
    IO.unit.as(ExitCode.Success)
  }
}