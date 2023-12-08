package me.blvckbytes.springtesting.http

import org.json.JSONObject

class HttpRequest(
  val url: String,
  val method: HttpMethod,
  val headers: Map<String, List<String>>,
  val body: JSONObject?
) {
  override fun toString(): String {
    val result = StringBuilder()

    result.append("$method $url\n")

    for (header in headers) {
      for (value in header.value) {
        result.append("${header.key}: ${value}\n")
      }
    }

    result.append(body?.toString(2) ?: "<no body>").append('\n')

    return result.toString()
  }
}