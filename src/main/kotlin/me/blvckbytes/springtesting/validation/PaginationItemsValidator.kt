package me.blvckbytes.springtesting.validation

import me.blvckbytes.springtesting.http.HttpClient
import me.blvckbytes.springtesting.http.MultiValueStringMapBuilder

object PaginationItemsValidator {

  fun ensureItemsExistence(
    path: String,
    pageSize: Int,
    itemValidators: List<BodyValidator>,
    fixedOrderSortingKeys: List<String>,
  ) {
    val paginationMap = buildBasePaginationMap(pageSize, fixedOrderSortingKeys)
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

      val jsonItems = response.extractObjectArray("items").map { JsonObjectExtractor(it) }

      itemValidatorLoop@ for (currentItem in jsonItems) {
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

        throw AssertionError("An item didn't match on any remaining validator (${remainingValidators.size}): ${currentItem.body?.toString(2)}")
      }

      seenItems += jsonItems.size
      paginationMap.override("selectedPage", ++selectedPage)
    } while (seenItems < totalItems!!)

    if (seenItems != totalItems)
      throw AssertionError("Expected to see totalItems=$totalItems, but found $seenItems")

    if (remainingValidators.size > 0)
      throw AssertionError("Expected all item validators to find a match, but ${remainingValidators.size} remained unmatched")
  }

  private fun buildBasePaginationMap(
    pageSize: Int,
    fixedOrderSortingKeys: List<String>,
  ): MultiValueStringMapBuilder {
    val result = MultiValueStringMapBuilder()

    result.add("pageSize", pageSize)

    // Required to ensure a fixed item order while paging
    result.add("sorting", fixedOrderSortingKeys.joinToString(",") { "+$it" })

    return result
  }
}