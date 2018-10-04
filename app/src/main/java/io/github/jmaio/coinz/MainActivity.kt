package io.github.jmaio.coinz

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.constants.Style
import com.mapbox.mapboxsdk.maps.MapView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    fun toast(text: String) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }
    private lateinit var mapView: MapView

    fun snackbar(text: String) {
        Snackbar.make(bottom_bar_group, text, Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(bottom_app_bar)

        Mapbox.getInstance(applicationContext, getString(R.string.app_access_token))
        mapView = findViewById(R.id.map_view)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mapboxMap ->
            map = mapboxMap
        }
        fab.setOnClickListener {
            toast("you pressed the fab!")
        }

        val Shil = Coin(R.string.curr_shil.toString(), button_shil)
        val Dolr = Coin(R.string.curr_dolr.toString(), button_dolr)
        val Quid = Coin(R.string.curr_quid.toString(), button_quid)
        val Peny = Coin(R.string.curr_peny.toString(), button_peny)

        val coins = listOf(Shil, Dolr, Quid, Peny)

        for (coin in coins) {
            coin.chip.setOnCheckedChangeListener { buttonView, isChecked ->
                snackbar("${buttonView.id} was pressed, now $isChecked")
//                toast("$buttonView was pressed, now $isChecked")
            }
        }
    }

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
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

}
