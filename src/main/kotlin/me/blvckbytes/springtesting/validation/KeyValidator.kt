package me.blvckbytes.springtesting.validation

open class KeyValidator(
  val key: String,
  val validator: ValidatorFunction
)