package me.blvckbytes.springtesting.http

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object HttpClient {

  var baseUrl: String = "http://localhost"
  var port: Int = 80

  fun performRequest(
    path: String,
    requestMethod: HttpMethod,
    requestHeaders: MultiValueStringMapBuilder = MultiValueStringMapBuilder(),
    requestParams: MultiValueStringMapBuilder = MultiValueStringMapBuilder(),
    requestBody: JSONObject? = null
  ): HttpResponse {
    val requestUrl = URL(buildTargetPath(path, requestParams))
    val connection = requestUrl.openConnection() as HttpURLConnection

    requestHeaders.override("Content-Type", "application/json")
    requestHeaders.override("Accept", "application/json")

    connection.requestMethod = requestMethod.name
    connection.doOutput = true

    requestHeaders.apply(connection)

    if (requestBody != null) {
      connection.outputStream.use {
        val bytes = requestBody.toString().toByteArray(Charsets.UTF_8)
        it.write(bytes, 0, bytes.size)
        it.flush()
      }
    }

    val responseString = BufferedReader(InputStreamReader(
      if (connection.responseCode in 200..299)
        connection.inputStream
      else
        connection.errorStream
    )).use {
      val responseBuilder = StringBuilder()

      while (true)
        responseBuilder.append(it.readLine() ?: break)

      responseBuilder.toString()
    }

    return HttpResponse(
      HttpRequest(
        requestUrl.toString(),
        requestMethod,
        requestHeaders.toMap(),
        requestBody
      ),
      connection.responseCode,
      if (responseString.isEmpty()) null else JSONObject(responseString),
      connection.headerFields
    )
  }

  fun performStandardGetRequest(
    path: String,
    requestHeaders: MultiValueStringMapBuilder = MultiValueStringMapBuilder(),
    requestParams: MultiValueStringMapBuilder = MultiValueStringMapBuilder(),
    requestBody: JSONObject? = null
  ): HttpResponse {
    return performRequest(path, HttpMethod.GET, requestHeaders, requestParams, requestBody)
      .log()
      .expectStatusCode(HttpStatus.OK)
      .expectContentTypeJson()
  }

  private fun buildRequestParameterString(params: MultiValueStringMapBuilder): String {
    val result = StringBuilder()

    for (entry in params.toMap()) {
      for (value in entry.value) {
        if (result.isNotEmpty())
          result.append('&')

        result
          .append(URLEncoder.encode(entry.key, Charsets.UTF_8))
          .append('=')
          .append(URLEncoder.encode(value, Charsets.UTF_8))
      }
    }

    return result.toString()
  }

  private fun buildTargetPath(
    path: String,
    params: MultiValueStringMapBuilder
  ): String {
    var result = joinPaths("$baseUrl:$port", path)
    val parameterString = buildRequestParameterString(params)

    if (parameterString.isNotEmpty())
      result += "?$parameterString"

    return result
  }

  private fun joinPaths(vararg paths: String): String {
    val result = StringBuilder()

    for (path in paths) {
      if (result.isEmpty()) {
        result.append(path)
        continue
      }

      if (result.last() == '/') {
        if (path[0] == '/') {
          result.append(path.substring(1))
          continue
        }

        result.append(path)
        continue
      }

      if (path[0] == '/') {
        result.append(path)
        continue
      }

      result.append('/').append(path)
    }

    return result.toString()
  }
}