/*
 * Ophaaldag - a Material 3 client for the Cure Afvalbeheer waste calendar.
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
package com.pegoku.ophaaldag.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin client for the MijnAfvalwijzer "appsinput" web service used by the Cure app.
 * See docs/API.md for the reverse-engineered contract.
 */
class CureApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    enum class TinyResult { OK, NO_DATA, UNKNOWN_POSTCODE }

    private fun base(method: String): HttpUrl.Builder =
        BASE.toHttpUrl().newBuilder()
            .addQueryParameter("apikey", API_KEY)
            .addQueryParameter("method", method)

    private fun HttpUrl.Builder.address(a: Address): HttpUrl.Builder = this
        .addQueryParameter("postcode", a.postcode)
        .addQueryParameter("street", a.street)
        .addQueryParameter("huisnummer", a.houseNumber)
        .addQueryParameter("toevoeging", a.suffix)

    private suspend fun get(url: HttpUrl): String = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(url).header("User-Agent", USER_AGENT).build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            resp.body.string()
        }
    }

    suspend fun streetList(postcode: String): List<String> {
        val body = get(base("streetList").addQueryParameter("postcode", postcode).build())
        val root = CureParser.json.parseToJsonElement(body).jsonObject
        return (root["data"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull } ?: emptyList()
    }

    suspend fun languages(postcode: String): List<Language> {
        val body = get(base("languagesList").addQueryParameter("postcode", postcode).build())
        val root = CureParser.json.parseToJsonElement(body).jsonObject
        return (root["data"] as? JsonArray)?.mapNotNull {
            runCatching { CureParser.json.decodeFromJsonElement(Language.serializer(), it) }.getOrNull()
        } ?: emptyList()
    }

    suspend fun tinyCheck(address: Address): TinyResult {
        val body = get(base("tinyPostcodeCheck").address(address).addQueryParameter("app_name", APP_NAME).build())
        return when {
            body.trim().startsWith("OK") -> TinyResult.OK
            body.trim().startsWith("NOK") -> TinyResult.NO_DATA
            else -> TinyResult.UNKNOWN_POSTCODE
        }
    }

    /** Full document as raw JSON. Throws [CureParser.ApiException] when the API answers NOK. */
    suspend fun postcodeCheck(address: Address, lang: String): String {
        val url = base("postcodecheck").address(address)
            .addQueryParameter("platform", "phone")
            .addQueryParameter("langs", lang)
            .addQueryParameter("mobiletype", "android")
            .addQueryParameter("version", UPSTREAM_VERSION)
            .addQueryParameter("app_name", APP_NAME)
            .build()
        val body = get(url)
        if (body.startsWith("NOK")) throw CureParser.ApiException("No data for this address")
        return body
    }

    companion object {
        const val BASE = "https://api.mijnafvalwijzer.nl/webservices/appsinput/"
        const val API_KEY = "5ef443e778f41c4f75c69459eea6e6ae0c2d92de729aa0fc61653815fbd6a8ca"
        const val APP_NAME = "cure"
        const val UPSTREAM_VERSION = "5.40"
        const val USER_AGENT = "ophaaldag/1.0 (Android)"
    }
}
