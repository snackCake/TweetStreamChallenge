package controllers

import javax.inject.Inject

import akka.stream.Materializer
import play.api.http.ContentTypes
import play.api.libs.Comet
import play.api.libs.json._
import play.api.mvc._
import services.TweetService

/**
  *
  * @author Josh Klun (jklun@nerdery.com)
  */
class TweetCometController  @Inject()(materializer: Materializer, tweetService: TweetService) extends Controller {

  def tweetComet = Action {
    implicit val mat = materializer
    def jsonSource = tweetService.searchSource.map { status => JsString(status.getText) }
    val content = jsonSource via Comet.json("parent.cometMessage")
    Ok.chunked(content).as(ContentTypes.HTML)
  }
}
