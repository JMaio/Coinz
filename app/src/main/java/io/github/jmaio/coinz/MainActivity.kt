package io.github.jmaio.coinz

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.location.Location
//import android.location.LocationListener
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.*
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.expressions.Expression.get
import com.mapbox.mapboxsdk.style.layers.Property.*
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.longSnackbar
import java.lang.Exception
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), AnkoLogger, LocationEngineListener {

    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var locationComponent: LocationComponent? = null
    private lateinit var symbolManager: SymbolManager
    private var shilSource: GeoJsonSource? = null
    private var dolrSource: GeoJsonSource? = null
    private var quidSource: GeoJsonSource? = null
    private var penySource: GeoJsonSource? = null
    private var geoJsonSources = mutableMapOf<String, GeoJsonSource?>()

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

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_main)
        setSupportActionBar(bottom_app_bar)

        currencies = listOf(
                getString(R.string.curr_shil).toLowerCase(),
                getString(R.string.curr_dolr).toLowerCase(),
                getString(R.string.curr_quid).toLowerCase(),
                getString(R.string.curr_peny).toLowerCase()
        )
        geoJsonSources = mutableMapOf(
                Pair(getString(R.string.curr_shil).toLowerCase(), shilSource),
                Pair(getString(R.string.curr_dolr).toLowerCase(), dolrSource),
                Pair(getString(R.string.curr_quid).toLowerCase(), quidSource),
                Pair(getString(R.string.curr_peny).toLowerCase(), penySource)
        )


        user = fbAuth.currentUser
        if (user?.email != null) userDisplay = user?.email.toString()

        createOnClickListeners()

        coinzmapFile = "${this.filesDir.absolutePath}/${getString(R.string.coinmap_filename)}"

        mapView = findViewById(R.id.map_view)
        mapView?.onCreate(savedInstanceState)
        info("[onCreate] Mapbox object setup complete")

        // asynchronously load the map
        mapView?.getMapAsync { mapboxMap ->
            map = mapboxMap.apply {
                // set map bound and zoom prefs
                setLatLngBoundsForCameraTarget(centralBounds)

                // add a currency marker layer for each
                currencies.forEach { c ->
                    val markerRes = resources.getIdentifier("marker_${c.toLowerCase()}", "drawable", packageName)
                    val icon = BitmapFactory.decodeResource(resources, markerRes)
                    addImage("marker_${c.toLowerCase()}", icon)
                }

                addOnMapClickListener { l ->
                    val screenPoint = projection.toScreenLocation(l)
                    val features = queryRenderedFeatures(screenPoint,
                            "shil_layer", "dolr_layer", "quid_layer", "peny_layer")
                    if (!features.isEmpty()) {
                        val selectedFeature = features[0]
                        val id = selectedFeature.getStringProperty("id")
                        collectCoinFromMap(id)
                    }
                }
            }

            doAsync { fetchCoinMap() }

            val locationComponentOptions = LocationComponentOptions.builder(this)
                    .minZoom(14.5)
                    .maxZoom(18.0)
                    .build()
            locationComponent = map!!.locationComponent
            info("[getMapAsync] created locationComponent")

            try {
                locationComponent!!.apply {
                    info("[getMapAsync] enabling location")
                    activateLocationComponent(this@MainActivity, locationComponentOptions)
                    isLocationComponentEnabled = true
                    renderMode = RenderMode.NORMAL
                    cameraMode = CameraMode.TRACKING
                    locationEngine?.apply {
                        requestLocationUpdates()
                        addLocationEngineListener(this@MainActivity)
                    }
                }
            } catch (e: SecurityException) {
                alert {
                    title = "Please enable location!"
                    message = getString(R.string.location_explanation)
                    isCancelable = false
                }.show()
            } catch (e: Exception) {
                info("location engine definition error: $e")
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

    @SuppressLint("MissingPermission")
    override fun onConnected() {
//        locationComponent.locationEngine?.requestLocationUpdates()
    }

    override fun onLocationChanged(location: Location?) {
        val closeCoins = arrayListOf<WildCoin>()
        coinMap?.coins?.forEach { wildCoin ->
            if (wildCoin.asLatLng().distanceTo(LatLng(location)) < 25) {
                closeCoins.add(wildCoin)
            }
        }
        closeCoins.forEach { coin ->
            collectCoinFromMap(coin)
        }
    }

    private fun fetchCoinMap() {
        main_view.longSnackbar("Fetching coin map...")
        val today = LocalDateTime.now()
        val dateString = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH).format(today)
        val maker = CoinMapMaker()
        // if map is already today's map
        if (dateString == downloadDate && !coinzDebugMode) {
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
                addMarkerLayers()
            } else {
                main_view.indefiniteSnackbar("Could not fetch map! Please check your connection.", "retry?") {
                    doAsync {
                        fetchCoinMap()
                    }
                }
            }
        }

    }

    private fun addMarkerLayers() {
        info("[addMarkerLayers] map = $map")
        geoJsonSources.forEach { (curr, _) ->
            geoJsonSources[curr] = GeoJsonSource("${curr.toLowerCase()}_source", coinMap!!.toGeoJson(curr))
            map?.addSource(geoJsonSources[curr]!!)
            val sl = SymbolLayer("${curr.toLowerCase()}_layer", "${curr.toLowerCase()}_source")
                    .withProperties(
                            iconImage("marker_${curr.toLowerCase()}"),
                            iconAnchor(ICON_ANCHOR_CENTER),
                            iconAllowOverlap(true),
                            iconOpacity(get("icon-opacity")),
                            visibility(VISIBLE)
                    )
            map?.addLayer(sl)
        }
    }

    private fun showMarkers(c: String) {
        info("[addMarkers] map = $map")
        info("[removeMarkers] removing markers for currency: $c")
        map?.getLayer("${c.toLowerCase()}_layer")?.setProperties(
                visibility(VISIBLE)
        )
    }

    private fun hideMarkers(c: String) {
        info("[removeMarkers] removing markers for currency: $c")
        map?.getLayer("${c.toLowerCase()}_layer")?.setProperties(
                visibility(NONE)
        )
    }

    private fun removeMarkers() {
        info("[removeMarkers] map = $map")
        map?.clear()
    }

    private fun removeMarkerByID(id: String) {
        info("[removeMarker] removing markers for coin id: $id")
        map?.markers?.filter { marker ->
            marker.snippet == id
        }?.forEach { marker ->
            map?.removeMarker(marker)
        }
    }

    private fun collectCoinFromMap(wildCoin: WildCoin) {
        val curr = wildCoin.properties.currency.toLowerCase()
        val source = geoJsonSources[curr]
        toast("Coin collected!\n(${curr.toUpperCase()} ${wildCoin.properties.value})")
        coinMap?.collectCoin(wildCoin)
        source!!.setGeoJson(coinMap?.toGeoJson(curr))
    }

    private fun collectCoinFromMap(id: String) {
        try {
            val coin = coinMap!!.coins.find { wildCoin -> wildCoin.properties.id == id }!!
            collectCoinFromMap(coin)
        } catch (e: Exception) {
            info("could not collect coin $id")
        }
    }

    private fun createOnClickListeners() {
        fab.setOnClickListener {
            startActivity(Intent(this, WalletActivity::class.java))
        }

        bottom_app_bar.setNavigationOnClickListener {
            startActivity(Intent(this, BankActivity::class.java).putExtra("coinMap", coinMap))
        }

        val buttons = listOf(button_shil, button_dolr, button_quid, button_peny)

        // map markers
        buttons.forEach { btn ->
            btn.setOnCheckedChangeListener { _, isChecked ->
                toast("${btn.text} was pressed, now $isChecked")
                debug("[coinButton] button '$btn' pressed --> $isChecked")
                if (isChecked) {
                    showMarkers(btn.text.toString())
                } else {
                    hideMarkers(btn.text.toString())
                }
            }
        }

        // show user id at the top of the screen
        user_id_chip.apply {
            text = userDisplay
            setOnClickListener {
                info("[user_id_chip] pressed")
                alert {
                    title = "Log Out?"
                    message = "Currently logged in as $userDisplay.\nContinue?"
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
