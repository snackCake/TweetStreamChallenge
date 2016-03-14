package services

import javax.inject._

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.Source
import org.reactivestreams.{Subscription, Subscriber, Publisher}
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import twitter4j._

import scala.collection.mutable
import scala.concurrent.Future
import scala.collection.JavaConverters._

/**
  *
  */
@Singleton
class TweetService @Inject()(appLifecycle: ApplicationLifecycle) {

  implicit val system = ActorSystem("tweet-service")
  implicit val materializer = ActorMaterializer()

  private val publishers = mutable.Buffer[TweetPublisher]()
  private val factory = new AsyncTwitterFactory()
  private val cacheMax = 100

  Logger.info(s"TweetService: Starting application.")

  appLifecycle.addStopHook { () =>
    publishers.foreach { _.shutdown() }
    Future.successful(())
  }

  private val combinedPublisher = new Publisher[(String, Status)] with Subscriber[(String, Status)] with DuplicateCheckSubscriber[(String, Status)] {
    private val eachSub = subscribers.foreach _
    override def onError(t: Throwable): Unit = eachSub { _.onError(t) }
    override def onSubscribe(s: Subscription): Unit = eachSub { _.onSubscribe(s) }
    override def onComplete(): Unit = eachSub { _.onComplete }
    override def onNext(t: (String, Status)): Unit = eachSub { _.onNext(t) }
  }

  Seq("#jvm", "#java", "#scala", "#clojure", "#groovy", "#kotlin").foreach { hashtag =>
    searchHashtag(hashtag).runForeach { status =>
      combinedPublisher.onNext(hashtag -> status) }
  }

  def buildCombinedSource: Source[(String, Status), NotUsed] = Source.fromPublisher(combinedPublisher)

  private def searchHashtag(hashtag: String): Source[Status, NotUsed] = {
    val query = new Query(if (hashtag.startsWith("#")) hashtag else s"#$hashtag")
    createTweetSource(query)
  }

  private def createTweetSource(query: Query): Source[Status, NotUsed] = {
    val tweetPublisher = new TweetPublisher(query)
    publishers.append(tweetPublisher)
    tweetPublisher.twitter.search(query)
    Source.fromPublisher(tweetPublisher)
  }

  private trait DuplicateCheckSubscriber[T] {

    protected val subscribers = mutable.Buffer[Subscriber[_ >: T]]()

    def subscribe(s: Subscriber[_ >: T]): Unit = if (!subscribers.contains(s)) subscribers.append(s)
  }

  private class TweetPublisher(val query: Query) extends Publisher[Status] with DuplicateCheckSubscriber[Status] {

    lazy val twitter = factory.getInstance()

    def shutdown() = {
      twitter.shutdown()
      subscribers.foreach { _.onComplete }
    }

    twitter.addListener(new TwitterAdapter() {

      override def searched(queryResult: QueryResult): Unit = {
        queryResult.getTweets.asScala.foreach { tweet =>
          subscribers.foreach { subscriber =>
            subscriber.onNext(tweet)
          }
        }

        if (queryResult.hasNext) {
          // Get the next page.
          val nextQuery = queryResult.nextQuery()
          twitter.search(nextQuery)
        } else {
          // Restart the query
          twitter.search(query)
        }
      }

      override def onException(e: TwitterException, method: TwitterMethod) {
        Logger.error(s"Twitter error: $method", e)
        subscribers.foreach { subscriber =>
          subscriber.onError(e)
        }
      }
    })
  }
}
