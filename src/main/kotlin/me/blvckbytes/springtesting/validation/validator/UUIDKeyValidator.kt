package me.blvckbytes.springtesting.validation.validator

import me.blvckbytes.springtesting.validation.BodyValidator
import me.blvckbytes.springtesting.validation.KeyValidator
import java.util.*

class UUIDKeyValidator(key: String, nullable: Boolean = false) : KeyValidator(key, {
  BodyValidator.checkTypeAndOtherwiseThrow(key, String::class, it) { stringValue ->
    if (nullable && stringValue == null)
      return@checkTypeAndOtherwiseThrow

    try {
      UUID.fromString(stringValue)
    } catch (exception: Exception) {
      throw AssertionError("Expected $key to be a UUID, but got $stringValue")
    }
  }
})