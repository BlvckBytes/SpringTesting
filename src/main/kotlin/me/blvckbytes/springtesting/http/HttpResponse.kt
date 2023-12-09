package me.blvckbytes.springtesting.http

import me.blvckbytes.springtesting.validation.JsonObjectExtractor
import org.json.JSONObject

class HttpResponse(
  val request: HttpRequest,
  val statusCode: Int,
  body: JSONObject?,
  val headers: Map<String, List<String>>
): JsonObjectExtractor(body) {
  fun expectContentTypeJson(): HttpResponse {
    expectHeader("Content-Type", "application/json")
    return this
  }

  fun expectHeader(key: String, value: String): HttpResponse {
    val values = headers[key] ?: throw AssertionError("Expected presence of header $key")

    if (!values.any { it == value })
      throw java.lang.AssertionError("Header $key didn't have value $value (${values.joinToString()})")

    return this
  }

  fun expectStatusCode(status: HttpStatus): HttpResponse {
    if (statusCode != status.code)
      throw AssertionError("Expected status code ${status.code} but found $statusCode")

    return this
  }

  fun log(): HttpResponse {
    println(request.toString())
    println(toString())
    return this
  }

  override fun toString(): String {
    val result = StringBuilder()

    result.append("STATUS $statusCode\n")

    for (header in headers) {
      for (value in header.value) {
        result.append("${header.key}: ${value}\n")
      }
    }

    result.append(body?.toString(2) ?: "<no body>").append('\n')

    return result.toString()
  }
}