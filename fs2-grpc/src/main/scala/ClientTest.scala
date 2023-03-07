
import cats.syntax.all._
import cats.effect._
import Hello._
import io.grpc._
import cats.effect.std._

object ClientTest extends IOApp{

  import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import fs2.grpc.syntax.all._

val managedChannelResource  =
  NettyChannelBuilder
    .forAddress("127.0.0.1", 8080)
    .usePlaintext()
    .resource[IO]

  // val action = (stub: GreeterFs2Grpc[IO, Metadata]) => {
  //   stub.sayHelloToInfinity(
  //       fs2.Stream("Chris", "Sarah", "Arman", "Zach")
  //         .map(HelloRequest(_)),
  //       new Metadata()
  //     )
  //     .compile
  //     .toList
  // }

  // val action = (stub: GreeterFs2Grpc[IO, Metadata]) => {
  //   stub.sayHello(HelloRequest("Chris"), new Metadata())
  // }

  val action = (stub: GreeterFs2Grpc[IO, Metadata]) => {
    stub.sayHelloTiny(
        fs2.Stream("Chris", "Sarah", "Arman", "Zach")
          .map(HelloRequest(_)),
        new Metadata()
      )
  }

  // val action = (stub: GreeterFs2Grpc[IO, Metadata]) => {
  //   stub.sayHelloAlot(HelloRequest("Chris"), new Metadata())
  //     .compile
  //     .toList
  // }

  def runProgram(stub: GreeterFs2Grpc[IO, Metadata]): IO[Unit] =
    action(stub).flatMap(a =>
      IO.println("") >>
      IO.println(a) >>
      IO.println("")
    )

val runIt: IO[Unit] = managedChannelResource
  .flatMap{ ch => GreeterFs2Grpc.stubResource(ch)}
  .use(runProgram)

  def run(args: List[String]): IO[ExitCode] = {
    runIt.as(ExitCode.Success)
  }
}