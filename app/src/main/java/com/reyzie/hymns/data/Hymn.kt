package com.reyzie.hymns.data

object AppConstants {
    const val HYMNS_DATA_URL = "https://raw.githubusercontent.com/Reynold29/csi-hymns-vault/main/hymns_data.json"
    const val KEERTHANE_DATA_URL = "https://raw.githubusercontent.com/Reynold29/csi-hymns-vault/main/keerthane_data.json"
    const val ORDER_OF_SERVICE_DATA_URL =
        "https://raw.githubusercontent.com/Reynold29/csi-hymns-vault/refs/heads/main/order-of-service_data.json"
}

data class Hymn(
    val number: Int,
    val title: String,
    val signature: String,
    val lyrics: String,
    val kannadaLyrics: String? = null
)
