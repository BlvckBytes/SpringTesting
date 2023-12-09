package me.blvckbytes.springtesting.validation.validator

import me.blvckbytes.springtesting.validation.KeyValidator
import java.time.LocalDateTime

class LocalDateTimeKeyValidator(key: String, nullable: Boolean = false) : KeyValidator(
  {
    it.extractValueIfExists(key, String::class, nullable)
  },
  validator@ {
    if (nullable && it == null)
      return@validator

  it as String

  try {
    LocalDateTime.parse(it)
  } catch (exception: Exception) {
    throw AssertionError("Expected $key to be a LocalDateTime, but got $it")
  }
})