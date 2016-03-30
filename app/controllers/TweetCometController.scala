package controllers

import javax.inject.Inject

import akka.stream.Materializer
import play.api.Configuration
import play.api.http.ContentTypes
import play.api.libs.Comet
import play.api.libs.json._
import play.api.mvc._
import services.TweetService

/**
  * @author Josh Klun (jklun@nerdery.com)
  */
class TweetCometController @Inject()(materializer: Materializer,
                                     config: Configuration,
                                     tweetService: TweetService) extends Controller {

  private lazy val searchTerms = config.getStringSeq("tweet.tags").getOrElse(Seq(""))

  def tweetComet = Action {
    implicit val mat = materializer
    def jsonSource = tweetService.createSearchSource(searchTerms).map { status =>
      JsString(Option(status.getText).getOrElse(""))
    }
    val content = jsonSource via Comet.json("parent.cometMessage")
    Ok.chunked(content).as(ContentTypes.HTML)
  }
}
