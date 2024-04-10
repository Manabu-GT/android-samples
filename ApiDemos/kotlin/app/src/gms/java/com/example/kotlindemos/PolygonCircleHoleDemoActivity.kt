package com.example.kotlindemos

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * This demonstrates a bug on the latest renderer where a circle hold does not get drawn correctly.
 */
class PolygonCircleHoleDemoActivity :
    AppCompatActivity(),
    OnMapReadyCallback,
    SeekBar.OnSeekBarChangeListener {

    private val center = LatLng(37.78, -122.41)
    private val worldPolygonLatLngs: List<LatLng>
        get() {
            // +-------------+
            // |1     8     7|
            // |             |
            // |2   (0,0)   6|
            // |             |
            // |3     4     5|
            // +-------------+
            val delta = 0.01
            val maxLat = 85.0
            return listOf(
                LatLng(maxLat - delta, -180 + delta), // 1
                LatLng(0.0, -180 + delta), // 2
                LatLng(-maxLat + delta, -180 + delta), // 3
                LatLng(-maxLat + delta, 0.0), // 4
                LatLng(-maxLat + delta, 180 - delta), // 5
                LatLng(0.0, 180 - delta), // 6
                LatLng(maxLat - delta, 180 - delta), // 7
                LatLng(maxLat - delta, 0.0), // 8
                LatLng(maxLat - delta, -180 + delta) // 1
            )
        }

    private val fillColorArgb = Color.argb(25, 0, 0, 127)
    private lateinit var mutablePolygon: Polygon
    private lateinit var numPointsSeekBar: SeekBar
    private var map: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // change here to switch between the latest and legacy renderer
        MapsInitializer.initialize(this, MapsInitializer.Renderer.LEGACY
        ) { renderer ->
            Log.i(
                "PolygonCircleHoleDemo",
                "Maps SDK initialized with renderer: $renderer"
            )
        }
        setContentView(R.layout.polygon_circle_hole_demo)

        numPointsSeekBar = findViewById<SeekBar>(R.id.numPointsSeekBar).apply {
            @RequiresApi(Build.VERSION_CODES.O)
            min = 3
            max = 360
            progress = 64
            setOnSeekBarChangeListener(this@PolygonCircleHoleDemoActivity)
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        with(googleMap) {
            moveCamera(CameraUpdateFactory.newLatLngZoom(center, 19f))
            mutablePolygon = addPolygon(PolygonOptions().apply {
                addAll(worldPolygonLatLngs)
                addHole(createCircleHole(center, points = numPointsSeekBar.progress.toDouble()))
                fillColor(fillColorArgb)
                strokeWidth(0f)
            })
        }
        map = googleMap
    }

    private fun createCircleHole(center: LatLng, radius: Double = 50.0, points: Double): List<LatLng> {
        val degreesBetweenPoints = 360.0 / points
        val distRadians = radius / 6371000.0

        val centerLatRadians = Math.toRadians(center.latitude)
        val centerLonRadians = Math.toRadians(center.longitude)
        val coordinates = mutableListOf<LatLng>()
        // array to hold all the points
        for (index in 0 until points.toInt()) {
            val degrees = index * degreesBetweenPoints
            val degreeRadians = Math.toRadians(degrees)
            val pointLatRadians = asin(sin(centerLatRadians) * cos(distRadians) + cos(centerLatRadians) * sin(distRadians) * cos(degreeRadians))
            val pointLonRadians = centerLonRadians + atan2(
                sin(degreeRadians) * sin(distRadians) * cos(centerLatRadians),
                cos(distRadians) - sin(centerLatRadians) * sin(pointLatRadians)
            )
            val pointLat = Math.toDegrees(pointLatRadians)
            val pointLon = Math.toDegrees(pointLonRadians)
            val point = LatLng(pointLat, pointLon)
            coordinates.add(point)
        }
        return coordinates.reversed()
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int,
                                   fromUser: Boolean) {
        mutablePolygon.remove()
        map?.let {
            mutablePolygon = it.addPolygon(PolygonOptions().apply {
                addAll(worldPolygonLatLngs)
                addHole(createCircleHole(center, points = numPointsSeekBar.progress.toDouble()))
                fillColor(fillColorArgb)
                strokeWidth(0f)
            })
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        // do nothing
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        // do nothing
    }
}