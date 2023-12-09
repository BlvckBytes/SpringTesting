package me.blvckbytes.springtesting.http

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.cast

class HttpResponse(
  val request: HttpRequest,
  val statusCode: Int,
  val body: JSONObject?,
  val headers: Map<String, List<String>>
) {
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

  fun <T> extractValue(path: String, type: KClass<T>): T where T : Any {
    if (body == null)
      throw AssertionError("Expected a body to be present")

    val pathParts = path.split(".")
    var currentNode: Any? = body

    for (pathPart in pathParts) {
      if (currentNode is JSONObject) {
        currentNode = currentNode.get(pathPart)
        continue
      }

      if (currentNode is JSONArray) {
        currentNode = currentNode.get(
          pathPart.toIntOrNull() ?: throw IllegalStateException("Cannot use $pathPart to index an array")
        )
        continue
      }

      if (currentNode == null)
        throw AssertionError("Path part $pathPart could not be resolved, because it's predecessor was null ($path)")

      throw IllegalStateException("Don't know how to access a node of type ${currentNode.javaClass.simpleName}")
    }

    if (type == UUID::class && currentNode is String)
      return type.cast(UUID.fromString(currentNode))

    else if (!type.isInstance(currentNode))
      throw AssertionError("Expected type ${type.simpleName} at path $path, but found ${currentNode?.javaClass?.simpleName}")

    return type.cast(currentNode)
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