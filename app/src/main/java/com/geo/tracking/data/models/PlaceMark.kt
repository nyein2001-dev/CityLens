package com.geo.tracking.data.models

data class PlaceMark(
    val countryName: String? = null,
    val countryCode: String? = null,
    val adminArea: String? = null,
    val subAdminArea: String? = null,
    val locality: String? = null,
    val subLocality: String? = null,
    val thoroughfare: String? = null,
    val subThoroughfare: String? = null,
    val postalCode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)