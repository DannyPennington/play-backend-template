package controllers

import Services.MongoService
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsError, JsValue}
import play.api.Logging
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request}
import models.User
import reactivemongo.api.commands.WriteResult

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class ResponseController @Inject()(val mongo: MongoService,
                                   val cc: ControllerComponents,
                                   implicit val ec: ExecutionContext) extends AbstractController(cc) with play.api.i18n.I18nSupport with Logging {


  def addUser():Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    jsonToUser(request).getOrElse("") match {
      case user: User => mongo.addUser(user).map {
        case result: WriteResult if result.ok =>
          logger.logger.info("[ResponseController][addUser] New user successfully created")
          Created("")
        case res: WriteResult =>
          logger.logger.error("[ResponseController][addUser] Could not create user using mongoService")
          InternalServerError("")
        case _ => InternalServerError("")
      }
      case _ =>
        logger.logger.error("[ResponseController][addUser] Could not parse Json correctly")
        Future.successful(BadRequest(""))
    }
  }


  def jsonToUser(request: Request[AnyContent]): Option[User] = {
    request.body.asJson match {
      case Some(json) =>
        Try(parseJson(json)) match {
          case Success(user) => user
          case Failure(_) => None
        }
      case None =>
        None
    }
  }

  def parseJson(json: JsValue): Option[User] = {
    val username = json.\("username").as[String]
    val email = json.\("email").as[String]
    val password = json.\("password").as[String]
    Some(User(username, email, password))
  }


}
