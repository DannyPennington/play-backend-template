package models

import play.api.libs.json.{Json, OFormat}
import reactivemongo.bson.BSONObjectID

object User{
  implicit val userFormat:OFormat[User] = Json.format[User]

  def apply(username: String,
            email: String,
            password: String) = new User( username, email, password)

}

case class User(
              username: String,
              email: String,
              password: String
               )
