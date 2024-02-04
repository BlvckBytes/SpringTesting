package me.blvckbytes.springtesting

import me.blvckbytes.springtesting.validation.EnumGenerationState
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.pow
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.typeOf

class GenerationMethodInvoker<T : Any>(
  private val creationMethod: KFunction<T>,
) {

  init {
    creationMethod.isAccessible = true
  }

  private val stringChars = (32..126).mapNotNull {
    if (it == '\\'.code) null else it.toChar()
  }

  private val random = ThreadLocalRandom.current()
  private val generatedValues = mutableMapOf<KType, MutableSet<Any>>()
  private val enumGenerationStates = mutableMapOf<Class<*>, EnumGenerationState>()

  fun callForAllNullCombinations(
    minNumberOfResults: Int? = null,
    invocationParameters: List<Any>? = null,
  ): List<T> {
    val result = mutableListOf<T>()

    val parameters = creationMethod.parameters
    val nullableParameterIndices = parameters.mapIndexedNotNull { index, parameter ->
      if (parameter.type.isMarkedNullable) index else null
    }

    var nullMask = 0

    for (permutationIndex in 0 until 2.0.pow(nullableParameterIndices.size).toInt()) {
      val parameterValues = arrayOfNulls<Any>(parameters.size)
      var invocationParametersIndex = 0

      for (parameterIndex in parameters.indices) {
        val parameter = parameters[parameterIndex]
        val nullMaskBitIndex = nullableParameterIndices.indexOf(parameterIndex)

        if (parameter.hasAnnotation<InvokerParam>()) {
          parameterValues[parameterIndex] = (
            if (invocationParameters == null)
              null
            else
              invocationParameters[invocationParametersIndex++]
          )
          continue
        }

        if (nullMaskBitIndex < 0 || nullMask and (1 shl nullMaskBitIndex) == 0) {
          parameterValues[parameterIndex] = generateValue(parameter.type)
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
          result.add(callRandomized(invocationParameters))
      }
    }

    return result
  }

  fun callRandomized(invocationParameters: List<Any>? = null): T {
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

      parameterValues[parameterIndex] = generateValue(parameter.type)
    }

    return creationMethod.call(*parameterValues)
  }

  private fun generateRandomAsciiString(length: Int): String {
    if (length == 0)
      throw IllegalStateException("Length cannot be zero")

    val result = StringBuilder()

    while (result.isBlank()) {
      result.clear()

      for (i in 0 until length)
        result.append(stringChars.random())
    }

    return result.toString()
  }

  private fun tryLoadClass(type: KType): Class<*>? {
    return try {
      var typeString = type.toString()

      if (typeString.endsWith('?'))
        typeString = typeString.substring(0, typeString.length - 1)

      Class.forName(typeString)
    } catch (exception: ClassNotFoundException) {
      null
    }
  }

  private fun generateValue(type: KType): Any {
    val typeClass = tryLoadClass(type)

    if (typeClass?.isEnum == true) {
      return enumGenerationStates.computeIfAbsent(typeClass) {
        EnumGenerationState(typeClass)
      }.next()
    }

    return generateUniqueRandomValue(type)
  }

  private fun generateUniqueRandomValue(type: KType): Any {
    var generatedValue: Any

    do {
      generatedValue = when (type) {
        typeOf<String>(),
        typeOf<String?>() -> generateRandomAsciiString(random.nextInt(128) + 1)
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