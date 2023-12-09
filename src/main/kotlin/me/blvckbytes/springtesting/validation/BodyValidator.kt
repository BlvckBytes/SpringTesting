package me.blvckbytes.springtesting.validation

import me.blvckbytes.springcommon.exception.DescribedException
import me.blvckbytes.springtesting.http.HttpStatus
import me.blvckbytes.springtesting.validation.validator.LocalDateTimeKeyValidator
import me.blvckbytes.springtesting.validation.validator.Nullability
import me.blvckbytes.springtesting.validation.validator.UUIDKeyValidator
import org.json.JSONObject

class BodyValidator {

  companion object {
    fun makeBase(): BodyValidator {
      return BodyValidator()
        .add(UUIDKeyValidator("id", Nullability.NOT_NULL))
        .add(LocalDateTimeKeyValidator("createdAt", Nullability.NOT_NULL))
        .add(LocalDateTimeKeyValidator("updatedAt", Nullability.MAY_BE_NULL))
    }

    fun makeForError(status: HttpStatus, error: DescribedException): BodyValidator {
      return makeForError(status, error.getDescription())
    }

    fun makeForError(status: HttpStatus, message: String? = null): BodyValidator {
      val result = BodyValidator()
        .expectString("status", status.name)
        .add(LocalDateTimeKeyValidator("timestamp", Nullability.NOT_NULL))

      if (message != null)
        result.expectString("message", message)

      return result
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

  fun expectString(key: String, value: String?): BodyValidator {
    keyValidators.add(KeyValidator({
      it.extractValueIfExists(key, String::class)
    }){
      if (it != value)
        throwUnexpectedValueError(key, value, it)
    })
    return this
  }

  fun expectInt(key: String, value: Int?): BodyValidator {
    keyValidators.add(KeyValidator({
      it.extractValueIfExists(key, Int::class)
    }){
      if (it != value)
        throwUnexpectedValueError(key, value, it)
    })
    return this
  }

  fun expectArray(key: String, validators: List<BodyValidator>): BodyValidator {
    keyValidators.add(KeyValidator({
      it.extractObjectArray(key)
    }) { jsonObjects ->
      @Suppress("UNCHECKED_CAST")
      jsonObjects as List<JSONObject>

      if (jsonObjects.size != validators.size)
        throw AssertionError("Expected the array at key $key to be of length ${validators.size}")

      val remainingItems = mutableListOf<JsonObjectExtractor>()

      for (jsonObject in jsonObjects)
        remainingItems.add(JsonObjectExtractor(jsonObject))

      validatorLoop@ for (validator in validators) {
        for (remainingItemIndex in remainingItems.indices) {
          try {
            validator.apply(remainingItems[remainingItemIndex])
            remainingItems.removeAt(remainingItemIndex)
            continue@validatorLoop
          } catch (exception: AssertionError) {
            continue
          }
        }

        throw AssertionError("A validator did not find a match")
      }
    })

    return this
  }

  fun apply(response: JsonObjectExtractor): BodyValidator {
    if (response.body == null)
      throw AssertionError("Expected a body to be present")

    for (keyValidator in keyValidators)
      keyValidator.validator.validate(keyValidator.extractor(response))

    return this
  }

  fun copyOf(): BodyValidator {
    val copy = BodyValidator()
    copy.keyValidators.addAll(this.keyValidators)
    return copy
  }
}