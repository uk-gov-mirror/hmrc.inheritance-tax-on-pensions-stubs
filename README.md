
# Inheritance Tax on Pensions Stubs

Microservice to provide endpoints to replicate request and response from the IHTP API.
Inheritance Tax on Pensions is a feature on manage your pension (MPS) service. Pension Scheme Administrators (PSA) and/or
Pension Scheme Practitioners use this service for reporting IHT due on unused pension funds and retrieving payment reference.

## Endpoints

### Submit IHTP report

- **URL**: `/etmp/RESTAdapter/pods/reports/ihtp`
- **Method**: `POST`

The body of the payload is the report details built from user answers to be submitted down to ETMP.

#### Example individual payload

```json
{
  "reportDetails": {
    "pstr": "S2400000001"
  },
  "deceasedDetails": {
    "inheritanceTaxReference": "A123456/25A",
    "title": "Mr",
    "firstForename": "Firstname",
    "secondForename": "Middlename",
    "surname": "Surname",
    "dateOfBirth": "1950-01-01",
    "dateOfDeath": "2026-01-01",
    "nino": null,
    "reasonForNoNino": "Reason for no national insurance number"
  },
  "prDetails": {
    "individual": {
      "title": "Mr",
      "firstForename": "Firstname",
      "secondForename": "Middlename",
      "surname": "Surname"
    }
  }
}
```

#### Example organisation payload

```json
{
  "reportDetails": {
    "pstr": "S2400000001"
  },
  "deceasedDetails": {
    "inheritanceTaxReference": "A123456/25A",
    "title": "Mr",
    "firstForename": "Firstname",
    "secondForename": "Middlename",
    "surname": "Surname",
    "dateOfBirth": "1950-01-01",
    "dateOfDeath": "2026-01-01",
    "nino": null,
    "reasonForNoNino": "Reason for no national insurance number"
  },
  "prDetails": {
    "organisation": {
      "organisationName": "Surname Incorporated",
      "title": "Ms",
      "firstForename": "FirstnameA",
      "secondForename": "MiddlenameB",
      "surname": "Surname"
    }
  }
}
```

#### Submit report stub scenarios

The last character of the `inheritanceTaxReference` is used to return specific error scenarios.

| Scenario | `inheritanceTaxReference` suffix | Response |
| --- | --- | --- |
| Success | `A123456/25A` | `200 OK` |
| Bad request | `A123456/25B` | `400 Bad Request` |
| Internal server error | `A123456/25C` | `500 Internal Server Error` |
| Service unavailable | `A123456/25D` | `503 Service Unavailable` |
| Unprocessable entity | `A123456/25E` | `422 Unprocessable Entity` |

### Get IHTP overview

- **URL**: `/etmp/RESTAdapter/pods/reports/ihtp-overview`
- **Method**: `GET`

**Query parameters**:

- `pstr` - required
- `dateFrom` - required, for example `2026-01-01`
- `dateTo` - required, for example `2026-12-31`
- `status` - optional, for example `Not reconciled`, `In progress` or `Paid`

#### Overview stub scenarios

The overview endpoint can return different responses by changing query parameter values. This is intentionally deterministic so
that Bruno and frontend/backend tests can exercise success and error paths without needing realistic ETMP data.

Known PSTRs:

- `00000042IN` (SRN: `S0000000042`, includes two amendment scenarios for IHTP-574)
- `24000001IN`
- `24000002IN`
- `24000036IN` (SRN: `S2400000036`, includes two amendment scenarios for IHTP-574)

For each IHTP-574 amendment scenario, the overview contains versions `001` and `002` with the same payment reference and
different form bundle numbers. Matching retrieve resources are available for every version by either form bundle number or the
payment reference and version combination. This allows the backend submission-list filtering and report drill-down to be tested
against representative obsolete versions.

| Scenario | Query values | Response |
| --- | --- | --- |
| Successful overview list | Known `pstr`, matching `dateFrom` / `dateTo`, no `status` | `200 OK` with all matching overview records |
| Successful filtered overview list | Known `pstr`, matching `dateFrom` / `dateTo`, `status=Not reconciled` | `200 OK` with matching not reconciled overview records |
| Successful filtered overview list | Known `pstr`, matching `dateFrom` / `dateTo`, `status=In progress` | `200 OK` with matching in progress overview records |
| Successful filtered overview list | Known `pstr`, matching `dateFrom` / `dateTo`, `status=Paid` | `200 OK` with the two additional paid version 001 records |
| No records found | Unknown `pstr` | `422 Unprocessable Entity` |
| No records found | Known `pstr`, date range with no matching overview records | `422 Unprocessable Entity` |
| No records found | Known `pstr`, normal `status` value with no matching overview records | `422 Unprocessable Entity` |
| Forced no records response | `status=NO_RECORDS` | `422 Unprocessable Entity` |
| Forced bad request response | `status=BAD_REQUEST` | `400 Bad Request` |
| Forced internal server error response | `status=SERVER_ERROR` | `500 Internal Server Error` |
| Forced service unavailable response | `status=SERVICE_UNAVAILABLE` | `503 Service Unavailable` |

### Get IHTP report

- **URL**: `/etmp/RESTAdapter/pods/reports/ihtp`
- **Method**: `GET`

**Query parameters**:

- `pstr` - required
- `fbNumber` - optional, used for specific record retrieval (12-digit pattern: `^[0-9]{12}$`)
- `paymentReferenceNumber` - optional, must be used with `versionNumber`; format is the 11-character inheritance tax
  reference followed by 6 digits (for example, `A123456/25A556789`)
- `versionNumber` - optional, must be used with `paymentReferenceNumber` (3-digit pattern: `^[0-9]{3}$`)

**Parameter combinations**:
- `pstr` + `fbNumber` - retrieve by form bundle number
- `pstr` + `paymentReferenceNumber` + `versionNumber` - retrieve by payment reference and version

#### Retrieve stub scenarios

The retrieve endpoint can return different responses by changing query parameter values. This is intentionally deterministic so
that Bruno and frontend/backend tests can exercise success and error paths without needing realistic ETMP data.

Known fbNumbers:

- `119000004320` (PSTR: 24000001IN)
- `119000004322` (PSTR: 24000002IN)
- `119000004360` (PSTR: 24000001IN, amendment version 001)
- `119000004361` (PSTR: 24000001IN, amendment version 002)
- `119000004362` (PSTR: 24000001IN, paid version 001)
- `119000004363` (PSTR: 24000001IN, paid version 001)
- `119000004364` and `119000004365` (PSTR: 24000036IN, paid amendment versions 001 and 002)
- `119000004366` and `119000004367` (PSTR: 24000036IN, not reconciled amendment versions 001 and 002)
- `119000004368` and `119000004369` (PSTR: 00000042IN, paid amendment versions 001 and 002)
- `119000004370` and `119000004371` (PSTR: 00000042IN, not reconciled amendment versions 001 and 002)

Known paymentReference + version combinations:

- `A123456/25A629671` + `001` (PSTR: 24000001IN)
- `A556789/26A758204` + `001` (PSTR: 24000001IN, pinned amendment baseline)
- `A556789/26A758204` + `002` (PSTR: 24000001IN, current amended report)
- `F246810/26B314159` + `001` (PSTR: 24000001IN, paid Firstname Middlename Surname report)
- `A975310/26C271828` + `001` (PSTR: 24000001IN, paid FirstnameA MiddlenameA Surname report)
- `A240036/26A836241` + `001` or `002` (PSTR: 24000036IN, paid amendment)
- `F360024/26B472915` + `001` or `002` (PSTR: 24000036IN, not reconciled amendment)
- `A000042/26C604218` + `001` or `002` (PSTR: 00000042IN, paid amendment)
- `F420000/26D195307` + `001` or `002` (PSTR: 00000042IN, not reconciled amendment)

#### Amendment stub scenario

The overview for PSTR `24000001IN` contains two versions of the report identified by inheritance tax reference
`A556789/26A` and payment reference `A556789/26A758204`.

| Version | fbNumber | Retrieve status | Change flags |
| --- | --- | --- | --- |
| `001` | `119000004360` | `Paid` | All section and beneficiary flags are `false` |
| `002` | `119000004361` | `Submitted` | IHT tax information and beneficiary details are `true`; the first beneficiary is `true` |

Both versions can be retrieved either by their fbNumber or by `A556789/26A758204` with the corresponding version number. The
deceased and PR details are unchanged. Version 002 increases the tax totals and the amount assigned to the first beneficiary.
All `changeFlag` values are `false` for initial version `001` reports. Later versions set flags to `true` only for sections or
beneficiaries changed by the amendment.

#### Additional paid version 001 scenarios

| fbNumber | Payment reference | Deceased                       | Overview status | Retrieve status | Version |
| --- | --- |--------------------------------| --- | --- | --- |
| `119000004362` | `F246810/26B314159` | Firstname Middlename Surname   | `Paid` | `Paid` | `001` |
| `119000004363` | `A975310/26C271828` | FirstnameA MiddlenameA Surname | `Paid` | `Paid` | `001` |

Both reports are included in the unfiltered overview for PSTR `24000001IN`, can be selected with `status=Paid`, and can be
retrieved either by their fbNumber or by their payment reference with `versionNumber=001`.

| Scenario | Query values | Response |
| --- | --- | --- |
| Successful retrieve by fbNumber | Known `pstr`, known `fbNumber` | `200 OK` with full report payload |
| Successful retrieve by payment reference | Known `pstr`, known `paymentReferenceNumber` + `versionNumber` | `200 OK` with full report payload |
| No records found | Known `pstr`, unknown `fbNumber` | `422 Unprocessable Entity` |
| No records found | PSTR does not match the resource file's PSTR | `422 Unprocessable Entity` |
| Bad request | Invalid parameter combination (e.g., fbNumber with paymentReferenceNumber) | `400 Bad Request` |
| Bad request | Missing required parameters (no fbNumber or paymentReferenceNumber + versionNumber) | `400 Bad Request` |

## Running the service

1. Make sure you run all the dependant services through the service manager:

   > `sm2 --start IHTP_ALL`

2. Stop the frontend microservice from the service manager and run it locally:

   > `sm2 --stop INHERITANCE_TAX_ON_PENSIONS_STUBS`

   > `sbt run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes`

The service runs on port `10712` by default. E.g:  http://localhost:10712/ping/ping

## Testing with Bruno

If you do not have Bruno installed you can install it with the following command:

`brew install bruno`

A Bruno collection is available in `test/resources/IHTP Stubs`.

The stubs collection does not require a login request. Requests are sent directly to the stubbed ETMP-style endpoints and use
`auth: none`.

To use it:

1. Open Bruno.
2. Select **Open Collection**.
3. Open the `test/resources/IHTP Stubs` folder.
4. Select the `LocalHost - IHTP Stubs` environment.
5. Run the stubs service locally on port `10712`.
6. Run one of the requests listed below.

Useful requests:

- `Ping` - checks the stubs service is running
- `Submit - Success` - exercises the successful submit report response
- `Overview - Success` - exercises the successful overview response, including amendment versions and the two paid version 001 reports
- `Overview - No Records 422` - exercises the no records overview response
- `Overview - Bad Request 400` - exercises the forced bad request overview response
- `Overview - Server Error 500` - exercises the forced internal server error overview response
- `Overview - Service Unavailable 503` - exercises the forced service unavailable overview response
- `Retrieve - Success (fbNumber)` - exercises the successful retrieve by fbNumber response
- `Retrieve - Success (paymentReference + version)` - exercises the successful retrieve by payment reference response
- `Retrieve - Amendment Version 001` - retrieves the pinned paid version
- `Retrieve - Amendment Version 002` - retrieves the current version and its change flags
- `Retrieve - Paid Version 001 (Firstname Surname)` - retrieves the additional paid Firstname Middlename Surname report
- `Retrieve - Paid Version 001 (FirstnameA Surname)` - retrieves the additional paid FirstnameA MiddlenameA Surname report
- `Retrieve - No Records 422` - exercises the no records retrieve response
- `Retrieve - PSTR Mismatch 422` - exercises the PSTR mismatch retrieve response
- `Retrieve - Bad Request 400` - exercises the bad request retrieve response

### Unit tests

> `sbt test`

### Integration tests

> `sbt it/test`

You can also execute the [runtests.sh](runtests.sh) file to run both unit and integration tests and generate coverage report easily.

```bash
/bin/bash ./runtests.sh
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
