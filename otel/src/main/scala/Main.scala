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
import com.google.protobuf.ByteString
import scodec.bits.ByteVector

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = EmberClientBuilder.default[IO].withHttp2.build.use{
    client =>
    val system = TraceService.fromClient(client, uri"http://localhost:5778")

    system.export(
      ExportTraceServiceRequest(
        Seq(
          ResourceSpans(
            None, 
            Seq(
              ScopeSpans(
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
    ) >>
    IO.unit.as(ExitCode.Success)
  }
}