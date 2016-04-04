package model

import org.joda.time.DateTime

case class Tweet (id: Option[Int],
                  content: String,
                  dateTime: DateTime)

object Tweet {
  def create(content: String): Tweet = Tweet(None, content, DateTime.now)
}
