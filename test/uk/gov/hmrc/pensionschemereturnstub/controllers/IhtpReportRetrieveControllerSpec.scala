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
import play.api.libs.json.{JsObject, JsPath, JsString, JsValue}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.inheritancetaxonpensionsstubs.controllers.IhtpReportRetrieveController
import uk.gov.hmrc.inheritancetaxonpensionsstubs.utils.APIResponses
import uk.gov.hmrc.pensionschemereturnstub.base.SpecBase

class IhtpReportRetrieveControllerSpec extends SpecBase with APIResponses {

  private val controller = app.injector.instanceOf[IhtpReportRetrieveController]
  private val correlationId = "d59434ad-6e01-4467-9209-66858e778736"

  private def retrieveRequest(queryString: String) = FakeRequest("GET", s"/$queryString").withHeaders(
    "correlationid" -> correlationId,
    "X-Message-Type" -> "Request",
    "X-Originating-System" -> "MDTP",
    "X-Receipt-Date" -> "2026-04-10T16:12:49Z",
    "X-Regime-Type" -> "IHTP",
    "X-Transmitting-System" -> "HIP"
  )

  "GET ihtp report" must {

    "return 200-Ok for a known fbNumber" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&fbNumber=119000004320")
      )

      status(result) mustBe Status.OK
      header("correlationid", result).value mustBe correlationId

      val content = contentAsJson(result)
      (JsPath \ "success" \ "schemeDetails" \ "pstr")(content) mustBe List(JsString("24000001IN"))
      (JsPath \ "success" \ "schemeDetails" \ "schemeName")(content) mustBe List(JsString("Test Scheme"))
      (JsPath \ "success" \ "schemeDetails" \ "schemeStartDate")(content) mustBe List(JsString("1980-01-01"))
      (JsPath \ "success" \ "schemeDetails" \ "schemeEndDate")(content) mustBe List(JsString("2030-01-01"))
      (JsPath \ "success" \ "reportDetails" \ "ihtPaymentReference")(content) mustBe List(JsString("A123456/25A629671"))

      (JsPath \ "fbNumber")(content) mustBe empty
      (JsPath \ "success" \ "deceased")(content) must not be empty
      (JsPath \ "success" \ "deceased" \ "deceasedChangeFlag")(content) mustBe empty
      (JsPath \ "success" \ "deceased" \ "deceasedPersonalDetails")(content) must not be empty
      (JsPath \ "success" \ "deceased" \ "deceasedDetails")(content) must not be empty
      (JsPath \ "success" \ "personalRep")(content) must not be empty
      (JsPath \ "success" \ "personalRep" \ "prChangeFlag")(content) mustBe empty
      (JsPath \ "success" \ "personalRep" \ "typeOfPR")(content) mustBe List(JsString("01"))
      (JsPath \ "success" \ "personalRep" \ "prContactDetails")(content) must not be empty
      (JsPath \ "success" \ "personalRep" \ "prAddress")(content) must not be empty

      (JsPath \ "success" \ "ihTaxInformation")(content) must not be empty
      (JsPath \ "success" \ "ihTaxInformation" \ "ihTaxChangeFlag")(content) mustBe empty
      (JsPath \ "success" \ "beneficiaries")(content) must not be empty
      (content \ "success" \ "beneficiaries")
        .as[Seq[JsObject]]
        .map(b => b.keys must not contain "beneficiaryChangeFlag")
      (JsPath \ "success" \ "declarations")(content) must not be empty
    }

    "return 200-Ok for a second known fbNumber" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000002IN&fbNumber=119000004322")
      )

      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      (JsPath \ "success" \ "schemeDetails" \ "pstr")(content) mustBe List(JsString("24000002IN"))
      (JsPath \ "success" \ "schemeDetails" \ "schemeName")(content) mustBe List(JsString("Test Scheme"))
      (JsPath \ "success" \ "schemeDetails" \ "schemeStartDate")(content) mustBe List(JsString("1980-01-01"))
      (JsPath \ "success" \ "schemeDetails" \ "schemeEndDate")(content) mustBe List(JsString("2030-01-01"))
      (JsPath \ "success" \ "reportDetails" \ "ihtPaymentReference")(content) mustBe List(JsString("A654321/25A999999"))

      (JsPath \ "fbNumber")(content) mustBe empty
      (JsPath \ "success" \ "deceased")(content) must not be empty
      (JsPath \ "success" \ "deceased" \ "deceasedChangeFlag")(content) mustBe empty
      (JsPath \ "success" \ "deceased" \ "deceasedPersonalDetails")(content) must not be empty
      (JsPath \ "success" \ "deceased" \ "deceasedDetails")(content) must not be empty
      (JsPath \ "success" \ "personalRep")(content) must not be empty
      (JsPath \ "success" \ "personalRep" \ "prChangeFlag")(content) mustBe empty
      (JsPath \ "success" \ "personalRep" \ "typeOfPR")(content) mustBe List(JsString("01"))
      (JsPath \ "success" \ "personalRep" \ "prContactDetails")(content) must not be empty
      (JsPath \ "success" \ "personalRep" \ "prAddress")(content) must not be empty

      (JsPath \ "success" \ "ihTaxInformation")(content) must not be empty
      (JsPath \ "success" \ "ihTaxInformation" \ "ihTaxChangeFlag")(content) mustBe empty
      (JsPath \ "success" \ "beneficiaries")(content) must not be empty
      (content \ "success" \ "beneficiaries")
        .as[Seq[JsObject]]
        .map(b => b.keys must not contain "beneficiaryChangeFlag")
      (JsPath \ "success" \ "declarations")(content) must not be empty
      (JsPath \ "success" \ "declarations" \ "submittedBy")(content) mustBe List(JsString("PSP"))
      (JsPath \ "success" \ "declarations" \ "submitterID")(content) mustBe List(JsString("A1816536"))
      (JsPath \ "success" \ "declarations" \ "pspDeclaration" \ "psaid")(content) mustBe List(JsString("A2100005"))

    }

    "return 200-Ok for known paymentReference and versionNumber" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&paymentReferenceNumber=A123456/25A629671&versionNumber=001")
      )

      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      (JsPath \ "success" \ "schemeDetails" \ "pstr")(content) mustBe List(JsString("24000001IN"))
      (JsPath \ "success" \ "schemeDetails" \ "schemeName")(content) mustBe List(JsString("Test Scheme"))
      (JsPath \ "success" \ "schemeDetails" \ "schemeStartDate")(content) mustBe List(JsString("1980-01-01"))
      (JsPath \ "success" \ "schemeDetails" \ "schemeEndDate")(content) mustBe List(JsString("2030-01-01"))
      (JsPath \ "success" \ "reportDetails" \ "ihtPaymentReference")(content) mustBe List(JsString("A123456/25A629671"))

      (JsPath \ "fbNumber")(content) mustBe empty
      (JsPath \ "success" \ "deceased")(content) must not be empty
      (JsPath \ "success" \ "deceased" \ "deceasedChangeFlag")(content) mustBe empty
      (JsPath \ "success" \ "deceased" \ "deceasedPersonalDetails")(content) must not be empty
      (JsPath \ "success" \ "deceased" \ "deceasedDetails")(content) must not be empty
      (JsPath \ "success" \ "personalRep")(content) must not be empty
      (JsPath \ "success" \ "personalRep" \ "prChangeFlag")(content) mustBe empty
      (JsPath \ "success" \ "personalRep" \ "typeOfPR")(content) mustBe List(JsString("01"))
      (JsPath \ "success" \ "personalRep" \ "prContactDetails")(content) must not be empty
      (JsPath \ "success" \ "personalRep" \ "prAddress")(content) must not be empty

      (JsPath \ "success" \ "ihTaxInformation")(content) must not be empty
      (JsPath \ "success" \ "ihTaxInformation" \ "ihTaxChangeFlag")(content) mustBe empty
      (JsPath \ "success" \ "beneficiaries")(content) must not be empty
      (content \ "success" \ "beneficiaries")
        .as[Seq[JsObject]]
        .map(b => b.keys must not contain "beneficiaryChangeFlag")
      (JsPath \ "success" \ "declarations")(content) must not be empty
      (JsPath \ "success" \ "declarations" \ "submittedBy")(content) mustBe List(JsString("PSA"))
      (JsPath \ "success" \ "declarations" \ "submitterID")(content) mustBe List(JsString("A2100005"))
      (JsPath \ "success" \ "declarations" \ "pspDeclaration" \ "psaid")(content) mustBe empty
    }

    "return the pinned and amended versions of a report by fbNumber" in {
      val versionOneResult = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&fbNumber=119000004360")
      )
      val versionTwoResult = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&fbNumber=119000004361")
      )

      status(versionOneResult) mustBe Status.OK
      status(versionTwoResult) mustBe Status.OK

      val versionOne = contentAsJson(versionOneResult)
      val versionTwo = contentAsJson(versionTwoResult)

      (versionOne \ "success" \ "deceased" \ "deceasedPersonalDetails") mustBe
        (versionTwo \ "success" \ "deceased" \ "deceasedPersonalDetails")

      (versionOne \ "success" \ "deceased" \ "deceasedChangeFlag").toOption mustBe None
      (versionTwo \ "success" \ "deceased" \ "deceasedChangeFlag").toOption mustBe None

      (versionOne \ "success" \ "beneficiaries")
        .as[Seq[JsObject]]
        .map(b => b.keys must not contain "beneficiaryChangeFlag")

      (versionTwo \ "success" \ "ihTaxInformation" \ "ihTaxChangeFlag").as[String] mustBe "Yes"
      (versionTwo \ "success" \ "beneficiaries").as[Seq[JsObject]].length mustBe 2
      (versionTwo \ "success" \ "beneficiaries")
        .as[Seq[JsObject]]
        .map(b => (b \ "beneficiaryChangeFlag").toOption.map(_.as[JsString])) mustBe Seq(Some(JsString("Yes")), None)

      (versionOne \ "success" \ "ihTaxInformation" \ "totalIHTPayable").as[Double] mustBe 100.00
      (versionTwo \ "success" \ "ihTaxInformation" \ "totalIHTPayable").as[Double] mustBe 120.00
    }

    "return each amendment version by paymentReference and versionNumber" in {
      val versionOneByFbNumber = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&fbNumber=119000004360")
      )
      val versionOneByPaymentReference = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&paymentReferenceNumber=A556789/26A999999&versionNumber=001")
      )
      val versionTwoByFbNumber = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&fbNumber=119000004361")
      )
      val versionTwoByPaymentReference = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&paymentReferenceNumber=A556789/26A999999&versionNumber=002")
      )

      status(versionOneByPaymentReference) mustBe Status.OK
      status(versionTwoByPaymentReference) mustBe Status.OK
      contentAsJson(versionOneByPaymentReference) mustBe contentAsJson(versionOneByFbNumber)
      contentAsJson(versionTwoByPaymentReference) mustBe contentAsJson(versionTwoByFbNumber)
    }

    "return the additional paid version 001 reports by fbNumber and paymentReference" in {
      Seq(
        ("119000004362", "F246810/26B999999"),
        ("119000004363", "A975310/26C999999")
      ).foreach { case (fbNumber, paymentReference) =>
        val byFbNumber = controller.getIhtpReport()(
          retrieveRequest(s"?pstr=24000001IN&fbNumber=$fbNumber")
        )
        val byPaymentReference = controller.getIhtpReport()(
          retrieveRequest(
            s"?pstr=24000001IN&paymentReferenceNumber=$paymentReference&versionNumber=001"
          )
        )

        status(byFbNumber) mustBe Status.OK
        status(byPaymentReference) mustBe Status.OK
        contentAsJson(byPaymentReference) mustBe contentAsJson(byFbNumber)

        val content = contentAsJson(byFbNumber)
        (content \ "success" \ "reportDetails" \ "ihtPaymentReference")
          .as[String] mustBe paymentReference.replace("-", "")
      }
    }

    "return only false change flags for every version 001 report" in {
      Seq(
        "?pstr=24000001IN&fbNumber=119000004320",
        "?pstr=24000002IN&fbNumber=119000004322",
        "?pstr=24000001IN&fbNumber=119000004360",
        "?pstr=24000001IN&fbNumber=119000004362",
        "?pstr=24000001IN&fbNumber=119000004363",
        "?pstr=24000036IN&fbNumber=119000004364",
        "?pstr=24000036IN&fbNumber=119000004366",
        "?pstr=00000042IN&fbNumber=119000004368",
        "?pstr=00000042IN&fbNumber=119000004370",
        "?pstr=24000001IN&paymentReferenceNumber=A123456/25A629671&versionNumber=001",
        "?pstr=24000001IN&paymentReferenceNumber=A556789/26A999999&versionNumber=001"
      ).foreach { queryString =>
        val result = controller.getIhtpReport()(retrieveRequest(queryString))

        status(result) mustBe Status.OK
        val content = contentAsJson(result)

        val beneficiaryChangeFlags = (content \ "success" \ "beneficiaries")
          .as[Seq[JsObject]]
          .map(b => (b \ "beneficiaryChangeFlag").toOption)

        val deceasedChangeFlag = (content \ "success" \ "deceased" \ "deceasedChangeFlag").toOption
        val prChangeFlag = (content \ "success" \ "personalRep" \ "prChangeFlag").toOption
        val ihTaxInformationChangeFlag = (content \ "success" \ "ihTaxInformation" \ "ihTaxChangeFlag").toOption

        val allChangeFlags =
          beneficiaryChangeFlags ++ Seq(deceasedChangeFlag, prChangeFlag, ihTaxInformationChangeFlag)
        allChangeFlags.foreach(_ mustBe None)

      }
    }

    "return 422-UnprocessableEntity for an unknown fbNumber" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&fbNumber=999999999999")
      )

      status(result) mustBe Status.UNPROCESSABLE_ENTITY
      header("correlationid", result).value mustBe correlationId
      (JsPath \ "errors" \ "code")(contentAsJson(result)) mustBe List(JsString("003"))
      (JsPath \ "errors" \ "text")(contentAsJson(result)) mustBe List(JsString("Request could not be processed"))
    }

    "return 422-UnprocessableEntity when pstr does not match the resource file's pstr" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000002IN&fbNumber=119000004320")
      )

      status(result) mustBe Status.UNPROCESSABLE_ENTITY
      header("correlationid", result).value mustBe correlationId
      (JsPath \ "errors" \ "code")(contentAsJson(result)) mustBe List(JsString("003"))
      (JsPath \ "errors" \ "text")(contentAsJson(result)) mustBe List(JsString("Request could not be processed"))
    }

    "return 400-BadRequest for invalid parameter combination (fbNumber with paymentReferenceNumber)" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&fbNumber=119000004320&paymentReferenceNumber=A123456/25A-629671")
      )

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }

    "return 400-BadRequest when pstr is missing" in {
      val result = controller.getIhtpReport()(retrieveRequest(""))

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }

    "return 400-BadRequest when both fbNumber and paymentReferenceNumber are missing" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN")
      )

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }

    "return 400-BadRequest when paymentReferenceNumber is provided without versionNumber" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&paymentReferenceNumber=A123456/25A-629671")
      )

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }

    "return 400-BadRequest when versionNumber is provided without paymentReferenceNumber" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&versionNumber=001")
      )

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }
  }
}
