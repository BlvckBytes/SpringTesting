package me.blvckbytes.springtesting.http

enum class HttpStatus(
  val code: Int
) {
  OK(200),
  NO_CONTENT(204),
  NOT_FOUND(404),
}