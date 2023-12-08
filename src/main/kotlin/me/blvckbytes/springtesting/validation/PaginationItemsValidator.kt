package me.blvckbytes.springtesting.validation

import me.blvckbytes.springtesting.http.HttpClient
import me.blvckbytes.springtesting.http.MultiValueStringMapBuilder
import org.json.JSONArray
import org.json.JSONObject

object PaginationItemsValidator {

  fun ensureItemsExistence(path: String, pageSize: Int, itemValidators: List<BodyValidator>) {
    val paginationMap = buildBasePaginationMap(pageSize)
    var totalItems: Int? = null
    var seenItems = 0
    var selectedPage = 1

    val remainingValidators = itemValidators.toMutableList()

    do {
      val response = HttpClient.performStandardGetRequest(path, requestParams = paginationMap)

      val cursorTotalItems = response.extractValue("cursor.totalItems", Int::class)

      if (totalItems == null)
        totalItems = cursorTotalItems
      else if (totalItems != cursorTotalItems)
        throw AssertionError("Total item count changed while paginating! $totalItems -> $cursorTotalItems")

      val itemsList = response.extractValue("items", JSONArray::class)
      val numberOfItems = itemsList.length()

      itemValidatorLoop@ for (i in 0 until numberOfItems) {
        val currentItem = itemsList.get(i)

        if (currentItem !is JSONObject)
          throw AssertionError("Expected item $i to be an object, but got a ${itemsList.javaClass.simpleName}")

        for (remainingValidatorIndex in remainingValidators.indices) {
          val validator = remainingValidators[remainingValidatorIndex]
          try {
            validator.apply(currentItem)
            remainingValidators.removeAt(remainingValidatorIndex)
            continue@itemValidatorLoop
          } catch (exception: AssertionError) {
            continue
          }
        }

        throw AssertionError("An item didn't match on any remaining validator: $currentItem")
      }

      seenItems += numberOfItems
      paginationMap.override("selectedPage", ++selectedPage)
    } while (seenItems < totalItems!!)

    if (seenItems != totalItems)
      throw AssertionError("Expected to see totalItems=$totalItems, but found $seenItems")

    if (remainingValidators.size > 0)
      throw AssertionError("Expected all item validators to find a match, but ${remainingValidators.size} remained unmatched")
  }

  private fun buildBasePaginationMap(pageSize: Int): MultiValueStringMapBuilder {
    val result = MultiValueStringMapBuilder()

    result.add("pageSize", pageSize)

    // Required to ensure a fixed item order while paging
    result.add("sorting", "+id")

    return result
  }
}