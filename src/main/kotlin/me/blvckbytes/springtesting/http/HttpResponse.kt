package me.blvckbytes.springtesting.http

import me.blvckbytes.springtesting.validation.JsonObjectExtractor
import org.json.JSONObject

class HttpResponse(
  val request: HttpRequest,
  val statusCode: Int,
  body: JSONObject?,
  val responseString: String,
  val headers: Map<String, List<String>>
): JsonObjectExtractor(body) {
  fun getHeader(key: String): String? {
    val headers = getHeaders(key)

    if (headers.size > 1)
      throw IllegalStateException("Encountered multiple headers for key=$key")

    return headers.getOrNull(0)
  }

  fun getHeaders(key: String): List<String> {
    return buildList {
      for (headerEntry in headers) {
        if (headerEntry.key.equals(key, ignoreCase = true))
          addAll(headerEntry.value)
      }
    }
  }

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

    result.append(body?.toString(2) ?: (responseString.ifBlank { "<no body>" })).append('\n')

    return result.toString()
  }
}