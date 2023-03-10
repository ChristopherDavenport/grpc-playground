
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

  val action = (stub: GreeterFs2Grpc[IO, Metadata]) => {
    stub.sayHelloToInfinity(
        fs2.Stream("Chris", "Sarah", "Arman", "Zach")
          .map(HelloRequest(_)),
        new Metadata()
      )
      .compile
      .toList
  }

  // val action = (stub: GreeterFs2Grpc[IO, Metadata]) => {
  //   stub.sayHello(HelloRequest("Chris"), new Metadata())
  // }

  // val action = (stub: GreeterFs2Grpc[IO, Metadata]) => {
  //   stub.sayHelloTiny(
  //       fs2.Stream("Chris", "Sarah", "Arman", "Zach")
  //         .map(HelloRequest(_)),
  //       new Metadata()
  //     )
  // }

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

object ServerTest extends IOApp{

  import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
  import fs2.grpc.syntax.all._

  val greeter = new GreeterFs2Grpc[IO, Metadata] {

    def sayHello(request: HelloRequest, ctx: io.grpc.Metadata): IO[HelloReply] =
      IO.println(request) >>
      IO(HelloReply(request.name))

    def sayHelloAlot(request: Hello.HelloRequest, ctx: io.grpc.Metadata): fs2.Stream[cats.effect.IO,Hello.HelloReply] =
      fs2.Stream.eval(IO.println(request)).drain ++
      fs2.Stream(Hello.HelloReply(request.name))
        .covary[IO]
        .repeat
        .take(5)

    def sayHelloTiny(request: fs2.Stream[cats.effect.IO,Hello.HelloRequest], ctx: io.grpc.Metadata): cats.effect.IO[Hello.HelloReply] =
      request.take(1).compile.to(List).map(_.headOption.fold(Hello.HelloReply("Unknown"))(req => Hello.HelloReply(req.name)))

    def sayHelloToInfinity(request: fs2.Stream[cats.effect.IO,Hello.HelloRequest], ctx: io.grpc.Metadata): fs2.Stream[cats.effect.IO,Hello.HelloReply] =
      request.map(r => Hello.HelloReply(r.name))
  }

  val greeterService: Resource[IO, ServerServiceDefinition] =
      GreeterFs2Grpc.bindServiceResource[IO](greeter)


  def runService(service: ServerServiceDefinition) = NettyServerBuilder
    .forPort(8080)
    .addService(service)
    .resource[IO]
    .evalMap(server => IO(server.start()))
    .useForever


  def run(args: List[String]): IO[ExitCode] =
    greeterService.use(runService)


}