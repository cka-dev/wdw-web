package net.winedownwednesday.web.data.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer
import net.winedownwednesday.web.data.Member

/**
 * Handles backward compatibility for the memberSpotlight field.
 *
 * The server may return either:
 * - v2 format: `[Member, Member, ...]` (array)
 * - Legacy format: `{...}` (single Member object)
 * - Legacy format: `null`
 *
 * This serializer wraps single objects in an array and
 * converts null to an empty array so the Kotlin type is
 * always `List<Member>`.
 */
object MemberSpotlightListSerializer :
    JsonTransformingSerializer<List<Member>>(
        ListSerializer(Member.serializer())
    ) {
    override fun transformDeserialize(
        element: JsonElement
    ): JsonElement {
        return when (element) {
            is JsonArray -> element
            is JsonObject -> JsonArray(listOf(element))
            else -> JsonArray(emptyList())
        }
    }
}
