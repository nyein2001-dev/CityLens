package com.geo.tracking.data

import kotlinx.coroutines.flow.Flow
import org.osmdroid.util.GeoPoint

interface ILocationService {
    fun requestLocationUpdates(): Flow<GeoPoint?>

    fun requestCurrentLocation(): Flow<GeoPoint?>
}