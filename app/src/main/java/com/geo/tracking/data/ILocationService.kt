package com.geo.tracking.data

import android.location.Location
import kotlinx.coroutines.flow.Flow
import org.osmdroid.util.GeoPoint

interface ILocationService {
    fun requestLocationUpdates(): Flow<Location?>

    fun requestCurrentLocation(): Flow<Location?>
}