package com.geo.tracking.domain

import android.location.Location
import com.geo.tracking.data.ILocationService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLocationUseCase @Inject constructor(
    private val locationService: ILocationService
) {
    operator fun invoke(): Flow<Location?> = locationService.requestLocationUpdates()
}