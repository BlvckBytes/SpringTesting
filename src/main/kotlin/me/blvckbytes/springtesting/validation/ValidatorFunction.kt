package me.blvckbytes.springtesting.validation

fun interface ValidatorFunction<T> {
  fun validate(value: T?)
}