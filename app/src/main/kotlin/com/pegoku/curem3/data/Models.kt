package com.pegoku.curem3.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** The address the user selected. Postcode is normalised (no spaces, upper-case). */
data class Address(
    val postcode: String,
    val houseNumber: String,
    val suffix: String = "",
    val street: String = "",
) {
    val display: String get() = buildString {
        append(street.ifBlank { postcode })
        append(' ').append(houseNumber)
        if (suffix.isNotBlank()) append(' ').append(suffix)
    }
}

@Serializable
data class PickupDay(
    val nameType: String = "",
    val type: String = "",
    val date: String = "",
) {
    val localDate: LocalDate? get() = runCatching { LocalDate.parse(date) }.getOrNull()
}

@Serializable
data class AddressInfo(
    val postcode: String = "",
    val huisnummer: String = "",
    val letter: String = "",
    val straat: String = "",
    val plaats: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val gemeenteName: String = "",
    val afvaldataVersion: String = "",
    val contentVersion: String = "",
) {
    val lat: Double? get() = latitude.toDoubleOrNull()
    val lon: Double? get() = longitude.toDoubleOrNull()
    val fullAddress: String get() = "$straat $huisnummer${if (letter.isNotBlank()) " $letter" else ""}"
    val cityLine: String get() = "${postcode.take(4)} ${postcode.drop(4)} $plaats".trim()
}

@Serializable
data class SeparationInfo(
    val iconName: String = "",
    val afvalTitle: String = "",
    val afvalName: String = "",
    val text: String = "",
    @SerialName("push_notification_message") val pushMessage: String = "",
)

@Serializable
data class Announcement(
    val id: String = "",
    val position: String = "",
    val title: String = "",
    val description: String = "",
    val text: String = "",
    @SerialName("start_date") val startDate: String = "",
    val date: String = "",
    @SerialName("expiration_date") val expirationDate: String = "",
    val topImage: String = "",
) {
    fun isActive(today: LocalDate): Boolean {
        val exp = runCatching { LocalDate.parse(expirationDate) }.getOrNull() ?: return true
        return !exp.isBefore(today)
    }
}

@Serializable
data class PushMessage(val date: String = "", val message: String = "")

@Serializable
data class Tip(
    val title: String = "",
    val content: String = "",
    @SerialName("start_date") val startDate: String = "",
    @SerialName("expiration_date") val expirationDate: String = "",
)

@Serializable
data class MunicipalityPage(
    val buttonTitle: String = "",
    val title: String = "",
    val text: String = "",
)

@Serializable
data class ContainerLocation(
    @SerialName("WasteType") val wasteType: String = "",
    val address: String = "",
    val city: String = "",
    val latitude: String = "",
    val longitude: String = "",
    @SerialName("OcNumber") val number: String = "",
) {
    val lat: Double? get() = latitude.toDoubleOrNull()
    val lon: Double? get() = longitude.toDoubleOrNull()
}

@Serializable
data class Language(val name: String = "", val lang: String = "")

/** Everything the app needs from one `postcodecheck` document. */
data class CureData(
    val info: AddressInfo,
    val gemeente: String,
    val logoUrl: String?,
    val pickups: List<PickupDay>,
    val wasteTypes: List<String>,
    val wasteAbc: Map<String, List<String>>,
    val separation: List<SeparationInfo>,
    val announcements: List<Announcement>,
    val pushMessages: List<PushMessage>,
    val tips: List<Tip>,
    val municipalityPages: List<MunicipalityPage>,
    val moreInfoHtml: String?,
    val faqHtml: String?,
    val containers: List<ContainerLocation>,
    val seedColor: Long?,
    val fetchedAt: Long,
) {
    fun separationFor(type: String): SeparationInfo? =
        separation.firstOrNull { it.iconName.equals(type, ignoreCase = true) }

    fun labelFor(type: String): String =
        separationFor(type)?.afvalTitle
            ?: pickups.firstOrNull { it.type == type }?.nameType?.replaceFirstChar { it.uppercase() }
            ?: type
}
