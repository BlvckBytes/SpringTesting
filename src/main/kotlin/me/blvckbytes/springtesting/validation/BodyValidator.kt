package me.blvckbytes.springtesting.validation

import me.blvckbytes.springcommon.exception.DescribedException
import me.blvckbytes.springtesting.http.HttpStatus
import me.blvckbytes.springtesting.validation.validator.LocalDateTimeKeyValidator
import me.blvckbytes.springtesting.validation.validator.UUIDKeyValidator
import org.json.JSONObject
import java.util.*
import kotlin.reflect.KClass

class BodyValidator {

  companion object {
    fun makeBase(): BodyValidator {
      return BodyValidator()
        .add(UUIDKeyValidator("id"))
        .add(LocalDateTimeKeyValidator("createdAt"))
        .add(LocalDateTimeKeyValidator("updatedAt", true))
    }

    fun makeForError(status: HttpStatus, error: DescribedException): BodyValidator {
      return makeForError(status, error.getDescription())
    }

    fun makeForError(status: HttpStatus, message: String? = null): BodyValidator {
      val result = BodyValidator()
        .expectString("status", status.name)
        .add(LocalDateTimeKeyValidator("timestamp"))

      if (message != null)
        result.expectString("message", message)

      return result
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> checkTypeAndOtherwiseThrow(key: String, expectedType: KClass<T>, value: Any?, handler: (value: T?) -> Unit) where T : Any {
      if (value == null || value.equals(null))
        return handler(null)

      if (!expectedType.isInstance(value))
        throw AssertionError("Expected property $key to be a ${expectedType.simpleName}, but got ${value?.javaClass?.simpleName}")

      handler(value as T)
    }

    private fun throwUnexpectedValueError(key: String, expected: Any?, actual: Any?) {
      throw AssertionError("Expected $key to be equal to $expected, but got $actual")
    }
  }

  private val keyValidators = mutableListOf<KeyValidator>()

  fun add(keyValidator: KeyValidator): BodyValidator {
    keyValidators.add(keyValidator)
    return this
  }

  fun validateString(key: String, validator: (value: String?) -> Unit): BodyValidator {
    keyValidators.add(KeyValidator(key) {
      checkTypeAndOtherwiseThrow(key, String::class, it, validator)
    })
    return this
  }

  fun validateInt(key: String, validator: (value: Int?) -> Unit): BodyValidator {
    keyValidators.add(KeyValidator(key) {
      checkTypeAndOtherwiseThrow(key, Int::class, it, validator)
    })
    return this
  }

  fun expectString(key: String, value: String?): BodyValidator {
    return validateString(key) {
      if (it != value)
        throwUnexpectedValueError(key, value, it)
    }
  }

  fun expectInt(key: String, value: Int?): BodyValidator {
    return validateInt(key) {
      if (it != value)
        throwUnexpectedValueError(key, value, it)
    }
  }

  fun apply(body: JSONObject?): BodyValidator {
    if (body == null)
      throw AssertionError("Expected a body to be present")

    for (keyValidator in keyValidators)
      keyValidator.validator.validate(body.get(keyValidator.key))

    return this
  }
}