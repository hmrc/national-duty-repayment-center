package uk.gov.hmrc.nationaldutyrepaymentcenter.controllers

import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.nationaldutyrepaymentcenter.config.AppConfig
import uk.gov.hmrc.nationaldutyrepaymentcenter.models.requests.CreateClaimRequest
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext

class ClaimRepaymentsController @Inject()(
                                           cc: ControllerComponents,
                                           appConfig: AppConfig
                                         )
                                         (implicit ec: ExecutionContext) extends BackendController(cc) {

  def submit(): Action[CreateClaimRequest] = Action(parse.json[CreateClaimRequest]).async {
    implicit request =>
      val channelType = if (request.headers.get("User-Agent").getOrElse("") == appConfig.internalServiceName) "INTERNAL" else "WEB"


      println("CreateClaimRequest: " + request.body)

  }



}
