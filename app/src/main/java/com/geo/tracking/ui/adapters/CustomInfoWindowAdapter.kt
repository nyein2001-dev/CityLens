package com.geo.tracking.ui.adapters

import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import com.geo.tracking.ui.components.CustomInfoWindow
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

class CustomInfoWindowAdapter(private val mapView: MapView) :
    InfoWindow(FrameLayout(mapView.context), mapView) {

    override fun onOpen(item: Any?) {
        val marker = item as? Marker
        marker?.let {
            val composeView = ComposeView(mapView.context)
            composeView.setContent {
                CustomInfoWindow(
                    title = marker.title ?: "",
                    snippet = marker.snippet ?: ""
                )
            }

            (mView as FrameLayout).removeAllViews()
            (mView as FrameLayout).addView(composeView)
        }
    }

    override fun onClose() {
        (mView as FrameLayout).removeAllViews()
    }
}