package services

import javax.inject._

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.Source
import org.reactivestreams.{Publisher, Subscriber}
import play.api.{Configuration, Logger}
import play.api.inject.ApplicationLifecycle
import twitter4j._

import scala.collection.mutable
import scala.concurrent.Future

/**
  *
  */
@Singleton
class TweetService @Inject()(config: Configuration, appLifecycle: ApplicationLifecycle) {

  Logger.info(s"TweetService: Starting application.")

  private lazy val searchTerms = config.getStringSeq("tweet.tags").getOrElse(Seq(""))
  lazy val searchSource: Source[Status, NotUsed] = searchHashtags(searchTerms)

  private lazy val tweetPublisher = new TweetPublisher
  private implicit val system = ActorSystem("tweet-service")


  appLifecycle.addStopHook { () =>
    tweetPublisher.shutdown()
    Future.successful(())
  }

  private implicit val materializer = ActorMaterializer()
  private val factory = new TwitterStreamFactory()

  private def searchHashtags(searchString: Seq[String]): Source[Status, NotUsed] = {
    val query = new FilterQuery(searchString: _*)
    createTweetSource(query)
  }

  private def createTweetSource(query: FilterQuery): Source[Status, NotUsed] = {
    tweetPublisher.twitter.filter(query)
    Source.fromPublisher(tweetPublisher)
  }

  private trait DuplicateCheckSubscriber[T] {
    protected val subscribers = mutable.Buffer[Subscriber[_ >: T]]()
    def subscribe(subscriber: Subscriber[_ >: T]): Unit =
      if (!subscribers.contains(subscriber)) {
        subscribers.append(subscriber)
      }
  }

  private class TweetPublisher extends Publisher[Status] with DuplicateCheckSubscriber[Status] {

    lazy val twitter = factory.getInstance()

    def shutdown() = {
      twitter.shutdown()
      subscribers.foreach { _.onComplete }
    }

    twitter.addListener(new StatusAdapter() {
      override def onStallWarning(warning: StallWarning): Unit =
        Logger.warn(s"Twitter stall warning: ${warning.getMessage} with code: ${warning.getCode}")

      override def onException(ex: Exception): Unit = {
        Logger.error(s"Twitter error:", ex)
        subscribers.foreach { subscriber =>
          subscriber.onError(ex)
        }
      }

      override def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit =
        Logger.warn(s"Twitter limitation notice, numberOfLimitedStatuses: $numberOfLimitedStatuses")

      override def onStatus(status: Status): Unit = subscribers.foreach { _.onNext(status) }
    })
  }
}
