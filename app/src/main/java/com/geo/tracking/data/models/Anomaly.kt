package com.geo.tracking.data.models

data class Anomaly(
    val anomalyType: AnomalyType,
    val description: String,
    val location: LocationData,
    val timestamp: Long
)