package com.geo.tracking.data.models

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracy: Float?,
    val bearing: Float?,
    val speed: Float?,
    val verticalAccuracy: Float?,
    val provider: String?,
    val time: Long,
    val elapsedTime: Long,
    val isMock: Boolean?,
    val heading: Double?,
    val altitudeAccuracy: Float?,
    val satelliteCount: Int?,
    val isInTunnel: Boolean?,
    val isAboveGround: Boolean?,
    val placeMarkList: List<PlaceMark>
)






