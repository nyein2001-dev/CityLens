package com.geo.tracking.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import org.osmdroid.api.IMapController
import org.osmdroid.api.IMapView
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.TileSystem
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.IOverlayMenuProvider
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Overlay.Snappable
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import java.util.LinkedList

class CustomLocationNewOverlay(
    private val mapView: MapView,
    private var myLocationProvider: IMyLocationProvider? = GpsMyLocationProvider(mapView.context)
) : Overlay(), IMyLocationConsumer, IOverlayMenuProvider, Snappable {

    private val mPaint: Paint = Paint().apply {
        isFilterBitmap = true
    }

    private val mCirclePaint: Paint = Paint().apply {
        setARGB(0, 100, 100, 255)
        isAntiAlias = true
    }

    private var mPersonBitmap: Bitmap? = null
    private var mDirectionArrowBitmap: Bitmap? = null

    private val mMapController: IMapController? = mapView.controller
    private var mMapView: MapView? = mapView

    private val mRunOnFirstFix = LinkedList<Runnable>()
    private val mDrawPixel = Point()
    private val mSnapPixel = Point()
    private val mHandler: Handler = Handler(Looper.getMainLooper())
    private val mHandlerToken: Any = Any()

    var enableAutoStop: Boolean = true
    var lastFix: Location? = null
        private set

    private val mGeoPoint = GeoPoint(0, 0)

    var isMyLocationEnabled: Boolean = false
        private set

    var isFollowLocationEnabled: Boolean = false
        private set

    var isDrawAccuracyEnabled: Boolean = true

    private val mPersonHotspot: PointF = PointF()

    private var mDirectionArrowCenterX: Float = 0f
    private var mDirectionArrowCenterY: Float = 0f

    private var mOptionsMenuEnabled = true
    private var wasEnabledOnPause = false

    init {
        setPersonAnchor(.5f, .8125f)
        setDirectionAnchor(.5f, .5f)
    }

    fun setDirectionIcon(pDirectionArrowBitmap: Bitmap?) {
        mDirectionArrowBitmap = pDirectionArrowBitmap
    }

    override fun onResume() {
        super.onResume()
        if (wasEnabledOnPause) enableFollowLocation()
        enableMyLocation()
    }

    override fun onPause() {
        wasEnabledOnPause = isFollowLocationEnabled
        disableMyLocation()
        super.onPause()
    }

    override fun onDetach(mapView: MapView) {
        disableMyLocation()
        this.mMapView = null
        mHandler.removeCallbacksAndMessages(mHandlerToken)
        if (myLocationProvider != null) myLocationProvider!!.destroy()
        super.onDetach(mapView)
    }

    override fun setOptionsMenuEnabled(pOptionsMenuEnabled: Boolean) {
        this.mOptionsMenuEnabled = pOptionsMenuEnabled
    }

    override fun isOptionsMenuEnabled(): Boolean = mOptionsMenuEnabled

    override fun onCreateOptionsMenu(
        pMenu: Menu, pMenuIdOffset: Int,
        pMapView: MapView
    ): Boolean {
        return true
    }

    override fun onPrepareOptionsMenu(
        pMenu: Menu, pMenuIdOffset: Int,
        pMapView: MapView
    ): Boolean {
        pMenu.findItem(MENU_MY_LOCATION + pMenuIdOffset)?.let {
            it.isChecked = isMyLocationEnabled
        }
        return false
    }

    override fun onOptionsItemSelected(
        pItem: MenuItem, pMenuIdOffset: Int,
        pMapView: MapView
    ): Boolean {
        val menuId = pItem.itemId - pMenuIdOffset
        if (menuId == MENU_MY_LOCATION) {
            if (isMyLocationEnabled) {
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

    val myLocation: GeoPoint?
        get() = lastFix?.let { GeoPoint(it) }

    fun enableFollowLocation() {
        isFollowLocationEnabled = true
        if (isMyLocationEnabled) {
            myLocationProvider?.lastKnownLocation?.let { setLocation(it) }
        }
        mMapView?.postInvalidate()
    }

    fun disableFollowLocation() {
        mMapController?.stopAnimation(false)
        isFollowLocationEnabled = false
    }

    override fun onLocationChanged(location: Location, source: IMyLocationProvider) {
        if (mHandler != null) {
            mHandler.postAtTime({
                setLocation(location)
                mRunOnFirstFix.forEach {
                    Thread(it).apply { name = this.javaClass.name + "#onLocationChanged" }.start()
                }
                mRunOnFirstFix.clear()
            }, mHandlerToken, 0)
        }
    }

    protected fun setLocation(location: Location?) {
        location?.let {
            lastFix = it
            mGeoPoint.setCoords(lastFix!!.latitude, lastFix!!.longitude)
            if (isFollowLocationEnabled) {
                mMapController?.animateTo(mGeoPoint)
            } else {
                mMapView?.postInvalidate()
            }
        }
    }

    fun enableMyLocation(myLocationProvider: IMyLocationProvider? = this.myLocationProvider): Boolean {
        this.myLocationProvider = myLocationProvider
        val success = myLocationProvider!!.startLocationProvider(this)
        isMyLocationEnabled = success
        if (success) {
            myLocationProvider.lastKnownLocation?.let { setLocation(it) }
        }
        mMapView?.postInvalidate()
        return success
    }

    fun disableMyLocation() {
        isMyLocationEnabled = false
        stopLocationProvider()
        mMapView?.postInvalidate()
    }

    protected fun stopLocationProvider() {
        myLocationProvider?.stopLocationProvider()
        mHandler.removeCallbacksAndMessages(mHandlerToken)
    }

    fun setPersonIcon(icon: Bitmap?) {
        mPersonBitmap = icon
    }

    fun setPersonAnchor(pHorizontal: Float, pVertical: Float) {
        mPersonBitmap?.let {
            mPersonHotspot.set(it.width * pHorizontal, it.height * pVertical)
        }
    }

    fun setDirectionAnchor(pHorizontal: Float, pVertical: Float) {
        mDirectionArrowBitmap?.let {
            mDirectionArrowCenterX = it.width * pHorizontal
            mDirectionArrowCenterY = it.height * pVertical
        }
    }

    override fun draw(c: Canvas, pProjection: Projection) {
        lastFix?.takeIf { isMyLocationEnabled }?.let {
            drawMyLocation(c, pProjection, it)
        }
    }

    protected fun drawMyLocation(canvas: Canvas, pj: Projection, lastFix: Location) {
        pj.toPixels(mGeoPoint, mDrawPixel)
        if (isDrawAccuracyEnabled) {
            val radius = (lastFix.accuracy / TileSystem.GroundResolution(lastFix.latitude, pj.zoomLevel)).toFloat()
            mCirclePaint.alpha = 50
            mCirclePaint.style = Paint.Style.FILL
            canvas.drawCircle(mDrawPixel.x.toFloat(), mDrawPixel.y.toFloat(), radius, mCirclePaint)
            mCirclePaint.alpha = 150
            mCirclePaint.style = Paint.Style.STROKE
            canvas.drawCircle(mDrawPixel.x.toFloat(), mDrawPixel.y.toFloat(), radius, mCirclePaint)
        }
        lastFix.bearing.takeIf { it >= 0 }?.let {
            canvas.save()
            canvas.rotate(it, mDrawPixel.x.toFloat(), mDrawPixel.y.toFloat())
            mDirectionArrowBitmap?.let {
                canvas.drawBitmap(it, mDrawPixel.x - mDirectionArrowCenterX, mDrawPixel.y - mDirectionArrowCenterY, mPaint)
            }
            canvas.restore()
        }
        mPersonBitmap?.let {
            canvas.drawBitmap(it, mDrawPixel.x - mPersonHotspot.x, mDrawPixel.y - mPersonHotspot.y, mPaint)
        }
    }


    companion object {
        private const val MENU_MY_LOCATION = 1
    }

    override fun onSnapToItem(x: Int, y: Int, snapPoint: Point?, mapView: IMapView?): Boolean {
        TODO("Not yet implemented")
    }
}
