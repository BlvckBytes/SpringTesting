package me.blvckbytes.springtesting.validation

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.cast

open class JsonObjectExtractor(
  val body: JSONObject?
) {

  fun <T : Any> extractTypedArray(path: String, type: KClass<T>): List<T?> {
    val jsonArray = extractValue(path, JSONArray::class)
    val arrayItems = mutableListOf<T?>()

    for (itemIndex in 0 until jsonArray.length()) {
      val item = jsonArray[itemIndex]

      if (item == JSONObject.NULL) {
        arrayItems.add(null)
        continue
      }

      if (!type.isInstance(item))
        throw AssertionError("Expected item $itemIndex to be of type ${type.simpleName}, but got a ${item.javaClass.simpleName}")

      arrayItems.add(type.cast(item))
    }

    return arrayItems
  }

  fun extractAnyArray(path: String): List<Any> {
    return extractValue(path, JSONArray::class).toList()
  }

  fun extractObjectArray(path: String): List<JSONObject> {
    val jsonArray = extractValue(path, JSONArray::class)
    val arrayItems = mutableListOf<JSONObject>()

    for (itemIndex in 0 until jsonArray.length()) {
      val item = jsonArray[itemIndex]

      if (item !is JSONObject)
        throw AssertionError("Expected item $itemIndex to be an object, but got a ${item.javaClass.simpleName}")

      arrayItems.add(item)
    }

    return arrayItems
  }

  fun <T> extractValueIfExists(path: String, type: KClass<T>, allowNull: Boolean = true): T? where T : Any {
    if (body == null)
      throw AssertionError("Expected a body to be present")

    val pathParts = path.split(".")
    var currentNode: Any? = body

    for (pathPart in pathParts) {
      if (currentNode is JSONObject) {

        if (!currentNode.has(pathPart)) {
          currentNode = null
          continue
        }

        currentNode = currentNode.get(pathPart)

        if (currentNode == JSONObject.NULL)
          currentNode = null

        continue
      }

      if (currentNode is JSONArray) {
        currentNode = currentNode.get(
          pathPart.toIntOrNull() ?: throw IllegalStateException("Cannot use $pathPart to index an array")
        )
        continue
      }

      if (currentNode == null)
        throw AssertionError("Path part $pathPart could not be resolved, because it's predecessor was null ($path)")

      throw IllegalStateException("Don't know how to access a node of type ${currentNode.javaClass.simpleName}")
    }

    if (currentNode == null) {
      if (!allowNull)
        throw AssertionError("Expected value at path $path to be non-null")

      return null
    }

    if (type == UUID::class && currentNode is String)
      return type.cast(UUID.fromString(currentNode))

    if (currentNode is BigDecimal) {
      if (type == Float::class)
        return type.cast(currentNode.toFloat())

      if (type == Double::class)
        return type.cast(currentNode.toDouble())
    }

    else if (!type.isInstance(currentNode))
      throw AssertionError("Expected type ${type.simpleName} at path $path, but found $currentNode (${currentNode.javaClass.simpleName})")

    return type.cast(currentNode)
  }

  fun <T> extractValue(path: String, type: KClass<T>): T where T : Any {
    return extractValueIfExists(path, type, false)!!
  }

  override fun toString(): String {
    return body?.toString(2) ?: "null"
  }
}