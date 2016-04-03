package repositories

import javax.inject.{Inject, Singleton}

import com.github.tototoshi.slick.MySQLJodaSupport._
import model.Tweet
import org.joda.time.DateTime
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

/** A repository for [[Tweet]]s.
  *
  * @author Ryan Evans (rs3vans@gmail.com)
  */
@Singleton
class TweetRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  Logger.info(s"TweetRepository: Starting...")

  import driver.api._

  private val Tweets = TableQuery[TweetsTable]

  /** Insert a new tweet from a [[Tweet]]. */
  def insert(tweet: Tweet): Future[Unit] = db.run(Tweets += tweet).map { _ => () }

  /** Insert a new tweet from a [[String]]. */
  def insert(tweetContent: String): Future[Unit] = insert(Tweet.create(tweetContent))

  /** A Slack table definition for the "tweet" table. */
  private class TweetsTable(tag: Tag) extends Table[Tweet](tag, "tweet") {

    def id = column[Int]("tweet_id", O.PrimaryKey, O.AutoInc)

    def content = column[String]("content")

    def dateTime = column[DateTime]("date_time")

    def * = (id.?, content, dateTime) <> ((Tweet.apply _).tupled, Tweet.unapply)
  }

}
