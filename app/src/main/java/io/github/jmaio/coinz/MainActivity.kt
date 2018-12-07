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

        currencies = listOf(
                getString(R.string.curr_shil),
                getString(R.string.curr_dolr),
                getString(R.string.curr_quid),
                getString(R.string.curr_peny)
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
                if (coinzDebugMode) {
                    setOnMarkerClickListener { marker ->
                        collectCoinFromMapByID(marker.snippet)
                        true
                    }
                }
                // add a currency marker layer for each
                currencies.forEach { c ->
                    val markerRes = resources.getIdentifier("marker_${c.toLowerCase()}", "drawable", packageName)
                    val icon = BitmapFactory.decodeResource(resources, markerRes)
                    addImage("marker_${c.toLowerCase()}", icon)
                }
            }

            doAsync {
                fetchCoinMap()
                // "Map interactions should happen on the UI thread."

            }

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
        toast("location changed OMG!")
    }

//
//    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun onProviderEnabled(provider: String?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun onProviderDisabled(provider: String?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }


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
        currencies.forEach { c ->
            val source = coinMap!!.toGeoJson(c)
            map?.addSource(source)
            val s = SymbolLayer("${c.toLowerCase()}_layer", "${c.toLowerCase()}_source")
                    .withProperties(
                            iconImage("marker_${c.toLowerCase()}"),
                            iconAnchor(ICON_ANCHOR_CENTER),
                            iconAllowOverlap(true),
                            iconOpacity(get("icon-opacity")),
                            visibility(VISIBLE)
                    )
            map?.addLayer(s)
        }
    }



//    private fun addMarkers() {
//        info("[addMarkers] map = $map")
//        currencies.forEach { c ->
//            addMarkers(c)
//        }
//    }
//
//    private fun addMarkers(c: String) {
//        info("[addMarkers] adding markers for currency: $c")
//        // Create an Icon object for the marker to use
//        val markerRes = resources.getIdentifier("marker_${c.toLowerCase()}", "drawable", packageName)
//        val icon = IconFactory.getInstance(this).fromResource(markerRes)
//        if (coinMap != null) {
//
//            val symbolOptionsList = ArrayList<SymbolOptions>()
//            coinMap!!.coins.filter { coin -> coin.properties.currency == c }.forEach { wildCoin ->
//                val s = SymbolOptions()
//                        .withLatLng(wildCoin.asLatLng())
//                        .withIconImage("marker_${c.toLowerCase()}")
//                        .withIconSize(1.3f)
//                symbolOptionsList.add(s)
////                symbolManager.create(s)
//            }
//            info("symbol list created: size ${symbolOptionsList.size}")
//            symbolManager.create(symbolOptionsList)
////                for (wildCoin in coinMap!!.coins.filter { coin -> coin.properties.currency == c }) {
////                    map?.addMarker(MarkerViewOptions()
////                            .position(wildCoin.asLatLng())
////                            // store the currency
////                            .title(wildCoin.properties.currency)
////                            // and the id
////                            .snippet(wildCoin.properties.id)
////                            .icon(icon)
//////                        .alpha((ceil(wildCoin.properties.markerSymbol.toDouble() / 5) / 2).toFloat())
////                            .anchor(.5f, .5f)
////                    )
////                }
//        }


    private fun showMarkers(c: String) {
        info("[addMarkers] map = $map")
        info("[removeMarkers] removing markers for currency: $c")
        map?.getLayer("${c.toLowerCase()}_layer")?.setProperties(
                visibility(VISIBLE)
        )
    }

    private fun removeMarkers() {
        info("[removeMarkers] map = $map")
        map?.clear()
    }

    private fun hideMarkers(c: String) {
        info("[removeMarkers] removing markers for currency: $c")
        map?.getLayer("${c.toLowerCase()}_layer")?.setProperties(
                visibility(NONE)
        )
//                markers?.filter { marker ->
//            marker.title == c
//        }?.forEach { marker ->
//            map?.removeMarker(marker)
//        }
    }

    private fun removeMarkerByID(id: String) {
        info("[removeMarker] removing markers for coin id: $id")
//        map?.layers?.forEach {
//            it.id
//        }
        map?.markers?.filter { marker ->
            marker.snippet == id
        }?.forEach { marker ->
            map?.removeMarker(marker)
        }
    }

    private fun collectCoinFromMapByID(id: String) {
        coinMap?.collectCoin(id)
        removeMarkerByID(id)
    }

    private fun collectCoinFromMap(wildCoin: WildCoin) {
        toast("Coin collected! (${wildCoin.properties.currency} ${wildCoin.properties.value})")
        collectCoinFromMapByID(wildCoin.properties.id)
    }

//    @SuppressLint("MissingPermission")
//    override fun onConnected() {
//    }
//
//    override fun onLocationChanged(location: Location?) {
//        toast("location changed?")
//        val l = LatLng(location)
//        coinMap?.coins?.forEach { wildCoin ->
//            // if distance to coin is below 25 metres
//            if (l.distanceTo(wildCoin.asLatLng()) < 25) {
//                collectCoinFromMap(wildCoin)
//            }
//        }
//    }

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
