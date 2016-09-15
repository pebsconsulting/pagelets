package org.splink.raven

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import play.api.http.HttpEntity
import play.api.mvc.{Action, Controller, ResponseHeader, Result}
import play.api.test.FakeRequest

class ResultToolsTest extends FlatSpec with Matchers with ScalaFutures {

  val tools = new ResultToolsImpl with SerializerImpl {}

  import tools._

  val result = Result(ResponseHeader(200), HttpEntity.NoEntity)

  "ResultTools.withJavaScript" should "add one Javascript to the result headers" in {
    val newResult = result.withJavascript(Javascript("some.js"))

    newResult.header.headers should contain(Javascript.name -> "some.js")
  }

  it should "add multiple Javascript to the result headers" in {
    val newResult = result.withJavascript(Javascript("some.js"), Javascript("some-more.js"))

    newResult.header.headers should contain(Javascript.name -> "some.js,some-more.js")
  }

  "ResultTools.withJavaScriptTop" should "add one Javascript to the result headers" in {
    val newResult = result.withJavascriptTop(Javascript("some.js"))

    newResult.header.headers should contain(Javascript.nameTop -> "some.js")
  }

  it should "add multiple Javascript to the result headers" in {
    val newResult = result.withJavascriptTop(Javascript("some.js"), Javascript("some-more.js"))

    newResult.header.headers should contain(Javascript.nameTop -> "some.js,some-more.js")
  }

  "ResultTools.withCss" should "add one Css to the result headers" in {
    val newResult = result.withCss(Css("some.css"))

    newResult.header.headers should contain(Css.name -> "some.css")
  }

  it should "add multiple Css to the result headers" in {
    val newResult = result.withCss(Css("some.css"), Css("some-more.css"))

    newResult.header.headers should contain(Css.name -> "some.css,some-more.css")
  }

  "ResultTools.withMetaTags" should "add one MetaTag to the result headers" in {
    val newResult = result.withMetaTags(MetaTag("some", "tag"))

    newResult.header.headers.get(MetaTag.name).get should startWith("rO0ABXNyABhvcmcuc3BsaW5r")
  }

  it should "add multiple MetaTag to the result headers" in {
    val newResult = result.withMetaTags(MetaTag("any", "tag"), MetaTag("another", "tag"))

    newResult.header.headers.get(MetaTag.name).get should contain(',')
  }

  "ResultToolsImpl provides an implicit Writable which" should "permit an Action to return BrickResult as Result" in {
    class TestController extends Controller {
      def index = Action {
        Ok(BrickResult("Body"))
      }
    }

    val result = new TestController().index.apply(FakeRequest()).futureValue
    result.header.status should equal(200)
  }

}