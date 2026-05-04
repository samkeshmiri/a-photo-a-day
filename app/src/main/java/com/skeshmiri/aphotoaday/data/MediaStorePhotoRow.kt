package com.skeshmiri.aphotoaday.data

data class MediaStorePhotoRow(
    val id: Long,
    val displayName: String,
    val dateTakenMillis: Long?,
    val dateAddedSeconds: Long?,
    val width: Int,
    val height: Int,
)

