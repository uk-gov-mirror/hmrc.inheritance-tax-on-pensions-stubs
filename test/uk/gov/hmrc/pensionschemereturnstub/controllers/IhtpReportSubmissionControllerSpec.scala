/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.libs.json.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.inheritancetaxonpensionsstubs.controllers.IhtpReportSubmissionController
import uk.gov.hmrc.inheritancetaxonpensionsstubs.models.{IhtpPaymentNoticeSubmission, PrContactDetails}
import uk.gov.hmrc.inheritancetaxonpensionsstubs.utils.{APIResponses, JsonUtils}
import uk.gov.hmrc.pensionschemereturnstub.base.SpecBase

class IhtpReportSubmissionControllerSpec extends SpecBase with APIResponses {

  private val controller = app.injector.instanceOf[IhtpReportSubmissionController]
  private val jsonUtils = app.injector.instanceOf[JsonUtils]

  private val fakePostRequest = FakeRequest("POST", "/").withHeaders(
    ("CorrelationId", "testId"),
    "Authorization" -> "test Bearer token",
    ("Environment", "local")
  )

  "POST ihtp report" must {

    "return 200-Ok for a valid request" in {
      val validData = jsonUtils.readJsonFile(filePath = "conf/resources/data/validReturnSubmission.json")
      val postRequest = fakePostRequest.withJsonBody(validData)

      val result = controller.postIhtpReport()(postRequest)
      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      (JsPath \ "formBundleNo")(content) must not be empty
      (JsPath \ "ihtPaymentReference")(content) mustBe List(JsString("A123456/25A556789"))
    }

    "return 200-Ok for a valid organisation PR request" in {
      val validData = Json.obj(
        "reportDetails" -> Json.obj(
          "pstr" -> "S2400000001",
          "ihtPaymentReference" -> "A123456/25A556789"
        ),
        "deceased" -> Json.obj(
          "deceasedPersonalDetails" -> Json.obj(
            "title" -> "Mr",
            "firstForename" -> "Firstname",
            "secondForename" -> "Secondname",
            "surname" -> "Surname",
            "ninoExist" -> "No",
            "reasonNoNINO" -> "Reason for no national insurance number"
          ),
          "deceasedDetails" -> Json.obj(
            "deceasedsDOB" -> "1950-01-01",
            "deceasedsDOD" -> "2026-01-01",
            "ihtRefNumber" -> "A123456/25A"
          )
        ),
        "personalRep" -> Json.obj(
          "typeOfPR" -> "02",
          "prContactDetails" -> Json.obj(
            "orgName" -> "Test Organisation",
            "title" -> "Ms",
            "firstForename" -> "FirstnameA",
            "secondForename" -> "Ann",
            "surname" -> "Surname"
          ),
          "prAddress" -> Json.obj(
            "addressLine1" -> "1 ABCDE Street",
            "addressLine2" -> "FGHIJ Town",
            "postcode" -> "ZZ99 1AA",
            "country" -> "GB"
          )
        ),
        "ihTaxInformation" -> Json.obj(
          "dateNoticeReceived" -> "2026-03-27",
          "noticeSubmittedByPR" -> "Yes",
          "knownBeneficiaries" -> "No",
          "totalIHTPayable" -> 1000.00,
          "totalInterestPayable" -> 50.00,
          "total" -> 1050.00
        ),
        "declarations" -> Json.obj(
          "submittedBy" -> "PSA",
          "submitterID" -> "TODO",
          "psaDeclaration" -> Json.obj(
            "psaDeclaration1" -> "false",
            "psaDeclaration2" -> "false"
          )
        )
      )
      validData.validate[IhtpPaymentNoticeSubmission].map(_.personalRep.prContactDetails) mustBe JsSuccess(
        PrContactDetails(Some("Test Organisation"), Some("Ms"), "FirstnameA", Some("Ann"), "Surname")
      )

      val postRequest = fakePostRequest.withJsonBody(validData)

      val result = controller.postIhtpReport()(postRequest)
      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      (JsPath \ "formBundleNo")(content) must not be empty
      (JsPath \ "ihtPaymentReference")(content) mustBe List(JsString("A123456/25A556789"))
    }

    "return 400-BadRequest for an organisation request missing organisation name" in {
      val invalidData = Json.obj(
        "reportDetails" -> Json.obj(
          "pstr" -> "S2400000001"
        ),
        "deceasedDetails" -> Json.obj(
          "inheritanceTaxReference" -> "A123456/25A",
          "firstForename" -> "Firstname",
          "surname" -> "Surname",
          "dateOfBirth" -> "1950-01-01",
          "dateOfDeath" -> "2026-01-01",
          "reasonForNoNino" -> "Reason for no national insurance number"
        ),
        "prDetails" -> Json.obj(
          "organisation" -> Json.obj(
            "title" -> "Ms",
            "firstForename" -> "FirstnameA",
            "secondForename" -> "Ann",
            "surname" -> "Surname"
          )
        )
      )
      invalidData.validate[IhtpPaymentNoticeSubmission] mustBe a[JsError]

      val postRequest = fakePostRequest.withJsonBody(invalidData)

      val result = controller.postIhtpReport()(postRequest)
      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }

    "return 400-BadRequest for an organisation request missing PR name fields" in {
      val invalidData = Json.obj(
        "reportDetails" -> Json.obj(
          "pstr" -> "S2400000001"
        ),
        "deceasedDetails" -> Json.obj(
          "inheritanceTaxReference" -> "A123456/25A",
          "firstForename" -> "Firstname",
          "surname" -> "Surname",
          "dateOfBirth" -> "1950-01-01",
          "dateOfDeath" -> "2026-01-01",
          "reasonForNoNino" -> "Reason for no national insurance number"
        ),
        "prDetails" -> Json.obj(
          "organisation" -> Json.obj(
            "organisationName" -> "Test Organisation"
          )
        )
      )
      val postRequest = fakePostRequest.withJsonBody(invalidData)

      val result = controller.postIhtpReport()(postRequest)
      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }

    "return 400-BadRequest for a missing json body" in {
      val result = controller.postIhtpReport()(fakePostRequest)
      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }

    "return 400-BadRequest for an invalid srn" in {
      val badRequestData = jsonUtils.readJsonFile(filePath = "conf/resources/data/BadRequestSubmission.json")
      val postRequest = fakePostRequest.withJsonBody(badRequestData)

      val result = controller.postIhtpReport()(postRequest)
      status(result) mustBe Status.BAD_REQUEST
      val content = contentAsJson(result)
      (JsPath \ "failures" \ 0 \ "reason")(content) mustBe List(
        JsString("Submission has not passed validation. Invalid parameter srn.")
      )
      (JsPath \ "failures" \ 0 \ "code")(content) mustBe List(JsString("INVALID_SRN"))
    }

    "return 500-InternalServerError for an srn" in {
      val serverErrorData = jsonUtils.readJsonFile(filePath = "conf/resources/data/ServerErrorSubmission.json")
      val postRequest = fakePostRequest.withJsonBody(serverErrorData)

      val result = controller.postIhtpReport()(postRequest)
      status(result) mustBe Status.INTERNAL_SERVER_ERROR
      val content = contentAsJson(result)
      (JsPath \ "failures" \ 0 \ "reason")(content) mustBe List(
        JsString("Something went wrong.")
      )
      (JsPath \ "failures" \ 0 \ "code")(content) mustBe List(JsString("INTERNAL_SERVER_ERROR"))
    }

    "return 503-ServiceUnavailable for an srn" in {
      val unavailableData = jsonUtils.readJsonFile(filePath = "conf/resources/data/UnavailableSubmission.json")
      val postRequest = fakePostRequest.withJsonBody(unavailableData)

      val result = controller.postIhtpReport()(postRequest)
      status(result) mustBe Status.SERVICE_UNAVAILABLE
      val content = contentAsJson(result)
      (JsPath \ "failures" \ 0 \ "reason")(content) mustBe List(
        JsString("The remote endpoint has indicated that the service is unavailable")
      )
      (JsPath \ "failures" \ 0 \ "code")(content) mustBe List(JsString("SERVICE_UNAVAILABLE"))
    }

    "return 422-UnprocessableEntity for an srn" in {
      val unprocessableData = jsonUtils.readJsonFile(filePath = "conf/resources/data/UnprocessableSubmission.json")
      val postRequest = fakePostRequest.withJsonBody(unprocessableData)

      val result = controller.postIhtpReport()(postRequest)
      status(result) mustBe Status.UNPROCESSABLE_ENTITY
      val content = contentAsJson(result)
      (JsPath \ "failures" \ 0 \ "reason")(content) mustBe List(
        JsString("The remote endpoint returned unprocessable")
      )
      (JsPath \ "failures" \ 0 \ "code")(content) mustBe List(JsString("UNPROCESSABLE_ENTITY"))
    }
  }
}
