
package Services

import akka.http.scaladsl.model.ErrorInfo
import play.api.mvc.Results.Ok
import javax.inject.Inject
import models._
import org.mindrot.jbcrypt.BCrypt
import play.api.Logging
import play.api.libs.json.Json
import play.modules.reactivemongo.{ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.Cursor
import reactivemongo.api.commands.{WriteConcernError, WriteError, WriteResult}
import reactivemongo.play.json._
import reactivemongo.play.json.collection.{JSONCollection, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Failure

class MongoService @Inject()(
                              val reactiveMongoApi: ReactiveMongoApi
                            ) extends ReactiveMongoComponents with Logging {

  def userCollection: Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection]("users"))

  def userSearchHelper(category: String = "email", search: String): Future[List[User]] = {
    userCollection.map {
        _.find(Json.obj(category -> search))
        .sort(Json.obj(category -> -1))
        .cursor[User]()
    }.flatMap(
      _.collect[List](
        1,
        Cursor.FailOnError[List[User]]()
      )
    )
  }

  def reInnitUsers: Future[WriteResult] = {
    userCollection.map {
      _.drop
    }
    val result = addUser(User("admin", "admin@admin.com", BCrypt.hashpw("admin",BCrypt.gensalt())))
    logger.info("[MongoService][reInnitUsers] Database reinitialized with admin user")
    result
  }

  def addUser(user: User): Future[WriteResult]= {
    userEmailExists(user.email).flatMap {
      case false => userUsernameExists(user.username).flatMap {
        case false => userCollection.flatMap(_.insert.one(user))
        case true => userCollection.flatMap(_.insert.one(user))
      }
      case true => userCollection.flatMap(_.insert.one(User("","","")))
      }
    }

  def findUserEmail(email: String): Future[Option[User]] = {
    userSearchHelper("email",email)
      .map(_.headOption)
  }

  def findUserUsername(username: String):Future[Option[User]] ={
    userSearchHelper("username", username)
      .map(_.headOption)
  }

  def userEmailExists(email: String): Future[Boolean] = {
    findUserEmail(email).map {
      _.getOrElse(None)
      match {
        case None => false
        case _ => true
      }
    }
  }
  def userUsernameExists(username: String): Future[Boolean] = {
    findUserUsername(username).map {
      _.getOrElse(None)
      match {
        case None => false
        case _ => true
      }
    }
  }

  def isEmail(e: String): Boolean = {
    val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r
    e match {
      case e if e.trim.isEmpty                            => false
      case e if emailRegex.findFirstMatchIn(e).isDefined  => true
      case _                                              => false
    }}
}