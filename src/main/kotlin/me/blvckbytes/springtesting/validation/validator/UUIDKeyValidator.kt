package me.blvckbytes.springtesting.validation.validator

import me.blvckbytes.springtesting.validation.KeyValidator
import java.util.*

class UUIDKeyValidator(key: String, nullable: Boolean = false) : KeyValidator(
  {
    it.extractValueIfExists(key, String::class, nullable)
  },
  validator@ {
    if (nullable && it == null)
      return@validator

    it as String

    try {
      UUID.fromString(it)
    } catch (exception: Exception) {
      throw AssertionError("Expected $key to be a UUID, but got $it")
    }
  }
)