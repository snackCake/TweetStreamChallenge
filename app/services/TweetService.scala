package services

import javax.inject._

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl.Source
import org.reactivestreams.{Publisher, Subscriber, Subscription}
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import twitter4j._

import scala.collection.mutable
import scala.concurrent.Future

/**
  *
  */
@Singleton
class TweetService @Inject()(appLifecycle: ApplicationLifecycle,
                             materializer: Materializer) {

  Logger.info(s"TweetService: Starting...")

  private implicit val mat = materializer
  private val factory = new TwitterStreamFactory()

  def createSearchSource(searchString: Seq[String]): Source[Status, NotUsed] = {
    val query = new FilterQuery(searchString: _*)
    createTweetSource(query)
  }

  private def createTweetSource(query: FilterQuery): Source[Status, NotUsed] = {
    val tweetPublisher = new TweetPublisher(query)

    appLifecycle.addStopHook { () =>
      tweetPublisher.shutdown()
      Future.successful(())
    }

    Source.fromPublisher(tweetPublisher)
  }

  private trait DuplicateCheckSubscriber[T] {
    this: Publisher[T] =>
      protected val subscribers = mutable.Set[Subscriber[_ >: T]]()
      override def subscribe(subscriber: Subscriber[_ >: T]): Unit = {
        if (!subscribers.contains(subscriber)) {
          subscriber.onSubscribe(new Subscription {
            override def cancel(): Unit = {
              subscribers.remove(subscriber)
            }
            override def request(n: Long): Unit = {
              //We're not doing anything special here. But the subscriber wants to know who it's
              //subscribed to if it's doing batching, so we'll make it think we care.
              //We don't actually care about its demands.
            }
          })
          subscribers.add(subscriber)
        }
      }
  }

  private class TweetPublisher(query: FilterQuery) extends Publisher[Status] with DuplicateCheckSubscriber[Status] {

    val twitter = {
      val t = factory.getInstance()
      t.addListener(new StatusAdapter() {
        override def onDeletionNotice(notice: StatusDeletionNotice) = Logger.warn(s"Status deletion notice: $notice")

        override def onStallWarning(warning: StallWarning): Unit =
          Logger.warn(s"Twitter stall warning: ${warning.getMessage} with code: ${warning.getCode}")

        override def onException(ex: Exception): Unit = {
          Logger.error(s"Twitter stream error:", ex)
          subscribers.foreach { subscriber =>
            subscriber.onError(ex)
          }
        }

        override def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit =
          Logger.warn(s"Twitter limitation notice, numberOfLimitedStatuses: $numberOfLimitedStatuses")

        override def onStatus(status: Status): Unit = {
          subscribers.foreach { subscriber =>
            subscriber.onNext(status)
          }
        }
      })
      t.filter(query)
      t
    }

    def shutdown() = {
      twitter.shutdown()
      subscribers.foreach { _.onComplete }
    }
  }
}
