package io.github.jmaio.coinz

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentOptions
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
    private var originLocation: Location? = null

    private val fbAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var user: FirebaseUser? = null
    private var userDisplay = "defaultUser"

    private val centralBounds = LatLngBounds.Builder()
            .include(LatLng(55.946233, -3.192473))
            .include(LatLng(55.942617, -3.184319))
            .build()

    private var downloadDate = "" // Format: YYYY/MM/DD
    private var coinzDebugMode = true

    private var coinMap: CoinMap? = null
    private lateinit var coinzmapFile: String

    private lateinit var currencies: List<String>

//    public lateinit var wallet: Wallet

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_main)
        setSupportActionBar(bottom_app_bar)

        user = fbAuth.currentUser
        if (user?.email != null) userDisplay = user?.email.toString()

        createOnClickListeners()

        coinzmapFile = "${this.filesDir.absolutePath}/${getString(R.string.coinmap_filename)}"

        mapView = findViewById(R.id.map_view)
        mapView?.onCreate(savedInstanceState)
        info("[onCreate] Mapbox object setup complete")

        // asynchronously fetch coin map, then load the map
        mapView?.getMapAsync { mapboxMap ->
            map = mapboxMap.apply {
                // set map bound and zoom prefs
                setLatLngBoundsForCameraTarget(CENTRAL_BOUNDS)
                setOnMarkerClickListener { marker ->
                    collectCoinFromMap(marker.snippet)
                    toast("${marker.snippet} collected! coinMap now has ${coinMap?.coins?.size} coins")
                    true
                }
            }

            doAsync {
                fetchCoinMap()
                // "Map interactions should happen on the UI thread."
                uiThread {
                    addMarkers()
                }
            }

            val locationComponentOptions = LocationComponentOptions.builder(this)
                    .minZoom(14.5)
                    .maxZoom(18.0)
                    .build()
            locationComponent = map!!.locationComponent
            info("[getMapAsync] created locationComponent")

            try {
                locationComponent.apply {
                    info("[getMapAsync] enabling location")
                    activateLocationComponent(this@MainActivity, locationComponentOptions)
                    isLocationComponentEnabled = true
                    renderMode = RenderMode.NORMAL
                    cameraMode = CameraMode.TRACKING
                }
                trackLocation()
            } catch (e: SecurityException)  {
                alert {
                    title = "Please enable location!"
                    message = getString(R.string.location_explanation)
                    isCancelable = false
                }.show()
            }
        }
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

    fun fetchCoinMap() {
        val today = LocalDateTime.now()
        val dateString = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH).format(today)
        val maker = CoinMapMaker()
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
        coinMap = maker.loadMapFromFile(coinzmapFile)

        info("[fetchCoinMap]: map loaded : $coinMap")
        runOnUiThread {
            if (coinMap != null && !coinMap!!.isEmpty()) {
                longToast("Map loaded successfully!")
                downloadDate = dateString
            } else {
                main_view.indefiniteSnackbar("Could not fetch map! Please check your connection.", "retry?") {
                    fetchCoinMap()
                }
            }
        }

    }

    fun addMarkers() {
        info("[addMarkers] map = $map")
        listOf(
                getString(R.string.curr_shil),
                getString(R.string.curr_dolr),
                getString(R.string.curr_quid),
                getString(R.string.curr_peny)
        ).forEach { c ->
            addMarkers(c)
        }
    }

    fun addMarkers(c: String) {
        info("[addMarkers] adding markers for currency: $c")
        if (coinMap != null) {
            for (wildCoin in coinMap!!.coins.filter { coin -> coin.properties.currency == c }) {
                map?.addMarker(MarkerOptions()
                        .position(wildCoin.asLatLng())
                        // store the currency
                        .title(wildCoin.properties.currency)
                        // and the id
                        .snippet(wildCoin.properties.id)
                )
            }
        }
    }
    fun removeMarkers() {
        info("[removeMarkers] map = $map")
        map?.clear()
    }

    fun removeMarkers(c: String) {
        info("[removeMarkers] removing markers for currency: $c")
        map?.markers?.filter { marker ->
            marker.title == c
        }?.forEach { marker ->
            map?.removeMarker(marker)
        }
    }
    fun removeMarkerByID(id: String) {
        info("[removeMarker] removing markers for currency: $id")
        map?.markers?.filter { marker ->
            marker.snippet == id
        }?.forEach { marker ->
            map?.removeMarker(marker)
        }
    }

    fun collectCoinFromMap(id: String) {
        coinMap?.collectCoin(id)
        removeMarkerByID(id)
    }

    private fun trackLocation() {

    }

    private fun createOnClickListeners() {
        fab.setOnClickListener {
            startActivity(Intent(this, WalletActivity::class.java))
        }

        bottom_app_bar.setNavigationOnClickListener {
            startActivity(Intent(this, BankActivity::class.java).
                    putExtra("coinMap", coinMap))
        }

        val buttons = listOf(button_shil, button_dolr, button_quid, button_peny)

        // map markers
        buttons.forEach { btn ->
            btn.setOnCheckedChangeListener { buttonView, isChecked ->
                toast("${btn.text} was pressed, now $isChecked")
                debug("[coinButton] button '$btn' pressed --> $isChecked")
                if (isChecked) {
                    addMarkers(btn.text.toString())
                } else {
                    removeMarkers(btn.text.toString())
                }
            }
        }

        // show user id at the top of the screen
        user_id_chip.apply {
            text = userDisplay
            setOnClickListener { view ->
                info("[user_id_chip] pressed")
                alert {
                    title = "Log Out?"
                    message = "Currently logged in as ${userDisplay}.\nContinue?"
                    yesButton {
                        fbAuth.signOut()
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        this@MainActivity.finish()
                    }
                    noButton {}
                }.show()
            }
        }

        info("[onCreate] created button press listeners")
    }

    // after mapbox
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.bottomappbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        info("clicked $item, id: ${item?.itemId}")
        when (item!!.itemId) {
            R.id.app_bar_settings -> startActivity(Intent(this, SettingsActivity::class.java))
        }
        return true
    }

}
