package example

import java.io.File

import akka.actor._
import akka.io.Tcp
import org.parboiled.common.FileUtils
import spray.http.CacheDirectives.`no-cache`
import spray.http.HttpHeaders.{`Content-Type`, Connection, `Cache-Control`}
import spray.http.MediaTypes._
import spray.http._
import spray.httpx.encoding.Gzip
import spray.routing.{HttpService, RequestContext}
import spray.routing.directives.CachingDirectives
import spray.util._

import scala.concurrent.duration._
import scala.language.postfixOps


// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class DemoServiceActor extends Actor with DemoService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(demoRoute)
}


// this trait defines our service behavior independently from the service actor
trait DemoService extends HttpService {

  // we use the enclosing ActorContext's or ActorSystem's dispatcher for our Futures and Scheduler
  implicit def executionContext = actorRefFactory.dispatcher

  val `text/event-stream` =  MediaType.custom("text/event-stream")
  MediaTypes.register(`text/event-stream`)

  val demoRoute = {
    get {
      path("sse") {
        complete(index)
      } ~
        path("sse" / "ping") {
          complete("PONG!")
        } ~
        path("sse" / "index") {
           complete(index)
        } ~
        path("sse" / "stream1") {
          // we detach in order to move the blocking code inside the simpleStringStream into a future
          detach() {
            respondWithMediaType(`text/html`) { // normally Strings are rendered to text/plain, we simply override here
              complete(simpleStringStream)
            }
          }
        } ~
        path("sse" / "stream2") {
          sendStreamingResponse
        } ~
        path("sse" / "stream-large-file") {
          encodeResponse(Gzip) {
            getFromFile(largeTempFile)
          }
        } ~
        path("sse" / "server-sent-event") {
          getFromResource("www/index.html")
        } ~ path("sse" / "streaming") {
          respondAsEventStream {
            sendSSE
          }
        } ~
        path("sse" / "www") {
           getFromResourceDirectory("www")
        } ~
        path("sse" / "css" / Segment) { path =>
         getFromResource("www/css/" + path)
        } ~
        path("sse" / "js" / Segment) { path =>
          getFromResource("www/js/" + path)
        }
    }
  }
 // lazy val simpleRouteCache = routeCache()

  lazy val index =
    <html>
      <body>
        <h1>Say hello to <i>spray-routing</i> on <i>Jetty</i>!</h1>
        <p>Defined resources:</p>
        <ul>
          <li><a href="ping">ping</a></li>
          <li><a href="stream1">stream1</a> (via a Stream[T])</li>
          <li><a href="stream2">stream2</a> (manually)</li>
          <li><a href="stream-large-file">stream-large-file</a></li>
          <li><a href="timeout">timeout</a></li>
          <li><a href="server-sent-event">server-sent-event</a> (server-sent-event (SSE)) </li>
        </ul>
      </body>
    </html>


  // we prepend 2048 "empty" bytes to push the browser to immediately start displaying the incoming chunks
  lazy val streamStart = " " * 2048 + "<html><body><h2>A streaming response</h2><p>(for 5 seconds)<ul>"
  lazy val streamEnd = "</ul><p>Finished.</p></body></html>"

  def simpleStringStream: Stream[String] = {
    val secondStream = Stream.continually {
      // CAUTION: we block here to delay the stream generation for you to be able to follow it in your browser,
      // this is only done for the purpose of this demo, blocking in actor code should otherwise be avoided
      Thread.sleep(250)
      "<li>" + DateTime.now.toIsoDateTimeString + "</li>"
    }
    streamStart #:: secondStream.take(16) #::: streamEnd #:: Stream.empty
  }

  // simple case class whose instances we use as send confirmation message for streaming chunks
  case class Ok(remaining: Int)

  def sendSSE(ctx: RequestContext): Unit =
    actorRefFactory.actorOf {
      Props {
        new Actor with ActorLogging {
          // we use the successful sending of a chunk as trigger for scheduling the next chunk
          val responseStart = HttpResponse(entity = HttpEntity(`text/event-stream`, "data: start\n\n"))
          log.info(" start chunk response  with 10 iterations")
          ctx.responder ! ChunkedResponseStart(responseStart).withAck(Ok(10))

          def receive = {
            case Ok(0) =>
             log.info(" going to stop it " )
              ctx.responder ! MessageChunk("data: " + 100 + "\n\n")
              ctx.responder ! MessageChunk("data: Finished.\n\n")
              ctx.responder ! ChunkedMessageEnd
              context.stop(self)
            case Ok(remaining) =>
              log.info(" got ok remaining " + remaining)
              in(Duration(500, MILLISECONDS)) {
                val nextChunk = MessageChunk("data: " + (10 - remaining)*10 + "\n\n")
                ctx.responder ! nextChunk.withAck(Ok(remaining - 1))
              }

            case ev: Tcp.ConnectionClosed =>
              log.warning("Stopping response streaming due to {}", ev)
          }
        }
      }
    }


  def sendStreamingResponse(ctx: RequestContext): Unit =
    actorRefFactory.actorOf {
      Props {
        new Actor with ActorLogging {
          // we use the successful sending of a chunk as trigger for scheduling the next chunk
          val responseStart = HttpResponse(entity = HttpEntity(`text/html`, streamStart))
          ctx.responder ! ChunkedResponseStart(responseStart).withAck(Ok(16))

          def receive = {
            case Ok(0) =>
              ctx.responder ! MessageChunk(streamEnd)
              ctx.responder ! ChunkedMessageEnd
              context.stop(self)

            case Ok(remaining) =>
              in(Duration(250, MILLISECONDS)) {
                val nextChunk = MessageChunk("<li>" + DateTime.now.toIsoDateTimeString + "</li>")
                ctx.responder ! nextChunk.withAck(Ok(remaining - 1))
              }

            case ev: Tcp.ConnectionClosed =>
              log.warning("Stopping response streaming due to {}", ev)
          }
        }
      }
    }

  def in[U](duration: FiniteDuration)(body: => U): Unit =
    actorSystem.scheduler.scheduleOnce(duration)(body)

  lazy val largeTempFile: File = {
    val file = File.createTempFile("streamingTest", ".txt")
    FileUtils.writeAllText((1 to 1000).map("This is line " + _) mkString "\n", file)
    file.deleteOnExit()
    file
  }

  def respondAsEventStream =
    respondWithHeader(`Cache-Control`(`no-cache`)) &
      respondWithHeader(`Connection`("Keep-Alive")) &
      respondWithMediaType(`text/event-stream`)

}
