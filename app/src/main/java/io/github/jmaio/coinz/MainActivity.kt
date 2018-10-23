package io.github.jmaio.coinz

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class MainActivity : AppCompatActivity(), PermissionsListener, LocationEngineListener,
        OnMapReadyCallback, AnkoLogger {

    private var mapView: MapView? = null
    private var map: MapboxMap? = null

    private lateinit var originLocation: Location

    private lateinit var permissionsManager: PermissionsManager
    private lateinit var locationEngine: LocationEngine
    private lateinit var locationLayerPlugin: LocationLayerPlugin

    private val CENTRAL_BOUNDS = LatLngBounds.Builder()
            .include(LatLng(-3.192473, 55.946233))
            .include(LatLng(-3.184319, 55.942617))
            .build()

    private val MARKER_SOURCE = "markers-source"
    private val MARKER_STYLE_LAYER = "markers-style-layer"
    private val MARKER_IMAGE = "custom-marker"

    private var downloadDate = "" // Format: YYYY/MM/DD
    private val DEBUG_MODE = true

    private lateinit var coinMap: CoinMap
    private lateinit var coinzmapFile: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(bottom_app_bar)

        // setup mapbox
        Mapbox.getInstance(applicationContext, getString(R.string.mapbox_access_token))

        mapView = findViewById(R.id.map_view)

        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
        info("[onCreate] Mapbox object setup")

        createOnClickListeners()
        info("[onCreate] created button press listeners")

        coinzmapFile = "${act.filesDir.absolutePath}/${getString(R.string.coinmap_filename)}"
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()

        mapView?.onStart()

        val settings = getSharedPreferences(getString(R.string.preferences_file), Context.MODE_PRIVATE)
        // use ”” as the default value (this might be the first time the app is run)
        downloadDate = settings.getString("lastDownloadDate", "")
        info("[onStart] last map load date = '$downloadDate'")

        fetchCoinMap()
//        coinMap = CoinMap().apply { loadMapFromFile(coinzmapFile) }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()

        locationEngine.removeLocationUpdates()

        mapView?.onStop()

        val settings = getSharedPreferences(getString(R.string.preferences_file), Context.MODE_PRIVATE)
        info("[onStop] setting last download date --> '$downloadDate'")
        val editor = settings.edit()
        editor.putString("lastDownloadDate", downloadDate)
        editor.apply()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
        locationEngine.deactivate()
    }

    private fun fetchCoinMap() {
        // TODO download today's file and set the date
        val today = LocalDateTime.now()
        val dateString = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH).format(today)

        // if map is already today's map
        if (dateString == downloadDate && !DEBUG_MODE) {
            info("[fetchCoinMap]: dateString = $dateString = downloadDate - halting...")
            // return if downloadDate
            return
        }

        // make url from date pattern
        info("[fetchCoinMap]: dateString = $dateString")
        val url = "${getString(R.string.map_repo)}/$dateString/${getString(R.string.coinmap_filename)}"
        info("[fetchCoinMap]: url = $url")


        val coinMapDownloader = DownloadFileTask(url, coinzmapFile)
        coinMapDownloader.execute()
        coinMap = CoinMap().apply { loadMapFromFile(coinzmapFile) }

        downloadDate = dateString

    }

    private fun createOnClickListeners() {
        fab.setOnClickListener {
            toast("you pressed the fab!")
        }

        val shilBtn = CoinButton(applicationContext, getString(R.string.curr_shil), button_shil)
        val dolrBtn = CoinButton(applicationContext, getString(R.string.curr_dolr), button_dolr)
        val quidBtn = CoinButton(applicationContext, getString(R.string.curr_quid), button_quid)
        val penyBtn = CoinButton(applicationContext, getString(R.string.curr_peny), button_peny)

    }

    // when map is ready, set map panning bounds
    override fun onMapReady(mapboxMap: MapboxMap?) {
        if (mapboxMap == null) {
            info("[onMapReady] mapboxMap null!")
        } else {
            debug("[onMapReady] mapboxMap found")
            map = mapboxMap

            val icon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.custom_marker)
            map?.addImage(MARKER_IMAGE, icon)
            enableLocation()
            addMarkers()
        }
    }

    private fun addMarkers() {
        var features: List<Feature> = ArrayList()

        for (wildCoin in coinMap.coins) {
//                features += wildCoin.asFeature()
            map?.addMarker(MarkerOptions()
                    .position(wildCoin.asLatLng())
                    .title(wildCoin.properties.markerSymbol.toString()))

        }
        info("[mapbox] added eiffel tower ---")
        map?.addMarker(MarkerOptions()
                .position(LatLng(48.85819, 2.29458))
                .title("Eiffel Tower"))
//        map?.addSource(source)


        val markerStyleLayer = SymbolLayer(MARKER_STYLE_LAYER, MARKER_SOURCE)
                .withProperties(
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconImage(MARKER_IMAGE)
                )
        map?.addLayer(markerStyleLayer)

    }

    private fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            debug("Location Permission [ON]")
            initializeLocationEngine()
            initializeLocationLayer()
        } else {
            debug("Location Permission [OFF]")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initializeLocationEngine() {
        locationEngine = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
        locationEngine.apply {
            interval = 5000
            fastestInterval = 1000
            priority = LocationEnginePriority.HIGH_ACCURACY
            activate()
        }

        val lastLocation = locationEngine.lastLocation

        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        } else {
            locationEngine.addLocationEngineListener(this)
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initializeLocationLayer() {
        if (mapView == null) {
            debug("mapView is null")
        } else {
            if (map == null) {
                debug("map is null")
            } else {
                locationLayerPlugin = LocationLayerPlugin(mapView!!, map!!, locationEngine)
                locationLayerPlugin.apply {
                    isLocationLayerEnabled = true // setLocationLayerEnabled(true)
                    cameraMode = CameraMode.TRACKING
                    renderMode = RenderMode.NORMAL
                }
            }
        }
    }

    private fun setCameraPosition(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        map?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    override fun onLocationChanged(location: Location?) {
        if (location == null) {
            debug("[onLocationChanged] location is null")
        } else {
            originLocation = location
            setCameraPosition(originLocation)
        }
    }

    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        debug("[onConnected] requesting location updates")
        locationEngine.requestLocationUpdates()
    }

    // mapbox / permissions
    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        debug("Permissions: $permissionsToExplain")
        // toast or dialog to explain access
        if (permissionsToExplain != null) {
            for (permission in permissionsToExplain) {
                toast(permission)
            }
        }
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocation()
        } else {
            onExplanationNeeded(mutableListOf(getString(R.string.location_explanation)))
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // after mapbox
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.bottomappbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.app_bar_settings -> toast("Settings item is clicked!")
            android.R.id.home -> toast("you pressed the bank button!")
        }
        return true
    }

}
