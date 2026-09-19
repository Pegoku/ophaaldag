/*
 * Cure M3 - a Material 3 client for the Cure Afvalbeheer waste calendar.
 * Copyright (C) 2026 Pere Gomila
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.pegoku.curem3.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Turns a raw `postcodecheck` document into [CureData].
 *
 * The API wraps every section in `{response, data, error}` where `data` changes shape
 * between OK and NOK, so every section is decoded defensively and independently.
 */
object CureParser {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    class ApiException(message: String) : Exception(message)

    fun parse(raw: String, fetchedAt: Long = System.currentTimeMillis()): CureData {
        val root = json.parseToJsonElement(raw).jsonObject
        if (root.str("response") != "OK") {
            throw ApiException(root.str("error") ?: "Unknown API error")
        }
        val data = root["data"]?.jsonObject ?: throw ApiException("Missing data")

        val info = data["info"]?.let { decodeOrNull<AddressInfo>(it) } ?: AddressInfo()

        val pickups = decodeList<PickupDay>(data.sectionData("ophaaldagen")) +
            decodeList<PickupDay>(data.sectionData("ophaaldagenNext"))

        val wasteAbc: Map<String, List<String>> = buildMap {
            (data["afvalABC"] as? JsonObject)?.forEach { (k, v) ->
                val types = (v as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()
                if (types.isNotEmpty()) put(k, types)
            }
        }

        val customization = data["options"]?.jsonObject?.sectionData("customization")?.jsonObject
        val seed = customization?.get("colorBackground")?.jsonObject?.str("colorBackground")
            ?: customization?.str("customColor")

        val images = data.sectionData("gemeenteImages")?.jsonObject
        val logo = images?.str("gemeenteLogoV2")?.takeIf { it.startsWith("http") }
            ?: images?.str("gemeenteLogo")?.takeIf { it.startsWith("http") }

        return CureData(
            info = info,
            gemeente = data.str("gemeente") ?: info.gemeenteName,
            logoUrl = logo,
            pickups = pickups.filter { it.localDate != null }.distinctBy { it.type + it.date }.sortedBy { it.date },
            wasteTypes = (data["afvalABCWasteTypes"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList(),
            wasteAbc = wasteAbc,
            separation = decodeList(data.sectionData("scheidingsinfo")),
            announcements = decodeList<Announcement>(data.sectionData("mededelingen"))
                .sortedBy { it.position.toIntOrNull() ?: Int.MAX_VALUE },
            pushMessages = decodeList(data.sectionData("pushData")),
            tips = decodeList(data.sectionData("tips")),
            municipalityPages = decodeList(data.sectionData("uwgemeente")),
            moreInfoHtml = (data.sectionData("meerweten") as? JsonPrimitive)?.contentOrNull,
            faqHtml = (data.sectionData("overafval") as? JsonPrimitive)?.contentOrNull,
            containers = decodeList<ContainerLocation>(data.sectionData("postcodeContainers")).filter { it.lat != null && it.lon != null },
            seedColor = seed?.removePrefix("#")?.toLongOrNull(16)?.let { 0xFF000000L or it },
            fetchedAt = fetchedAt,
        )
    }

    /** Returns the `data` element of an envelope section if its `response` is OK. */
    private fun JsonObject.sectionData(name: String): JsonElement? {
        val section = this[name] as? JsonObject ?: return null
        if (section.str("response") != "OK") return null
        return section["data"]
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull

    private inline fun <reified T> decodeOrNull(element: JsonElement): T? =
        runCatching { json.decodeFromJsonElement<T>(kotlinx.serialization.serializer(), element) }.getOrNull()

    private inline fun <reified T> decodeList(element: JsonElement?): List<T> {
        val array = element as? JsonArray ?: return emptyList()
        return array.mapNotNull { decodeOrNull<T>(it) }
    }
}
