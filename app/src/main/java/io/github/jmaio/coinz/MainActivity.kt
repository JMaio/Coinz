package io.github.jmaio.coinz

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.indefiniteSnackbar
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : AppCompatActivity(), AnkoLogger {

    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private lateinit var locationComponent: LocationComponent

    private val fbAuth: FirebaseAuth = FirebaseAuth.getInstance()
    val user = fbAuth.currentUser

    private val CENTRAL_BOUNDS = LatLngBounds.Builder()
            .include(LatLng(55.946233, -3.192473))
            .include(LatLng(55.942617, -3.184319))
            .build()

    private val MARKER_SOURCE = "markers-source"
    private val MARKER_STYLE_LAYER = "markers-style-layer"
    private val MARKER_IMAGE = "custom-marker"

    private var downloadDate = "" // Format: YYYY/MM/DD
    private val DEBUG_MODE = false

    private var coinMap: CoinMap = CoinMap()
    private lateinit var coinzmapFile: String

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
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

        // asynchronously fetch coin map, then load the map
        mapView?.getMapAsync { mapboxMap ->
            map = mapboxMap.apply {
                // set map bound and zoom prefs
                setMinZoomPreference(14.5)
                setMaxZoomPreference(18.0)
                setLatLngBoundsForCameraTarget(CENTRAL_BOUNDS)
            }
            doAsync {
                // fetch coin map asynchronously
                fetchCoinMap()
                uiThread {
                    // add markers on ui thread after fetching
                    addMarkers()
                }
            }
            locationComponent = map!!.locationComponent
            info("[getMapAsync] created locationComponent")
            enableLocation()
        }

        // show user id at the top of the screen
        if (intent.extras != null) {
            user_id_chip.text = intent.getStringExtra("id")
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
                main_view.indefiniteSnackbar("Could not fetch map! Please check your connection.", "retry?") {
                    fetchCoinMap()
                }
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

        user_id_chip.setOnClickListener { view ->
            info("[user_id_chip] pressed")
            alert {
                title = "Log Out?"
                message = "Currently logged in as ${user?.email}.\nContinue?"
                yesButton {
                    fbAuth.signOut()
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    this@MainActivity.finish()
                }
                noButton {}
            }.show()
        }

        info("[onCreate] created button press listeners")
    }

    // requires locationComponent to ensure it only runs if such a component exists
    private fun enableLocation() {
        info("[location] enabling location")
        try {
            locationComponent.apply {
                activateLocationComponent(this@MainActivity, true)
                isLocationComponentEnabled = true
                renderMode = RenderMode.NORMAL
                cameraMode = CameraMode.TRACKING
            }
        } catch (e: SecurityException)  {
            alert {
                title = "Please enable location!"
                message = getString(R.string.location_explanation)
                isCancelable = false
            }.show()
        }
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
