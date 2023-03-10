package io.chrisdavenport.grpcplayground

import cats.effect._
import cats.syntax.all._
import org.http4s.Headers
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.implicits._
import io.opentelemetry.proto.collector.trace.v1.trace_service.{
  TraceService
}
import io.opentelemetry.proto.collector.trace.v1.trace_service.ExportTraceServiceRequest
import io.opentelemetry.proto.trace.v1.trace.ResourceSpans
import io.opentelemetry.proto.trace.v1.trace.ScopeSpans
import io.opentelemetry.proto.trace.v1.trace.Span
import io.opentelemetry.proto.resource.v1.resource.Resource
import com.google.protobuf.ByteString
import scodec.bits.ByteVector
import io.opentelemetry.proto.common.v1.common.InstrumentationScope
import com.comcast.ip4s.Port

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = EmberClientBuilder.default[IO].withHttp2.build.use{
    iclient =>
    val client = org.http4s.client.middleware.Logger(true, true, logAction = {(s: String) => IO.println(s)}.some)(iclient)
    
    // val system = TraceService.fromClient(client, uri"http://localhost:4317")

    _root_.org.http4s.grpc.ClientGrpc.unaryToUnary(_root_.org.http4s.grpc.codecs.ScalaPb.codecForGenerated(_root_.io.opentelemetry.proto.collector.trace.v1.trace_service.ExportTraceServiceRequest), _root_.org.http4s.grpc.codecs.ScalaPb.codecForGenerated(_root_.io.opentelemetry.proto.collector.trace.v1.trace_service.ExportTraceServiceResponse), "opentelemetry.proto.collector.trace.v1.TraceService", "Export")(client, uri"http://localhost:4317")(
    // system.export(
      ExportTraceServiceRequest(
        Seq(
          ResourceSpans(
            None, 
            Seq(
              ScopeSpans(
                InstrumentationScope("grpc-playground").some,
                spans = Seq(
                  Span(
                    traceId = {
                      ByteString.copyFrom(ByteVector.fromHex("0x5b8aa5a2d2c872e8321cf37308d69df2").get.toArrayUnsafe)
                    },
                    spanId = {
                      ByteString.copyFrom(ByteVector.fromHex("0x5fb397be34d26b51").get.toArrayUnsafe)
                    },
                    startTimeUnixNano = System.nanoTime(),
                    endTimeUnixNano = System.nanoTime(),
                    name = "Hello-Greetings"
                  )
                )
              )
            )
          )
        )
      ),
      Headers.empty
    ).flatTap(IO.println) >>
    IO.unit.as(ExitCode.Success)
  }
}