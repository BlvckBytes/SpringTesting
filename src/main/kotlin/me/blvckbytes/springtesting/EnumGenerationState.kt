package me.blvckbytes.springtesting

class EnumGenerationState(
  enumClass: Class<*>
) {
  private val constants = enumClass.enumConstants
  private val numberOfConstants = constants.size
  private var nextConstantIndex = 0

  fun next(): Any {
    if (nextConstantIndex == numberOfConstants)
      nextConstantIndex = 0

    return constants[nextConstantIndex++]
  }
}