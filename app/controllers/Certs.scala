package controllers

import java.util.UUID

import com.mle.cert.{CertMaker, CertRequestInfo}
import com.mle.util.Log
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import views.html


/**
 *
 * @author mle
 */
object Certs extends Controller with Log {
  val COUNTRY_CODE = "country_code"
  val STATE = "state"
  val LOCALITY = "locality"
  val ORG = "org"
  val CN = "cn"
  val STORE_PASS = "store_pass"

  val form = Form(mapping(
    COUNTRY_CODE -> nonEmptyText,
    STATE -> nonEmptyText,
    LOCALITY -> nonEmptyText,
    ORG -> nonEmptyText,
    CN -> nonEmptyText,
    STORE_PASS -> nonEmptyText(minLength = 6)
  )(CertRequestInfo.apply)(CertRequestInfo.unapply))

  def certs = Action(Ok(html.certificates(form)))

  def submit = Action(implicit req => {
    form.bindFromRequest().fold(
      errors => {
        log info s"Form failure: $errors"
        BadRequest(html.certificates(errors))
      },
      data => {
        //        log info s"Making: $data"
        val uuid = UUID.randomUUID()
        Runner.tasks += uuid -> data
        Ok(views.html.run(Some(uuid.toString), CertMaker.meta))
      }
    )
  })
}