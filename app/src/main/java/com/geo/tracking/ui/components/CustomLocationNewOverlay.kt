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
import org.osmdroid.library.R
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

class CustomLocationNewOverlay(myLocationProvider: IMyLocationProvider?, mapView: MapView) : Overlay(),
    IMyLocationConsumer, IOverlayMenuProvider, Snappable {
    protected var mPaint: Paint = Paint()
    protected var mCirclePaint: Paint? = Paint()

    protected var mPersonBitmap: Bitmap? = null
    protected var mDirectionArrowBitmap: Bitmap? = null

    protected var mMapView: MapView?

    private var mMapController: IMapController?
    var mMyLocationProvider: IMyLocationProvider? = null

    private val mRunOnFirstFix = LinkedList<Runnable>()
    private val mDrawPixel = Point()
    private val mSnapPixel = Point()
    private var mHandler: Handler?
    private var mHandlerToken: Any? = Any()

    var enableAutoStop: Boolean = true
    var lastFix: Location? = null
        private set
    private val mGeoPoint = GeoPoint(0, 0) // for reuse


    var isMyLocationEnabled: Boolean = false
        private set

    var isFollowLocationEnabled: Boolean = false
        protected set

    var isDrawAccuracyEnabled: Boolean = true


    protected val mPersonHotspot: PointF

    protected var mDirectionArrowCenterX: Float = 0f
    protected var mDirectionArrowCenterY: Float = 0f

    private var mOptionsMenuEnabled = true

    private var wasEnabledOnPause = false

    constructor(mapView: MapView) : this(GpsMyLocationProvider(mapView.context), mapView)

    init {
        mMapView = mapView
        mMapController = mapView.controller
        mCirclePaint!!.setARGB(0, 100, 100, 255)
        mCirclePaint!!.isAntiAlias = true
        mPaint.isFilterBitmap = true


        setPersonIcon((mapView.context.resources.getDrawable(R.drawable.person) as BitmapDrawable).bitmap)
        setDirectionIcon((mapView.context.resources.getDrawable(R.drawable.round_navigation_white_48) as BitmapDrawable).bitmap)

        mPersonHotspot = PointF()
        setPersonAnchor(.5f, .8125f)
        setDirectionAnchor(.5f, .5f)

        mHandler = Handler(Looper.getMainLooper())
//        this.myLocationProvider = myLocationProvider
    }

    @Deprecated(
        """Use {@link #setPersonIcon(Bitmap)}, {@link #setDirectionIcon(Bitmap)},
	  {@link #setPersonAnchor(float, float)} and {@link #setDirectionAnchor(float, float)} instead"""
    )
    fun setDirectionArrow(personBitmap: Bitmap?, directionArrowBitmap: Bitmap?) {
        setPersonIcon(personBitmap)
        setDirectionIcon(directionArrowBitmap)
        setDirectionAnchor(.5f, .5f)
    }


    fun setDirectionIcon(pDirectionArrowBitmap: Bitmap?) {
        mDirectionArrowBitmap = pDirectionArrowBitmap
    }

    override fun onResume() {
        super.onResume()
        if (wasEnabledOnPause) this.enableFollowLocation()
        this.enableMyLocation()
    }

    override fun onPause() {
        wasEnabledOnPause = isFollowLocationEnabled
        this.disableMyLocation()
        super.onPause()
    }

    override fun onDetach(mapView: MapView) {
        this.disableMyLocation()
        this.mMapView = null
        this.mMapController = null
        mHandler = null
        mCirclePaint = null
        mHandlerToken = null
        lastFix = null
        mMapController = null
        if (mMyLocationProvider != null) mMyLocationProvider!!.destroy()

        mMyLocationProvider = null
        super.onDetach(mapView)
    }


    var myLocationProvider: IMyLocationProvider?
        get() = mMyLocationProvider
        protected set(myLocationProvider) {
            if (myLocationProvider == null) throw RuntimeException(
                "You must pass an IMyLocationProvider to setMyLocationProvider()"
            )

            if (isMyLocationEnabled) stopLocationProvider()

            mMyLocationProvider = myLocationProvider
        }


    @Deprecated("Use {@link #setPersonAnchor(float, float)} instead")
    fun setPersonHotspot(x: Float, y: Float) {
        mPersonHotspot[x] = y
    }

    protected fun drawMyLocation(canvas: Canvas, pj: Projection, lastFix: Location) {
        pj.toPixels(mGeoPoint, mDrawPixel)

        if (isDrawAccuracyEnabled) {
            val radius = (lastFix.accuracy
                    / TileSystem.GroundResolution(
                lastFix.latitude,
                pj.zoomLevel
            ).toFloat())

            mCirclePaint!!.alpha = 50
            mCirclePaint!!.style = Paint.Style.FILL
            canvas.drawCircle(
                mDrawPixel.x.toFloat(), mDrawPixel.y.toFloat(), radius,
                mCirclePaint!!
            )

            mCirclePaint!!.alpha = 150
            mCirclePaint!!.style = Paint.Style.STROKE
            canvas.drawCircle(
                mDrawPixel.x.toFloat(), mDrawPixel.y.toFloat(), radius,
                mCirclePaint!!
            )
        }

        if (lastFix.hasBearing()) {
            canvas.save()
            var mapRotation: Float
            mapRotation = lastFix.bearing
            if (mapRotation >= 360.0f) mapRotation = mapRotation - 360f
            canvas.rotate(mapRotation, mDrawPixel.x.toFloat(), mDrawPixel.y.toFloat())
            // Draw the bitmap
            canvas.drawBitmap(
                mDirectionArrowBitmap!!, mDrawPixel.x
                        - mDirectionArrowCenterX, mDrawPixel.y - mDirectionArrowCenterY,
                mPaint
            )
            canvas.restore()
        } else {
            canvas.save()
            canvas.rotate(
                -mMapView!!.mapOrientation, mDrawPixel.x.toFloat(),
                mDrawPixel.y.toFloat()
            )
            canvas.drawBitmap(
                mPersonBitmap!!, mDrawPixel.x - mPersonHotspot.x,
                mDrawPixel.y - mPersonHotspot.y, mPaint
            )
            canvas.restore()
        }
    }

    override fun draw(c: Canvas, pProjection: Projection) {
        if (lastFix != null && isMyLocationEnabled) {
            drawMyLocation(c, pProjection, lastFix!!)
        }
    }

    override fun onSnapToItem(
        x: Int, y: Int, snapPoint: Point,
        mapView: IMapView
    ): Boolean {
        if (this.lastFix != null) {
            val pj = mMapView!!.projection
            pj.toPixels(mGeoPoint, mSnapPixel)
            snapPoint.x = mSnapPixel.x
            snapPoint.y = mSnapPixel.y
            val xDiff = (x - mSnapPixel.x).toDouble()
            val yDiff = (y - mSnapPixel.y).toDouble()
            val snap = xDiff * xDiff + yDiff * yDiff < 64
            if (Configuration.getInstance().isDebugMode) {
                Log.d(IMapView.LOGTAG, "snap=$snap")
            }
            return snap
        } else {
            return false
        }
    }

    override fun onTouchEvent(event: MotionEvent, mapView: MapView): Boolean {
        val isSingleFingerDrag = ((event.action == MotionEvent.ACTION_MOVE)
                && (event.pointerCount == 1))

        if (event.action == MotionEvent.ACTION_DOWN && enableAutoStop) {
            this.disableFollowLocation()
        } else if (isSingleFingerDrag && isFollowLocationEnabled) {
            return true // prevent the pan
        }

        return super.onTouchEvent(event, mapView)
    }

    override fun setOptionsMenuEnabled(pOptionsMenuEnabled: Boolean) {
        this.mOptionsMenuEnabled = pOptionsMenuEnabled
    }

    override fun isOptionsMenuEnabled(): Boolean {
        return this.mOptionsMenuEnabled
    }

    override fun onCreateOptionsMenu(
        pMenu: Menu, pMenuIdOffset: Int,
        pMapView: MapView
    ): Boolean {
        pMenu.add(
            0, MENU_MY_LOCATION + pMenuIdOffset, Menu.NONE,
            pMapView.context.resources.getString(R.string.my_location)
        )
            .setIcon(
                pMapView.context.resources.getDrawable(R.drawable.ic_menu_mylocation)
            )
            .setCheckable(true)

        return true
    }

    override fun onPrepareOptionsMenu(
        pMenu: Menu, pMenuIdOffset: Int,
        pMapView: MapView
    ): Boolean {
        pMenu.findItem(MENU_MY_LOCATION + pMenuIdOffset).setChecked(
            isMyLocationEnabled
        )
        return false
    }

    override fun onOptionsItemSelected(
        pItem: MenuItem, pMenuIdOffset: Int,
        pMapView: MapView
    ): Boolean {
        val menuId = pItem.itemId - pMenuIdOffset
        if (menuId == MENU_MY_LOCATION) {
            if (this.isMyLocationEnabled) {
                this.disableFollowLocation()
                this.disableMyLocation()
            } else {
                this.enableFollowLocation()
                this.enableMyLocation()
            }
            return true
        } else {
            return false
        }
    }

    val myLocation: GeoPoint?
        get() = if (lastFix == null) {
            null
        } else {
            GeoPoint(lastFix)
        }


    fun enableFollowLocation() {
        isFollowLocationEnabled = true

        // set initial location when enabled
        if (isMyLocationEnabled) {
            val location = mMyLocationProvider!!.lastKnownLocation
            if (location != null) {
                setLocation(location)
            }
        }

        if (mMapView != null) {
            mMapView!!.postInvalidate()
        }
    }

    fun disableFollowLocation() {
        if (mMapController != null) mMapController!!.stopAnimation(false)
        isFollowLocationEnabled = false
    }

    override fun onLocationChanged(location: Location, source: IMyLocationProvider) {
        if (location != null && mHandler != null) {
            // These location updates can come in from different threads
            mHandler!!.postAtTime(object : Runnable {
                override fun run() {
                    setLocation(location)

                    for (runnable in mRunOnFirstFix) {
                        val t = Thread(runnable)
                        t.name = this.javaClass.name + "#onLocationChanged"
                        t.start()
                    }
                    mRunOnFirstFix.clear()
                }
            }, mHandlerToken, 0)
        }
    }

    protected fun setLocation(location: Location?) {
        lastFix = location
        mGeoPoint.setCoords(lastFix!!.latitude, lastFix!!.longitude)
        if (isFollowLocationEnabled) {
            mMapController!!.animateTo(mGeoPoint)
        } else if (mMapView != null) {
            mMapView!!.postInvalidate()
        }
    }

    @JvmOverloads
    fun enableMyLocation(myLocationProvider: IMyLocationProvider? = mMyLocationProvider): Boolean {
        this.myLocationProvider = myLocationProvider

        val success = mMyLocationProvider!!.startLocationProvider(this)
        isMyLocationEnabled = success

        if (success) {
            val location = mMyLocationProvider!!.lastKnownLocation
            if (location != null) {
                setLocation(location)
            }
        }

        if (mMapView != null) {
            mMapView!!.postInvalidate()
        }

        return success
    }


    fun disableMyLocation() {
        isMyLocationEnabled = false

        stopLocationProvider()

        // Update the screen to see changes take effect
        if (mMapView != null) {
            mMapView!!.postInvalidate()
        }
    }

    protected fun stopLocationProvider() {
        if (mMyLocationProvider != null) {
            mMyLocationProvider!!.stopLocationProvider()
        }
        if (mHandler != null && mHandlerToken != null) mHandler!!.removeCallbacksAndMessages(
            mHandlerToken
        )
    }


    fun runOnFirstFix(runnable: Runnable): Boolean {
        if (mMyLocationProvider != null && lastFix != null) {
            val t = Thread(runnable)
            t.name = this.javaClass.name + "#runOnFirstFix"
            t.start()
            return true
        } else {
            mRunOnFirstFix.addLast(runnable)
            return false
        }
    }


    fun setPersonIcon(icon: Bitmap?) {
        mPersonBitmap = icon
    }


    fun setPersonAnchor(pHorizontal: Float, pVertical: Float) {
        mPersonHotspot[mPersonBitmap!!.width * pHorizontal] = mPersonBitmap!!.height * pVertical
    }


    fun setDirectionAnchor(pHorizontal: Float, pVertical: Float) {
        mDirectionArrowCenterX = mDirectionArrowBitmap!!.width * pHorizontal
        mDirectionArrowCenterY = mDirectionArrowBitmap!!.height * pVertical
    }

    companion object {
        val MENU_MY_LOCATION: Int = getSafeMenuId()
    }
}