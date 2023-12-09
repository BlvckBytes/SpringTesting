package me.blvckbytes.springtesting.validation.validator

enum class ToleranceMode(
  val checker: (deviation: Long, tolerance: Long) -> Boolean
) {
  PLUS({ deviation, tolerance ->
    deviation in 0..tolerance
  }),
  MINUS({ deviation, tolerance ->
    deviation in 0 downTo -tolerance
  }),
  PLUS_MINUS({ deviation, tolerance ->
    deviation in tolerance downTo -tolerance
  })
  ;
}