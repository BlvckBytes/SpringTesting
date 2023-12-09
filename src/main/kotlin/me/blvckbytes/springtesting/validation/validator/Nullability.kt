package me.blvckbytes.springtesting.validation.validator

enum class Nullability(
  val readNullable: Boolean
) {
  MAY_BE_NULL(true),
  NOT_NULL(false),
  IS_NULL(true)
  ;

  fun assertNullabilityReturnIsNull(key: String, value: Any?): Boolean {
    if (this == NOT_NULL && value == null)
      throw AssertionError("Key $key had nullability of $this, but was null")

    if (value != null) {
      if (this == IS_NULL)
        throw AssertionError("Key $key had nullability of $this, but was not null: $value")

      return false
    }

    return true
  }
}