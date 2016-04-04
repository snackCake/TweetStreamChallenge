package services

import javax.inject.{Inject, Singleton}

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import scala.util.{Failure, Success}

/** A service that provides methods for creating stream flows that persist [[String]] data.
  *
  * @author Ryan Evans (rs3vans@gmail.com)
  */
@Singleton
class PersistenceService @Inject()(materializer: Materializer) {

  Logger.info(s"PersistenceService: Starting...")

  private implicit val mat = materializer

  /** Creates a stream [[Flow]] which takes in a [[String]] and outputs that same [[String]] while also branching to
    * persist said [[String]] using the given callback.
    */
  def stringPersister(pf: String => Future[Unit]): Flow[String, String, NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val persistenceSink = Sink.foreach[String] { content =>
        val f = pf(content)
        f.onComplete {
          case Success(u) => Logger.debug(s"Persisted content: '$content'")
          case Failure(t) => Logger.error(s"Failed to persist content: '$content", t)
        }
      }

      val bcast = builder.add(Broadcast[String](2))
      bcast.out(1) ~> persistenceSink

      FlowShape(bcast.in, bcast.out(0))
    })
}
