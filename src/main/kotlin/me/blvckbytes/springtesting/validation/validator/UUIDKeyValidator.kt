package me.blvckbytes.springtesting.validation.validator

import me.blvckbytes.springtesting.validation.KeyValidator
import java.util.*

class UUIDKeyValidator(key: String, nullability: Nullability) : KeyValidator(
  {
    it.extractValueIfExists(key, String::class, nullability.readNullable)
  },
  validator@ {
    if (nullability.assertNullabilityReturnIsNull(key, it))
      return@validator

    try {
      UUID.fromString(it as String)
    } catch (exception: Exception) {
      throw AssertionError("Expected $key to be a UUID, but got $it")
    }
  }
)