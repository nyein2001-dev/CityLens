package com.geo.tracking

import android.Manifest
import android.app.Activity
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.geo.tracking.ui.theme.GeoTrackingTheme
import com.geo.tracking.utils.LocationUtils.createLocationRequest
import com.geo.tracking.utils.LocationUtils.fetchLastLocation
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeoTrackingTheme {

                var locationFromGps: Location? by remember { mutableStateOf(null) }
                var openDialog: String by remember { mutableStateOf("") }

                val locationPermissionsState = rememberMultiplePermissionsState(
                    listOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    )
                )

                val context = LocalContext.current
                val fusedLocationProviderClient =
                    remember { LocationServices.getFusedLocationProviderClient(context) }
                val locationCallback = remember {
                    object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            Log.d(
                                "onLocationResult",
                                "locationResult.latitude: ${locationResult.lastLocation?.latitude}"
                            )
                            locationFromGps = locationResult.lastLocation
                        }
                    }
                }

                val settingsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult(),
                    onResult = {
                        when (it.resultCode) {
                            Activity.RESULT_OK -> {
                                context.fetchLastLocation(
                                    fusedLocationClient = fusedLocationProviderClient,
                                    settingsLauncher = null,
                                    location = {
                                        Log.d("settingsLauncher", "location: ${it.latitude}")
                                        if (locationFromGps == null && locationFromGps != it) {
                                            locationFromGps = it
                                        }
                                    },
                                    locationCallback = locationCallback
                                )
                            }

                            Activity.RESULT_CANCELED -> {
                                Toast.makeText(
                                    context,
                                    "Activity.RESULT_CANCELED",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )

                LaunchedEffect(
                    key1 = locationPermissionsState.revokedPermissions.size,
                    key2 = locationPermissionsState.shouldShowRationale,
                    block = {
                        fetchLocation(
                            locationPermissionsState,
                            context,
                            settingsLauncher,
                            fusedLocationProviderClient,
                            locationCallback,
                            openDialog = {
                                openDialog = it
                            })
                    })

                LaunchedEffect(
                    key1 = locationFromGps,
                    block = {
                        Log.d("LaunchedEffect", "locationFromGps: $locationFromGps")
                        // TODO: setup GeoCoder

                    }
                )

                DisposableEffect(
                    key1 = true
                ) {
                    onDispose {
                        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(text = "Has permission: ${locationPermissionsState.revokedPermissions.size <= 1}")
                        Text(text = "Current latitude: ${locationFromGps?.latitude}")
                        Text(text = "Current longitude: ${locationFromGps?.longitude}")
                        Button(onClick = {
                            if (
                                locationPermissionsState.revokedPermissions.size == 2
                                && !locationPermissionsState.shouldShowRationale
                            ) {
                                openDialog = "Permission fully denied. Go to settings to enable"
                                Log.d(
                                    "LaunchedEffect",
                                    "revokedPermissions.size == 2 && shouldShowRationale"
                                )
                            } else {
                                fetchLocation(
                                    locationPermissionsState,
                                    context,
                                    settingsLauncher,
                                    fusedLocationProviderClient,
                                    locationCallback,
                                    openDialog = {
                                        openDialog = it
                                    }
                                )
                            }
                        }) {
                            Text(text = "Fetch location")
                        }
                    }
                }


                if (openDialog.isNotEmpty()) {
                    Dialog(
                        onDismissRequest = { openDialog = "" },
                        properties = DialogProperties(
                            dismissOnBackPress = false,
                            dismissOnClickOutside = false
                        )
                    ) {

                    }
                }

            }
        }
    }


    @OptIn(ExperimentalPermissionsApi::class)
    private fun fetchLocation(
        locationPermissionsState: MultiplePermissionsState,
        context: Context,
        settingsLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
        fusedLocationProviderClient: FusedLocationProviderClient,
        locationCallback: LocationCallback,
        openDialog: (String) -> Unit
    ) {
        when {
            locationPermissionsState.revokedPermissions.size <= 1 -> {
                // Has permission at least one permission [coarse or fine]
                context.createLocationRequest(
                    settingsLauncher = settingsLauncher,
                    fusedLocationClient = fusedLocationProviderClient,
                    locationCallback = locationCallback
                )
                Log.d("LaunchedEffect", "revokedPermissions.size <= 1")
            }

            locationPermissionsState.shouldShowRationale -> {
                openDialog("Should show rationale")
                Log.d("LaunchedEffect", "shouldShowRationale")
            }

            locationPermissionsState.revokedPermissions.size == 2 -> {
                locationPermissionsState.launchMultiplePermissionRequest()
                Log.d("LaunchedEffect", "revokedPermissions.size == 2")
            }

            else -> {
                openDialog("This app requires location permission")
                Log.d("LaunchedEffect", "else")
            }
        }
    }
}