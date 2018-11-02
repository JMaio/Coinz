package io.github.jmaio.coinz

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

class MainActivity : AppCompatActivity(), AnkoLogger, PermissionsListener {

    private var mapView: MapView? = null
    private var map: MapboxMap? = null

    private lateinit var permissionsManager: PermissionsManager

    private val CENTRAL_BOUNDS = LatLngBounds.Builder()
            .include(LatLng(-3.192473, 55.946233))
            .include(LatLng(-3.184319, 55.942617))
            .build()

    private val MARKER_SOURCE = "markers-source"
    private val MARKER_STYLE_LAYER = "markers-style-layer"
    private val MARKER_IMAGE = "custom-marker"

    private var downloadDate = "" // Format: YYYY/MM/DD
    private val DEBUG_MODE = false

    private var coinMap: CoinMap = CoinMap()
    private lateinit var coinzmapFile: String

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Coinz)
        super.onCreate(savedInstanceState)

        info("[onCreate] -- coinMap empty? ${coinMap.isEmpty()}")

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_main)
        setSupportActionBar(bottom_app_bar)

        coinzmapFile = "${this.filesDir.absolutePath}/${getString(R.string.coinmap_filename)}"

        mapView = findViewById(R.id.map_view)
        mapView?.onCreate(savedInstanceState)
        info("[onCreate] Mapbox object setup complete")

        enableLocationPermissions()

        // asynchronously fetch coin map, then load the map
        mapView?.getMapAsync { mapboxMap ->
            map = mapboxMap
            doAsync {
                fetchCoinMap()
                uiThread {
                    addMarkers()
                }
            }
//            addMarkers()
        }

        createOnClickListeners()
    }


    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()

        val settings = getSharedPreferences(getString(R.string.preferences_file), Context.MODE_PRIVATE)
        // use ”” as the default value (this might be the first time the app is run)
        downloadDate = settings.getString("lastDownloadDate", "")
        info("[onStart] last map load date = '$downloadDate'")

        mapView?.onStart()
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
    }

    private fun fetchCoinMap() {
        val today = LocalDateTime.now()
        val dateString = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH).format(today)

        // if map is already today's map
        if (dateString == downloadDate && !DEBUG_MODE) {
            info("[fetchCoinMap]: dateString = $dateString = downloadDate - loading...")
        } else {
            // make url from date pattern
            info("[fetchCoinMap]: dateString = $dateString")
            val url = "${getString(R.string.map_repo)}/$dateString/${getString(R.string.coinmap_filename)}"
            info("[fetchCoinMap]: url = $url")

            val coinMapDownloader = DownloadFileTask(url, coinzmapFile)
            coinMapDownloader.execute()
        }

        coinMap.apply { loadMapFromFile(coinzmapFile) }

        info("[fetchCoinMap]: map loaded : $coinMap")
        runOnUiThread {
            if (!coinMap.isEmpty()) {
                longToast("Map loaded successfully!")
                downloadDate = dateString
            } else {
                longToast("Could not fetch map! Please check your connection.")
            }
        }

    }

    private fun addMarkers() {
        info("[addMarkers] map = $map")
        if (!coinMap.isEmpty()) {
            for (wildCoin in coinMap.coins) {
                map?.addMarker(MarkerOptions()
                        .position(wildCoin.asLatLng())
                        .title(wildCoin.properties.markerSymbol.toString()))
            }
            info("[addMarkers] added coin markers ---")
        } else {
            info("[addMarkers] coinMap is empty! no markers...")
        }

    }

    private fun createOnClickListeners() {
        fab.setOnClickListener {
            toast("you pressed the fab!")
        }

        val shilBtn = CoinButton(applicationContext, getString(R.string.curr_shil), button_shil)
        val dolrBtn = CoinButton(applicationContext, getString(R.string.curr_dolr), button_dolr)
        val quidBtn = CoinButton(applicationContext, getString(R.string.curr_quid), button_quid)
        val penyBtn = CoinButton(applicationContext, getString(R.string.curr_peny), button_peny)

        info("[onCreate] created button press listeners")
    }

    private fun enableLocation() {
        try {
            map?.locationComponent?.apply {
                activateLocationComponent(this@MainActivity)
                isLocationComponentEnabled = true
            }
        } catch (e: SecurityException) {
            enableLocationPermissions()
        }
    }

    private fun enableLocationPermissions(): Boolean {
        val permissionGranted = ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (permissionGranted) {
            info("[enableLocation] Location Permission [ON]")
//            checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(), Process.myUid())
        } else {
            info("[enableLocation] Location Permission [OFF] -- requesting")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
        return permissionGranted
    }

// mapbox / permissions
    override fun onExplanationNeeded(permsToExplain: MutableList<String>?) {
//        info("Permissions: $permsToExplain")
//        if (permsToExplain != null) {
//            main_view.snackbar("Action, reaction", "Click me!") { enableLocationPermissions() }
//        }
//        if (!PermissionsManager.areLocationPermissionsGranted(this)) {
//            longToast(R.string.location_explanation)
//            main_view.snackbar("Action, reaction", "Click me!") { enableLocationPermissions() }
//        }
        // toast or dialog to explain access
//        if (permsToExplain != null) {
        ////            for (perm in permsToExplain) {
//            }
//        }
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocation()
        } else {
            alert {
                title = "Please enable location!"
                message = getString(R.string.location_explanation)
                yesButton { enableLocationPermissions() }
                noButton { }
            }.show()
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
            R.id.app_bar_settings -> toast("you pressed the settings button!")
            R.id.home -> toast("you pressed the bank button!")
        }
        return true
    }

}
