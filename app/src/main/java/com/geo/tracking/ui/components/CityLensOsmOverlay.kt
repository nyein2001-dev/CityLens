package com.geo.tracking.ui.components

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.Point
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.geo.tracking.R
import org.osmdroid.api.IMapController
import org.osmdroid.api.IMapView
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.TileSystem
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.IOverlayMenuProvider
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import java.util.LinkedList

class CityLensOsmOverlay(
    initialPoint: Location,
    private val mapView: MapView,
    private var myLocationProvider: IMyLocationProvider = GpsMyLocationProvider(mapView.context)
) : Overlay(), IMyLocationConsumer, IOverlayMenuProvider, Overlay.Snappable {

    private val paint = Paint().apply { isFilterBitmap = true }
    private val circlePaint = Paint().apply {
        setARGB(0, 100, 100, 255)
        isAntiAlias = true
    }
    private var personBitmap: Bitmap? = null
    private var directionArrowBitmap: Bitmap? = null
    private var mapController: IMapController? = mapView.controller
    private var location: Location = initialPoint
    private val geoPoint = GeoPoint(initialPoint.latitude, initialPoint.longitude)
    private var isLocationEnabled = false
    private var isFollowing = false
    private var drawAccuracyEnabled = true
    private val personHotspot = PointF()
    private var directionArrowCenterX = 0f
    private var directionArrowCenterY = 0f
    private var enableAutoStop = true
    private val drawPixel = Point()
    private val snapPixel = Point()
    private val handler = Handler(Looper.getMainLooper())
    private val handlerToken = Any()
    private val runOnFirstFix = LinkedList<Runnable>()
    private var optionsMenuEnabled = true
    private var wasEnabledOnPause = false

    init {
        setPersonIcon(
            ContextCompat.getDrawable(
                mapView.context,
                R.drawable.baseline_circle_24
            )!!
        )
        setDirectionIcon(
            ContextCompat.getDrawable(
                mapView.context,
                R.drawable.arrow
            )!!
        )
        setPersonAnchor()
        setDirectionAnchor()
    }

    override fun draw(canvas: Canvas, projection: Projection) {
        location.let {
            if (isLocationEnabled) {
                drawMyLocation(canvas, projection, it)
            }
        }
    }

    private fun drawMyLocation(canvas: Canvas, projection: Projection, lastFix: Location) {
        projection.toPixels(geoPoint, drawPixel)

        if (drawAccuracyEnabled) {
            val radius = lastFix.accuracy / TileSystem.GroundResolution(
                lastFix.latitude,
                projection.zoomLevel
            ).toFloat()

            circlePaint.alpha = 50
            circlePaint.style = Style.FILL
            canvas.drawCircle(drawPixel.x.toFloat(), drawPixel.y.toFloat(), radius, circlePaint)

            circlePaint.alpha = 150
            circlePaint.style = Style.STROKE
            canvas.drawCircle(drawPixel.x.toFloat(), drawPixel.y.toFloat(), radius, circlePaint)
        }

        if (lastFix.hasBearing()) {
            canvas.save()
            val mapRotation = lastFix.bearing % 360f
            canvas.rotate(mapRotation, drawPixel.x.toFloat(), drawPixel.y.toFloat())
            directionArrowBitmap?.let {
                canvas.drawBitmap(
                    it,
                    drawPixel.x - directionArrowCenterX,
                    drawPixel.y - directionArrowCenterY,
                    paint
                )
            }
            canvas.restore()
        } else {
            canvas.save()
            canvas.rotate(-mapView.mapOrientation, drawPixel.x.toFloat(), drawPixel.y.toFloat())
            personBitmap?.let {
                canvas.drawBitmap(
                    it,
                    drawPixel.x - personHotspot.x,
                    drawPixel.y - personHotspot.y,
                    paint
                )
            }
            canvas.restore()
        }
    }

    override fun onSnapToItem(x: Int, y: Int, snapPoint: Point, mapView: IMapView?): Boolean {
        location.let {
            mapView?.projection?.toPixels(geoPoint, snapPixel)
            snapPoint.set(snapPixel.x, snapPixel.y)
            val xDiff = (x - snapPixel.x).toDouble()
            val yDiff = (y - snapPixel.y).toDouble()
            return xDiff * xDiff + yDiff * yDiff < 64
        }
    }

    override fun onTouchEvent(event: MotionEvent, mapView: MapView): Boolean {
        val isSingleFingerDrag = event.action == MotionEvent.ACTION_MOVE && event.pointerCount == 1

        if (event.action == MotionEvent.ACTION_DOWN && enableAutoStop) {
            disableFollowLocation()
        } else if (isSingleFingerDrag && isFollowing) {
            return true
        }

        return super.onTouchEvent(event, mapView)
    }

    override fun onResume() {
        super.onResume()
        if (wasEnabledOnPause) enableFollowLocation()
        enableMyLocation()
    }

    override fun onPause() {
        wasEnabledOnPause = isFollowing
        disableMyLocation()
        super.onPause()
    }

    override fun onDetach(mapView: MapView?) {
        disableMyLocation()
        this.mapController = null
        personBitmap = null
        directionArrowBitmap = null
        handler.removeCallbacksAndMessages(handlerToken)
        myLocationProvider.destroy()
        super.onDetach(mapView)
    }

    override fun setOptionsMenuEnabled(pOptionsMenuEnabled: Boolean) {
        optionsMenuEnabled = pOptionsMenuEnabled
    }

    override fun isOptionsMenuEnabled(): Boolean = optionsMenuEnabled

    override fun onCreateOptionsMenu(menu: Menu, pMenuIdOffset: Int, pMapView: MapView?): Boolean {
        menu.add(
            0,
            MENU_MY_LOCATION + pMenuIdOffset,
            Menu.NONE,
            pMapView?.context?.resources?.getString(R.string.my_location)
        )
            .setIcon(
                ContextCompat.getDrawable(
                    pMapView?.context!!,
                    R.drawable.round_my_location_24
                )
            )
            .isCheckable = true

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu, pMenuIdOffset: Int, pMapView: MapView?): Boolean {
        menu.findItem(MENU_MY_LOCATION + pMenuIdOffset).isChecked = isLocationEnabled
        return false
    }

    override fun onOptionsItemSelected(
        item: MenuItem,
        pMenuIdOffset: Int,
        pMapView: MapView?
    ): Boolean {
        if (item.itemId - pMenuIdOffset == MENU_MY_LOCATION) {
            if (isLocationEnabled) {
                disableFollowLocation()
                disableMyLocation()
            } else {
                enableFollowLocation()
                enableMyLocation()
            }
            return true
        }
        return false
    }

    private fun enableFollowLocation() {
        isFollowing = true
        myLocationProvider.lastKnownLocation?.let {
            setLocation(it)
        }
        mapView.postInvalidate()
    }

    fun disableFollowLocation() {
        mapController?.stopAnimation(false)
        isFollowing = false
    }

    fun enableMyLocation(): Boolean {
        return myLocationProvider.startLocationProvider(this).also {
            isLocationEnabled = it
            myLocationProvider.lastKnownLocation?.let { loc -> setLocation(loc) }
            mapView.postInvalidate()
        }
    }

    private fun disableMyLocation() {
        isLocationEnabled = false
        myLocationProvider.stopLocationProvider()
        handler.removeCallbacksAndMessages(handlerToken)
        mapView.postInvalidate()
    }

    private fun setLocation(location: Location) {
        this.location = location
        geoPoint.setCoords(location.latitude, location.longitude)
        if (isFollowing) {
            mapController?.animateTo(geoPoint)
        } else {
            mapView.postInvalidate()
        }
    }

    private fun setPersonIcon(drawable: Drawable) {
        personBitmap = when (drawable) {
            is BitmapDrawable -> {
                drawable.bitmap
            }

            is VectorDrawable -> {
                vectorDrawableToBitmap(drawable)
            }

            else -> {
                null
            }
        }
    }

    private fun setDirectionIcon(drawable: Drawable) {
        directionArrowBitmap = when (drawable) {
            is BitmapDrawable -> {
                drawable.bitmap
            }

            is VectorDrawable -> {
                vectorDrawableToBitmap(drawable)
            }

            else -> {
                null
            }
        }
    }

    private fun setPersonAnchor() {
        personBitmap?.let {
            personHotspot.set(it.width * 0.5f, it.height * 0.5f)
        }
    }

    private fun setDirectionAnchor() {
        directionArrowBitmap?.let {
            directionArrowCenterX = it.width * 0.5f
            directionArrowCenterY = it.height * 0.5f
        }
    }

    private fun vectorDrawableToBitmap(vectorDrawable: VectorDrawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight,
            Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return bitmap
    }

    companion object {
        private const val MENU_MY_LOCATION = 1
    }


    override fun onLocationChanged(location: Location, source: IMyLocationProvider) {
        handler.postAtTime({
            setLocation(location)
            runOnFirstFix.forEach {
                Thread(it).apply { name = this.javaClass.name + "#onLocationChanged" }.start()
            }
            runOnFirstFix.clear()
        }, handlerToken, 0)
    }
}

