package me.blvckbytes.springtesting.http

import java.net.HttpURLConnection

class MultiValueStringMapBuilder {

  private val headerMap = mutableMapOf<String, MutableList<String>>()

  fun add(key: String, value: String): MultiValueStringMapBuilder {
    headerMap.computeIfAbsent(key) { mutableListOf() }.add(value)
    return this
  }

  fun override(key: String, value: String): MultiValueStringMapBuilder {
    headerMap[key] = mutableListOf(value)
    return this
  }

  fun add(key: String, value: Int): MultiValueStringMapBuilder {
    return add(key, value.toString())
  }

  fun override(key: String, value: Int): MultiValueStringMapBuilder {
    return override(key, value.toString())
  }

  fun apply(connection: HttpURLConnection) {
    for (entry in headerMap) {
      for (value in entry.value) {
        connection.setRequestProperty(entry.key, value)
      }
    }
  }

  fun toMap(): Map<String, List<String>> {
    return headerMap
  }
}