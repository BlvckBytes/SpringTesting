package me.blvckbytes.springtesting.validation

import me.blvckbytes.springtesting.http.HttpResponse

class CreationResult(
  val response: HttpResponse,
  val validator: BodyValidator
)