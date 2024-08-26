package com.geo.tracking.data.models
import java.time.LocalDateTime

data class TripProgress(
    val tripId: String,
    val userId: String,
    val vehicleId: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val startLocation: LocationData,
    val currentLocation: LocationData,
    val destinationLocation: LocationData?,
    val locationHistory: List<LocationData>,
    val distanceTravelled: Double,
    val tripStatus: TripStatus,
    val timestamp: Long,
    val weatherConditions: String?,
    val trafficConditions: String?,
    val batteryLevel: Int?,
    val networkType: String?,
    val deviceOrientation: String?,
    val tripDuration: Long?,
    val busOccupancy: Int?,
    val predictedTravelTime: Long?,
    val predictedBusOccupancy: Int?,
    val predictedRoute: List<LocationData>?,
    val anomaliesDetected: List<Anomaly>?
)