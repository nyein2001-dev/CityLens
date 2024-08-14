package com.geo.tracking.ui.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geo.tracking.domain.GetLocationUseCase
import com.google.android.gms.location.LocationRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityVM @Inject constructor(
    private val getLocationUseCase: GetLocationUseCase
) : ViewModel() {

    private val _viewState: MutableStateFlow<ViewState> = MutableStateFlow(ViewState.Loading)
    val viewState = _viewState.asStateFlow()

    fun handle(event: PermissionEvent) {
        when (event) {
            PermissionEvent.Granted -> {
                viewModelScope.launch {
                    getLocationUseCase.invoke().collect {
                        _viewState.value = ViewState.Success(it)
                    }
                }
            }

            PermissionEvent.Revoked -> {
                _viewState.value = ViewState.RevokedPermissions
            }
        }
    }

    fun changeLocationSettings(distanceFilter: Float, accuracy: String) {
        createLocationRequest(distanceFilter, accuracy)
    }

    private fun createLocationRequest(distanceFilter: Float, accuracy: String): LocationRequest {
        val priority = if (accuracy == "low") {
            LocationRequest.PRIORITY_LOW_POWER
        } else {
            LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        return LocationRequest.Builder(1000)
            .setMinUpdateDistanceMeters(distanceFilter)
            .setPriority(priority)
            .build()
    }
}

sealed interface ViewState {
    data object Loading : ViewState
    data class Success(val location: Location?) : ViewState
    data object RevokedPermissions : ViewState
}

sealed interface PermissionEvent {
    data object Granted : PermissionEvent
    data object Revoked : PermissionEvent
}