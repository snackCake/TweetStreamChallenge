package controllers

import javax.inject.Inject

import akka.stream.Materializer
import play.api.Configuration
import play.api.http.ContentTypes
import play.api.libs.Comet
import play.api.libs.json._
import play.api.mvc._
import repositories.TweetRepository
import services.{PersistenceService, TweetService}

/**
  * @author Josh Klun (jklun@nerdery.com)
  */
class TweetCometController @Inject()(materializer: Materializer,
                                     config: Configuration,
                                     tweetRepository: TweetRepository,
                                     tweetService: TweetService,
                                     persistenceService: PersistenceService) extends Controller {

  private lazy val searchTerms = config.getStringSeq("tweet.tags").getOrElse(Seq(""))

  def tweetComet = Action {
    implicit val mat = materializer
    def content = tweetService
      .createSearchSource(searchTerms)
      .map { s => Option(s.getText).getOrElse("") }
      .via(persistenceService.stringPersister(tweetRepository.insert))
      .map { s => JsString(s) }
      .via(Comet.json("parent.cometMessage"))
    Ok.chunked(content).as(ContentTypes.HTML)
  }
}
