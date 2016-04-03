package repositories

import javax.inject.Inject

import com.github.tototoshi.slick.MySQLJodaSupport._
import model.Tweet
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.concurrent.Future

/**
  * TODO
  * <p>
  * Created by rs3vans on 4/3/16.
  */
class TweetRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  import driver.api._

  private val Tweets = TableQuery[TweetsTable]

  def insert(tweet: Tweet): Future[Unit] = db.run(Tweets += tweet).map { _ => () }

  def insert(tweetContent: String): Future[Unit] = insert(Tweet.create(tweetContent))

  private class TweetsTable(tag: Tag) extends Table[Tweet](tag, "tweet") {

    def id = column[Int]("tweet_id", O.PrimaryKey, O.AutoInc)

    def content = column[String]("content")

    def dateTime = column[DateTime]("date_time")

    def * = (id.?, content, dateTime) <> ((Tweet.apply _).tupled, Tweet.unapply)
  }

}
