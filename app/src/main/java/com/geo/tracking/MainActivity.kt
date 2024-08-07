package com.geo.tracking

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.geo.tracking.ui.theme.GeoTrackingTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GeoTrackingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var locationText by remember { mutableStateOf("No location obtained :(") }
                    var showPermissionResultText by remember { mutableStateOf(false) }
                    var permissionResultText by remember { mutableStateOf("Permission Granted...") }

                    RequestLocationPermission(
                        onPermissionGranted = {
                            showPermissionResultText = true
                            getLastUserLocation(
                                onGetLastLocationSuccess = {
                                    locationText =
                                        "Location using LAST-LOCATION: LATITUDE: ${it.first}, LONGITUDE: ${it.second}"
                                },
                                onGetLastLocationFailed = { exception ->
                                    showPermissionResultText = true
                                    locationText =
                                        exception.localizedMessage ?: "Error Getting Last Location"
                                },
                                onGetLastLocationIsNull = {

                                }
                            )
                        },
                        onPermissionDenied = {
                            showPermissionResultText = true
                            permissionResultText = "Permissoin Denied :("
                        },
                        onPermissionsRevoked = {
                            showPermissionResultText = true;
                            permissionResultText = "Permission Revoked :("
                        }
                    )

                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Requesting location permission...",
                            textAlign = TextAlign.Center
                        )
                        if (showPermissionResultText) {
                            Text(text = permissionResultText, textAlign = TextAlign.Center)
                            Text(text = locationText, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }

    private fun getLastUserLocation(
        onGetLastLocationSuccess: (Pair<Double, Double>) -> Unit,
        onGetLastLocationFailed: (Exception) -> Unit,
        onGetLastLocationIsNull: () -> Unit
    ) {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (areLocationPermissionGranted()) {
            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        onGetLastLocationSuccess(Pair(it.latitude, it.longitude))
                    }?.run {
                        onGetLastLocationIsNull()
                    }
                }
                .addOnFailureListener { exception ->
                    onGetLastLocationFailed(exception)
                }
        }
    }

    private fun areLocationPermissionGranted(): Boolean {
        return (ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestLocationPermission(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionsRevoked: () -> Unit
) {
    val permissionState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )
    LaunchedEffect(key1 = permissionState) {
        val allPermissionRevoked =
            permissionState.permissions.size == permissionState.revokedPermissions.size

        val permissionToRequest = permissionState.permissions.filter { !it.status.isGranted }

        if (permissionToRequest.isNotEmpty()) permissionState.launchMultiplePermissionRequest()

        if (allPermissionRevoked) {
            onPermissionsRevoked()
        } else {
            if (permissionState.allPermissionsGranted) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GeoTrackingTheme {
        RequestLocationPermission(
            onPermissionGranted = {

            },
            onPermissionDenied = {

            },
            onPermissionsRevoked = {

            }
        )
    }
}