
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
    
  def runProgram(stub: GreeterFs2Grpc[IO, Metadata]): IO[Unit] = stub.sayHello(HelloRequest("Sarah"), new Metadata()).flatMap(IO.println(_))

val runIt: IO[Unit] = managedChannelResource
  .flatMap{ ch => GreeterFs2Grpc.stubResource(ch)}
  .use(runProgram)

  def run(args: List[String]): IO[ExitCode] = {
    runIt.as(ExitCode.Success)
  }
}