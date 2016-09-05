package org.splink.raven

import akka.stream.Materializer
import org.splink.raven.Exceptions.PageletException
import org.splink.raven.Resources.Fingerprint
import play.api.http.Writeable
import play.api.mvc._
import play.api.{Environment, Logger}
import scala.concurrent.duration._

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

case class Head(title: String,
                metaTags: Set[MetaTag] = Set.empty,
                js: Option[Fingerprint] = None,
                css: Option[Fingerprint] = None)

case class Page(language: String,
                head: Head,
                body: String,
                js: Option[Fingerprint] = None)

case class ErrorPage(language: String,
                     title: String,
                     exception: PageletException)


trait BricksController {
  implicit def resultOps(result: Result): ResultTools#ResultOps

  implicit def treeOps(tree: Tree): TreeTools#TreeOps

  def visualize(p: Part): String

  def ResourceAction(fingerprint: String, validFor: Duration = 365.days): Action[AnyContent]

  def PageAction[T: Writeable](errorTemplate: ErrorPage => T)(
    title: String, plan: RequestHeader => Part, args: Arg*)(template: (Request[_], Page) => T)(
                                implicit ec: ExecutionContext, m: Materializer, env: Environment): Action[AnyContent]

  def PagePartAction[T: Writeable](errorTemplate: ErrorPage => T)(
    plan: RequestHeader => Tree, id: Symbol)(template: (Request[_], Page) => T)(
                                    implicit ec: ExecutionContext, m: Materializer, env: Environment): Action[AnyContent]
}

trait BricksControllerImpl extends BricksController with Controller {
  self: Mason with TreeTools with ResultTools with ResourceActions with Visualizer =>
  val log = Logger(getClass).logger

  override implicit def resultOps(result: Result): ResultOps = resultOps(result)

  override implicit def treeOps(tree: Tree): TreeOps = treeOps(tree)

  override def visualize(p: Part) = visualizer.visualize(p)

  override def ResourceAction(fingerprint: String, validFor: Duration = 365.days) =
    resourceService.ResourceAction(fingerprint, validFor)

  override def PageAction[T: Writeable](errorTemplate: ErrorPage => T)(
    title: String, plan: RequestHeader => Part, args: Arg*)(template: (Request[_], Page) => T)(
                   implicit ec: ExecutionContext, m: Materializer, env: Environment) = Action.async { implicit request =>
    mason.build(plan(request), args: _*).map { result =>
      Ok(template(request, mkPage(title, result))).withCookies(result.cookies: _*)
    }.recover {
      case e: PageletException =>
        log.error(s"error $e")
        InternalServerError(errorTemplate(ErrorPage(request2lang.language, title, e)))
    }
  }

  override def PagePartAction[T: Writeable](errorTemplate: ErrorPage => T)(
    plan: RequestHeader => Tree, id: Symbol)(template: (Request[_], Page) => T)(
               implicit ec: ExecutionContext, m: Materializer, env: Environment) = Action.async { request =>
    plan(request).find(id).map { part =>
      val args = request.queryString.map { case (key, values) =>
        Arg(key, values.head)
      }.toSeq

      PageAction(errorTemplate)(id.name, _ => part, args: _*)(template).apply(request)
    }.getOrElse {
      Future.successful(BadRequest(s"'$id' does not exist"))
    }
  }

  def mkPage(title: String, result: BrickResult)(implicit r: RequestHeader, env: Environment) = {
    val jsFinger = Resources.update(result.js)
    val jsTopFinger = Resources.update(result.jsTop)
    val cssFinger = Resources.update(result.css)

    Page(request2lang.language,
      Head(title, result.metaTags, jsTopFinger, cssFinger),
      result.body,
      jsFinger)
  }
}
