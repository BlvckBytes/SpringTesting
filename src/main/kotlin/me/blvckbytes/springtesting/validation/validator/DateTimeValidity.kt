package me.blvckbytes.springtesting.validation.validator

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class DateTimeValidity(
  val targetStamp: LocalDateTime,
  val toleranceValue: Long,
  val toleranceUnit: ChronoUnit,
  val toleranceMode: ToleranceMode = ToleranceMode.PLUS,
) {
  companion object {
    fun defaultNow(): DateTimeValidity {
      return DateTimeValidity(
        LocalDateTime.now(),
        500,
        ChronoUnit.MILLIS
      )
    }
  }
}