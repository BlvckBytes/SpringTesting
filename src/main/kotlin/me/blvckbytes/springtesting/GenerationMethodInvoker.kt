package me.blvckbytes.springtesting

import me.blvckbytes.springtesting.validation.CreationResult
import org.testcontainers.shaded.com.google.common.math.IntMath.pow
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.typeOf

class GenerationMethodInvoker(
  private val creationMethod: KFunction<CreationResult>,
  private val invocationParameters: List<Any>? = null
) {

  init {
    creationMethod.isAccessible = true
  }

  private val stringChars = (32..126).mapNotNull {
    if (it == '\\'.code) null else it.toChar()
  }

  private val random = ThreadLocalRandom.current()
  private val generatedValues = mutableMapOf<KType, MutableSet<Any>>()

  fun callForAllNullCombinations(minNumberOfResults: Int? = null): List<CreationResult> {
    val result = mutableListOf<CreationResult>()

    val parameters = creationMethod.parameters
    val nullableParameterIndices = parameters.mapIndexedNotNull { index, parameter ->
      if (parameter.type.isMarkedNullable) index else null
    }

    var nullMask = 0

    for (permutationIndex in 0 until pow(2, nullableParameterIndices.size)) {
      val parameterValues = arrayOfNulls<Any>(parameters.size)
      var invocationParametersIndex = 0

      for (parameterIndex in parameters.indices) {
        val parameter = parameters[parameterIndex]
        val nullMaskBitIndex = nullableParameterIndices.indexOf(parameterIndex)

        if (nullMaskBitIndex < 0 || nullMask and (1 shl nullMaskBitIndex) == 0) {
          if (parameter.hasAnnotation<InvokerParam>()) {
            parameterValues[parameterIndex] = (
              if (invocationParameters == null)
                null
              else
                invocationParameters[invocationParametersIndex++]
            )
            continue
          }

          parameterValues[parameterIndex] = generateUniqueRandomValue(parameter.type)
          continue
        }

        if (!parameter.type.isMarkedNullable)
          throw IllegalStateException("Something went wrong... The current parameter is not nullable")

        parameterValues[parameterIndex] = null
      }

      result.add(creationMethod.call(*parameterValues))
      ++nullMask
    }

    if (minNumberOfResults != null) {
      val missingItems = minNumberOfResults - result.size

      if (missingItems > 0) {
        for (i in 0 until missingItems)
          result.add(callRandomized())
      }
    }

    return result
  }

  fun callRandomized(): CreationResult {
    val parameters = creationMethod.parameters
    val parameterValues = arrayOfNulls<Any>(parameters.size)
    var invocationParametersIndex = 0

    for (parameterIndex in creationMethod.parameters.indices) {
      val parameter = parameters[parameterIndex]

      if (parameter.hasAnnotation<InvokerParam>()) {
        parameterValues[parameterIndex] = (
          if (invocationParameters == null)
            null
          else
            invocationParameters[invocationParametersIndex++]
          )
        continue
      }

      parameterValues[parameterIndex] = generateUniqueRandomValue(parameter.type)
    }

    return creationMethod.call(*parameterValues)
  }

  private fun generateRandomAsciiString(length: Int): String {
    val result = StringBuilder()

    while (result.isBlank()) {
      result.clear()

      for (i in 0 until length)
        result.append(stringChars.random())
    }

    return result.toString()
  }

  private fun generateUniqueRandomValue(type: KType): Any {
    var generatedValue: Any

    do {
      generatedValue = when (type) {
        typeOf<String>(),
        typeOf<String?>() -> generateRandomAsciiString(random.nextInt(128 + 1))
        typeOf<Int>(),
        typeOf<Int?>() -> ThreadLocalRandom.current().nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE)
        typeOf<UUID>(),
        typeOf<UUID?>()-> UUID.randomUUID()
        else -> throw IllegalStateException("No generator for type $type implemented yet")
      }
    } while (!generatedValues.computeIfAbsent(type) { mutableSetOf() }.add(generatedValue))

    return generatedValue
  }
}