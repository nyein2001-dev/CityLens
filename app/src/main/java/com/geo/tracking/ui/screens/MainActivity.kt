package com.geo.tracking.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.geo.tracking.R
import com.geo.tracking.extension.hasLocationPermission
import com.geo.tracking.ui.components.CityLensOsmOverlay
import com.geo.tracking.ui.components.OsmOverlay
import com.geo.tracking.ui.theme.GeoTrackingTheme
import com.geo.tracking.ui.viewmodel.MainActivityVM
import com.geo.tracking.ui.viewmodel.PermissionEvent
import com.geo.tracking.ui.viewmodel.ViewState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val locationViewModel: MainActivityVM by viewModels()

        setContent {

            val permissionState = rememberMultiplePermissionsState(
                permissions = listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )

            val viewState by locationViewModel.viewState.collectAsStateWithLifecycle()

            GeoTrackingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
//                    TopAppBar(
//                        title = {
//                            Text(
//                                LocalContext.current.getString(R.string.app_name),
//                                style = MaterialTheme.typography.titleLarge
//                            )
//                        },
//                    )
                    LaunchedEffect(!hasLocationPermission()) {
                        permissionState.launchMultiplePermissionRequest()
                    }

                    when {
                        permissionState.allPermissionsGranted -> {
                            LaunchedEffect(Unit) {
                                locationViewModel.handle(PermissionEvent.Granted)
                            }
                        }

                        permissionState.shouldShowRationale -> {
                            RationaleAlert(onDismiss = {}) {
                                permissionState.launchMultiplePermissionRequest()
                            }
                        }

                        !permissionState.allPermissionsGranted && !permissionState.shouldShowRationale -> {
                            LaunchedEffect(Unit) {
                                locationViewModel.handle(PermissionEvent.Revoked)
                            }
                        }
                    }

                    with(viewState) {
                        when (this) {
                            ViewState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            ViewState.RevokedPermissions -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "We need permissions to use this app")
                                    Button(
                                        onClick = {
                                            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                        },
                                        enabled = !hasLocationPermission()
                                    ) {
                                        if (hasLocationPermission()) CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = Color.White
                                        ) else Text(
                                            text = "Settings"
                                        )
                                    }

                                }
                            }

                            is ViewState.Success -> {
                                MainScreen(currentPosition = location)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RationaleAlert(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties()) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "We need location permissions to use this app"
                )
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = "OK")
                }
            }
        }
    }
}

@Composable
fun MainScreen(currentPosition: Location?) {
//    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()

    AndroidView(
        factory = {
            mapView.apply {
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

                currentPosition?.let { position ->
                    controller.setCenter(GeoPoint(position.latitude, position.longitude))
//                    overlays.clear()
//                    val marker = Marker(this).apply {
//                        this.position = position
//                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
//                        title = "Location Information"
//                        icon = ContextCompat.getDrawable(context, R.drawable.baseline_circle_24)
//                        snippet = "${position.latitude}, ${position.longitude}"
//                        infoWindow = CustomInfoWindowAdapter(mapView)
//                        showInfoWindow()
//                        setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
//                    }
//                    overlays.add(marker)
                } ?: run {
                    // Optionally handle the case where currentPosition is null
                    // For example, you could add a default marker or show a message
                }

                overlays.add(
                    currentPosition?.let { it1 ->
                        CityLensOsmOverlay(it1, this).apply {
                            disableFollowLocation()
                            enableMyLocation()
                        }
                    }
                )
            }
            mapView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            Configuration.getInstance()
                .load(context, context.getSharedPreferences("osm_pref", Context.MODE_PRIVATE))
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(
        lifecycleOwner
    ) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = MapLifecycleObserver(mapView)
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    return mapView
}

private class MapLifecycleObserver(
    private val mapView: MapView
) : DefaultLifecycleObserver {
    override fun onResume(owner: LifecycleOwner) {
        mapView.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        mapView.onPause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        mapView.onDetach()
    }
}