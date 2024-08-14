package com.geo.tracking.ui.components

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.Point
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
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
import java.util.Random

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

    private var infoWindowBitmap: Bitmap? = null
    private var directionArrowBitmap: Bitmap? = null
    private var mapController: IMapController? = mapView.controller
    private var location: Location = initialPoint
    private val geoPoint = GeoPoint(initialPoint.latitude, initialPoint.longitude)
    private var isLocationEnabled = false
    private var isFollowing = false
    private var drawAccuracyEnabled = true
    private var directionArrowCenterX = 0f
    private var directionArrowCenterY = 0f
    private var enableAutoStop = true
    private val drawPixel = Point()
    private val snapPixel = Point()
    private val handler = Handler(Looper.getMainLooper())
    private val runOnFirstFix = LinkedList<Runnable>()
    private var optionsMenuEnabled = true
    private var wasEnabledOnPause = false

    init {
        initializeIcons()
    }

    private fun initializeIcons() {
        setDirectionIcon(ContextCompat.getDrawable(mapView.context, R.drawable.rocket_direction)!!)
        setDirectionAnchor()
    }

    override fun draw(canvas: Canvas, projection: Projection) {
        if (isLocationEnabled) {
            drawMyLocation(canvas, projection, location)
        }
    }

    private fun drawMyLocation(canvas: Canvas, projection: Projection, lastFix: Location) {
        projection.toPixels(geoPoint, drawPixel)
        drawAccuracyCircle(canvas, lastFix, projection)
        drawDirectionArrow(canvas, lastFix)
        drawPersonIcon(canvas)
        drawFlamingRocket(canvas, lastFix)
    }

    private fun drawAccuracyCircle(canvas: Canvas, lastFix: Location, projection: Projection) {
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
    }

    private fun drawDirectionArrow(canvas: Canvas, lastFix: Location) {
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
    }

    private fun drawPersonIcon(canvas: Canvas) {
        val personIconX = drawPixel.x - directionArrowCenterX
        val personIconY = drawPixel.y - directionArrowCenterY
        infoWindowBitmap?.let { infoBitmap ->
            val infoWindowX = (personIconX - (infoBitmap.width / 2.25)).toFloat()
            val infoWindowY = personIconY - infoBitmap.height - 25
            canvas.drawBitmap(infoBitmap, infoWindowX, infoWindowY, paint)
        }
    }

    private fun drawFlamingRocket(canvas: Canvas, lastFix: Location) {
        canvas.save()
        val mapRotation = lastFix.bearing % 360f
        canvas.rotate(mapRotation, drawPixel.x.toFloat(), drawPixel.y.toFloat())

        renderFlameAnimation(canvas, lastFix.speed)

        directionArrowBitmap?.let {
            canvas.drawBitmap(
                it,
                drawPixel.x - directionArrowCenterX,
                drawPixel.y - directionArrowCenterY,
                paint
            )
        }
        canvas.restore()
    }

    private fun renderFlameAnimation(canvas: Canvas, speed: Float) {
        val flameSize = speed * 15f

        val flamePaint = Paint().apply {
            isAntiAlias = true
            style = Style.FILL
        }

        flamePaint.shader = LinearGradient(
            drawPixel.x.toFloat(),
            drawPixel.y + directionArrowCenterY,
            drawPixel.x.toFloat(),
            drawPixel.y + directionArrowCenterY + flameSize,
            intArrayOf(Color.RED, Color.parseColor("#FF4500"), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(
            drawPixel.x.toFloat(),
            drawPixel.y + directionArrowCenterY + flameSize / 2,
            flameSize,
            flamePaint
        )

        flamePaint.shader = LinearGradient(
            drawPixel.x.toFloat(),
            drawPixel.y + directionArrowCenterY,
            drawPixel.x.toFloat(),
            drawPixel.y + directionArrowCenterY + flameSize * 0.7f,
            intArrayOf(Color.YELLOW, Color.parseColor("#FFA500"), Color.TRANSPARENT),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(
            drawPixel.x.toFloat(),
            drawPixel.y + directionArrowCenterY + flameSize * 0.7f / 2,
            flameSize * 0.7f,
            flamePaint
        )

        flamePaint.shader = LinearGradient(
            drawPixel.x.toFloat(),
            drawPixel.y + directionArrowCenterY,
            drawPixel.x.toFloat(),
            drawPixel.y + directionArrowCenterY + flameSize * 0.4f,
            intArrayOf(Color.WHITE, Color.YELLOW, Color.TRANSPARENT),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(
            drawPixel.x.toFloat(),
            drawPixel.y + directionArrowCenterY + flameSize * 0.4f / 2,
            flameSize * 0.4f,
            flamePaint
        )

        renderSparks(canvas, flameSize, speed)
    }

    private fun renderSparks(canvas: Canvas, flameSize: Float, speed: Float) {
        val sparksCount = (speed * 0.2f).toInt().coerceAtLeast(5)
        val random = Random()

        val sparkPaint = Paint().apply {
            isAntiAlias = true
            style = Style.FILL
            color = Color.YELLOW
        }

        for (i in 0 until sparksCount) {
            val xOffset = random.nextFloat() * flameSize - flameSize / 2
            val yOffset = random.nextFloat() * flameSize + directionArrowCenterY
            val sparkSize = random.nextFloat() * 5f + 2f

            sparkPaint.alpha = (random.nextFloat() * 200 + 55).toInt()

            canvas.drawCircle(
                drawPixel.x + xOffset,
                drawPixel.y + yOffset,
                sparkSize,
                sparkPaint
            )
        }
    }


    private fun renderInfoWindow(position: Location) {
        renderComposableToBitmapSafely(mapView.context, position) { bitmap ->
            infoWindowBitmap = bitmap
            mapView.postInvalidate()
        }
    }


    fun updateInfoWindow(position: Location) {
        renderInfoWindow(position)
        mapView.postInvalidate()
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
        mapController = null
        directionArrowBitmap = null
        handler.removeCallbacksAndMessages(null)
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

    fun enableFollowLocation() {
        isFollowing = true
        myLocationProvider.lastKnownLocation?.let { setLocation(it) }
        mapView.postInvalidate()
    }

    private fun disableFollowLocation() {
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
        handler.removeCallbacksAndMessages(null)
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
        updateInfoWindow(location)
    }

    private fun setDirectionIcon(drawable: Drawable) {
        directionArrowBitmap = drawable.toBitmap()
    }

    private fun setDirectionAnchor() {
        directionArrowBitmap?.let {
            directionArrowCenterX = it.width * 0.5f
            directionArrowCenterY = it.height * 0.5f
        }
    }

    private fun Drawable.toBitmap(): Bitmap {
        return when (this) {
            is BitmapDrawable -> this.bitmap
            is VectorDrawable -> {
                val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                setBounds(0, 0, canvas.width, canvas.height)
                draw(canvas)
                bitmap
            }

            else -> throw IllegalArgumentException("Unsupported drawable type")
        }
    }

    private fun renderComposableToBitmapSafely(
        context: Context,
        position: Location,
        onBitmapReady: (Bitmap) -> Unit
    ) {
        val frameLayout = FrameLayout(context)
        val composeView = ComposeView(context).apply {
            layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                MapInfoWindowContent(location = position)
            }
        }

        frameLayout.addView(composeView)

        val rootView =
            (context as Activity).window.decorView.findViewById<ViewGroup>(android.R.id.content)
        rootView.addView(frameLayout)

        frameLayout.doOnLayout {
            val width = composeView.width
            val height = composeView.height
            val bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            frameLayout.draw(canvas)

            rootView.removeView(frameLayout)
            onBitmapReady(bitmap)
        }
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
        }, Any(), 0)
    }
}