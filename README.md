# national-duty-repayment-center

Backend microservice supporting the submission of NDRC claims from the [national-duty-repayment-center-frontend](https://github.com/hmrc/national-duty-repayment-center-frontend) microservice

Other related NDRC services:
- Frontend service: [National-Duty-Repayment-Center-Frontend](https://github.com/hmrc/national-duty-repayment-center-frontend)
- Stubs: [National-Duty-Repayment-Center-Stubs](https://github.com/hmrc/national-duty-repayment-center-stubs)

## Local Setup

1. Checkout this repo
1. Start dependent services with [service-manager](https://github.com/hmrc/service-manager): `sm2 --start NDRC_ALL`
1. Stop the `service-manager` owned version of the service: `sm2 --stop NATIONAL_DUTY_REPAYMENT_CENTER`
1. Start the service: `sbt run`

Ensure you get a JSON response from `curl -i http://localhost:8451/`

## API

| Method | Url | Required Headers | RequestBody | Response | 
| --- | --- | --- | --- | --- |
| POST | /create-case | x-correlation-id | JSON - [request model](./app/uk/gov/hmrc/nationaldutyrepaymentcenter/models/requests/CreateClaimRequest.scala) | JSON - [response model](./app/uk/gov/hmrc/nationaldutyrepaymentcenter/models/responses/EISCreateCaseResponse.scala) |
| POST | /amend-case | x-correlation-id | JSON - [request model](./app/uk/gov/hmrc/nationaldutyrepaymentcenter/models/requests/AmendClaimRequest.scala) | JSON - [response model](./app/uk/gov/hmrc/nationaldutyrepaymentcenter/models/responses/EISAmendCaseResponse.scala) |

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
