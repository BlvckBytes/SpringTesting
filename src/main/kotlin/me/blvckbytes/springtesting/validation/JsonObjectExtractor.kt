package me.blvckbytes.springtesting.validation

import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.cast

open class JsonObjectExtractor(
  val body: JSONObject?
) {
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

    else if (!type.isInstance(currentNode))
      throw AssertionError("Expected type ${type.simpleName} at path $path, but found $currentNode")

    return type.cast(currentNode)
  }

  fun <T> extractValue(path: String, type: KClass<T>): T where T : Any {
    return extractValueIfExists(path, type, false)!!
  }
}