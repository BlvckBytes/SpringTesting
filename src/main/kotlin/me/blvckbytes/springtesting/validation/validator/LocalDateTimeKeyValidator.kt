package me.blvckbytes.springtesting.validation.validator

import me.blvckbytes.springtesting.validation.KeyValidator
import java.time.LocalDateTime

class LocalDateTimeKeyValidator(
  key: String,
  nullability: Nullability,
  validity: DateTimeValidity? = null,
) : KeyValidator(
  {
    it.extractValueIfExists(key, String::class, nullability.readNullable)
  },
  validator@ {
    if (nullability.assertNullabilityReturnIsNull(key, it))
      return@validator

    try {
      val stamp = LocalDateTime.parse(it as String)

      if (validity == null)
        return@validator

      val delta = validity.toleranceUnit.between(validity.targetStamp, stamp)

      if (validity.toleranceMode.checker(delta, validity.toleranceValue))
        return@validator

      throw AssertionError(
        "Expected value $stamp of key $key to be within " +
        "${validity.toleranceMode} ${validity.toleranceValue} ${validity.toleranceUnit} " +
        "of ${validity.targetStamp}, but delta was $delta"
      )
    } catch (exception: Exception) {
      throw AssertionError("Expected $key to be a LocalDateTime, but got $it")
    }
  }
)