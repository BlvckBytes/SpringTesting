package me.blvckbytes.springtesting.validation

fun interface ValidatorFunction {
  fun validate(value: Any?)
}