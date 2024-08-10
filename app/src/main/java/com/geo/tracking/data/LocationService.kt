package com.geo.tracking.data

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.flow.Flow
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

class LocationService @Inject constructor(
    private val context: Context,
    private val locationClient: FusedLocationProviderClient
) : ILocationService {
    override fun requestLocationUpdates(): Flow<GeoPoint?> {
        TODO("Not yet implemented")
    }

    override fun requestCurrentLocation(): Flow<GeoPoint?> {
        TODO("Not yet implemented")
    }
}