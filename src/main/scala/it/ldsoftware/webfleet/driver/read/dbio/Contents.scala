package it.ldsoftware.webfleet.driver.read.dbio
import java.time.ZonedDateTime

import it.ldsoftware.webfleet.driver.actors.model._
import it.ldsoftware.webfleet.driver.database.ExtendedProfile.api._
import it.ldsoftware.webfleet.driver.read.model.ContentRM
import slick.ast.BaseTypedType
import slick.lifted.ProvenShape

class Contents(tag: Tag) extends Table[ContentRM](tag, "contents") {

  implicit def webTypeMapper: BaseTypedType[WebType] =
    MappedColumnType.base[WebType, String](
      tmap = {
        case Page     => "Page"
        case Folder   => "Folder"
        case Calendar => "Calendar"
      },
      tcomap = {
        case "Page"     => Page
        case "Folder"   => Folder
        case "Calendar" => Calendar
        case s          => throw new IllegalArgumentException(s"Unknown web type $s")
      }
    )

  def path: Rep[String] = column[String]("path", O.PrimaryKey)
  def title: Rep[String] = column[String]("title")
  def description: Rep[String] = column[String]("description")
  def webType: Rep[WebType] = column[WebType]("web_type")
  def createdAt: Rep[ZonedDateTime] = column[ZonedDateTime]("created_at")
  def parent: Rep[String] = column[String]("parent")

  def * : ProvenShape[ContentRM] =
    (path, title, description, webType, createdAt, parent.?) <> (ContentRM.tupled, ContentRM.unapply)
}
