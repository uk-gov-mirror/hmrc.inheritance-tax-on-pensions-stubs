/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.pensionschemereturnstub.controllers

import play.api.http.Status
import play.api.libs.json.{JsPath, JsString, JsValue}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.inheritancetaxonpensionsstubs.controllers.IhtpReportOverviewController
import uk.gov.hmrc.inheritancetaxonpensionsstubs.utils.APIResponses
import uk.gov.hmrc.pensionschemereturnstub.base.SpecBase

class IhtpReportOverviewControllerSpec extends SpecBase with APIResponses {

  private val controller = app.injector.instanceOf[IhtpReportOverviewController]
  private val correlationId = "d59434ad-6e01-4467-9209-66858e778736"

  private def overviewRequest(queryString: String) = FakeRequest("GET", s"/$queryString").withHeaders(
    "correlationid" -> correlationId,
    "X-Message-Type" -> "Request",
    "X-Originating-System" -> "MDTP",
    "X-Receipt-Date" -> "2026-04-10T16:12:49Z",
    "X-Regime-Type" -> "IHTP",
    "X-Transmitting-System" -> "MDTP"
  )

  "GET ihtp overview" must {

    "return 200-Ok for a known pstr with overview items" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000001IN&dateFrom=2026-01-01&dateTo=2026-12-31")
      )

      status(result) mustBe Status.OK
      header("correlationid", result).value mustBe correlationId

      val content = contentAsJson(result)
      (JsPath \ "success" \ "pstr")(content) mustBe empty
      (JsPath \ "success" \ "ihtpOverview" \ 0 \ "fbNumber")(content) mustBe List(JsString("119000004320"))
      (JsPath \ "success" \ "ihtpOverview" \ 0 \ "firstForename")(content) mustBe List(JsString("Firstname"))
      (JsPath \ "success" \ "ihtpOverview" \ 0 \ "surname")(content) mustBe List(JsString("Surname"))
    }

    "return 200-Ok for a second known pstr" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000002IN&dateFrom=2026-01-01&dateTo=2026-12-31")
      )

      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      (JsPath \ "success" \ "pstr")(content) mustBe empty
      (JsPath \ "success" \ "ihtpOverview" \ 0 \ "paymentReference")(content) mustBe List(
        JsString("A654321/25A392617")
      )
    }

    "return related report versions for an amendment scenario" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000001IN&dateFrom=2026-01-01&dateTo=2026-12-31")
      )

      status(result) mustBe Status.OK
      val amendmentReports = (contentAsJson(result) \ "success" \ "ihtpOverview")
        .as[Seq[JsValue]]
        .filter(report => (report \ "inheritanceTaxReference").asOpt[String].contains("A556789/26A"))

      amendmentReports.size mustBe 2
      amendmentReports.map(report => (report \ "ihtVersion").as[String]) mustBe Seq("001", "002")
      amendmentReports.map(report => (report \ "fbNumber").as[String]).distinct.size mustBe 2
      amendmentReports.map(report => (report \ "paymentReference").as[String]).distinct mustBe Seq("A556789/26A758204")
    }

    "return two amendment scenarios with consistent payment statuses for each IHTP-574 scheme" in {
      Seq(
        "24000036IN" -> Map(
          "A240036/26A836241" -> "Paid",
          "F360024/26B472915" -> "Not reconciled"
        ),
        "00000042IN" -> Map(
          "A000042/26C604218" -> "Paid",
          "F420000/26D195307" -> "Not reconciled"
        )
      ).foreach { case (pstr, expectedStatuses) =>
        val result = controller.getIhtpOverview()(
          overviewRequest(s"?pstr=$pstr&dateFrom=2026-01-01&dateTo=2026-12-31")
        )

        status(result) mustBe Status.OK
        val reports = (contentAsJson(result) \ "success" \ "ihtpOverview").as[Seq[JsValue]]
        val amendmentScenarios = reports
          .groupBy(report => (report \ "paymentReference").as[String])
          .values
          .filter(_.size == 2)
          .toSeq

        amendmentScenarios.size mustBe 2
        amendmentScenarios.foreach { versions =>
          versions.map(report => (report \ "ihtVersion").as[String]).sorted mustBe Seq("001", "002")
          versions.map(report => (report \ "fbNumber").as[String]).distinct.size mustBe 2
          versions.map(report => (report \ "inheritanceTaxReference").as[String]).distinct.size mustBe 1

          val paymentReference = (versions.head \ "paymentReference").as[String]
          versions.map(report => (report \ "ihtpStatus").as[String]).distinct mustBe
            Seq(expectedStatuses(paymentReference))
          versions.foreach(report => (report \ "nino").toOption mustBe None)
        }
      }
    }

    "return payment references in IHT reference plus six digit UPR format" in {
      Seq("00000042IN", "24000001IN", "24000002IN", "24000036IN").foreach { pstr =>
        val result = controller.getIhtpOverview()(
          overviewRequest(s"?pstr=$pstr&dateFrom=2026-01-01&dateTo=2026-12-31")
        )

        status(result) mustBe Status.OK
        val reports = (contentAsJson(result) \ "success" \ "ihtpOverview").as[Seq[JsValue]]

        reports.foreach { report =>
          (report \ "paymentReference").asOpt[String].foreach { paymentReference =>
            val inheritanceTaxReference = (report \ "inheritanceTaxReference").as[String]

            paymentReference must startWith(s"$inheritanceTaxReference")
            (paymentReference must fullyMatch).regex("""[AF]\d{6}/\d{2}[A-Z]\d{6}""")
          }
        }
      }
    }

    "return two additional paid version 001 reports" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000001IN&dateFrom=2026-01-01&dateTo=2026-12-31")
      )

      status(result) mustBe Status.OK
      val paidReports = (contentAsJson(result) \ "success" \ "ihtpOverview")
        .as[Seq[JsValue]]
        .filter(report =>
          (report \ "ihtpStatus").as[String] == "Paid" &&
            (report \ "ihtVersion").as[String] == "001"
        )

      paidReports
        .map(report => (report \ "fbNumber").as[String]) mustBe Seq("119000004360", "119000004362", "119000004363")
      paidReports.map(report => (report \ "paymentReference").as[String]) mustBe
        Seq("A556789/26A758204", "F246810/26B314159", "A975310/26C271828")
    }

    "return 200-Ok with only matching items when status is supplied" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000001IN&dateFrom=2026-01-01&dateTo=2026-12-31&status=Not reconciled")
      )

      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      val statuses =
        (content \ "success" \ "ihtpOverview").as[Seq[JsValue]].flatMap(item => (item \ "ihtpStatus").asOpt[String])

      statuses.size mustBe 33
      statuses.distinct mustBe Seq("Not reconciled")
      (JsPath \ "success" \ "ihtpOverview" \ 0 \ "fbNumber")(content) mustBe List(JsString("119000004320"))
    }

    "return 200 and empty list when no overview items match the supplied date range" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000002IN&dateFrom=2027-01-01&dateTo=2027-12-31")
      )

      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      val reports = (content \ "success" \ "ihtpOverview").as[Seq[JsValue]]
      reports.size mustBe 0
    }

    "return 200 and empty list when status is NO_RECORDS" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000001IN&dateFrom=2026-01-01&dateTo=2026-12-31&status=NO_RECORDS")
      )

      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      val reports = (content \ "success" \ "ihtpOverview").as[Seq[JsValue]]
      reports.size mustBe 0
    }

    "return 400-BadRequest when status is BAD_REQUEST" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000001IN&dateFrom=2026-01-01&dateTo=2026-12-31&status=BAD_REQUEST")
      )

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }

    "return 500-InternalServerError when status is SERVER_ERROR" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000001IN&dateFrom=2026-01-01&dateTo=2026-12-31&status=SERVER_ERROR")
      )

      status(result) mustBe Status.INTERNAL_SERVER_ERROR
      (JsPath \ "failures" \ 0 \ "code")(contentAsJson(result)) mustBe List(JsString("INTERNAL_SERVER_ERROR"))
    }

    "return 503-ServiceUnavailable when status is SERVICE_UNAVAILABLE" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000001IN&dateFrom=2026-01-01&dateTo=2026-12-31&status=SERVICE_UNAVAILABLE")
      )

      status(result) mustBe Status.SERVICE_UNAVAILABLE
      (JsPath \ "failures" \ 0 \ "code")(contentAsJson(result)) mustBe List(JsString("SERVICE_UNAVAILABLE"))
    }

    "return 400-BadRequest when mandatory query params are missing" in {
      val result = controller.getIhtpOverview()(overviewRequest(""))

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }
  }
}
