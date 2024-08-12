package com.geo.tracking.domain

import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import com.geo.tracking.data.ILocationService
import kotlinx.coroutines.flow.Flow
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

class GetLocationUseCase @Inject constructor(
    private val locationService: ILocationService
) {
    @RequiresApi(Build.VERSION_CODES.S)
    operator fun invoke(): Flow<Location?> = locationService.requestLocationUpdates()
}