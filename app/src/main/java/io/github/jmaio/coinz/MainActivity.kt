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

    fun snackbar(text: String) {
        Snackbar.make(bottom_bar_group, text, Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Mapbox.getInstance(applicationContext, getString(R.string.app_access_token))

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        setSupportActionBar(bottom_app_bar)

        mapView.getMapAsync {
        it.setStyle(Style.MAPBOX_STREETS)
        fab.setOnClickListener {
            toast("you pressed the fab!")
        }

        val Shil = Coin(applicationContext, "SHIL", button_shil)
        val Dolr = Coin(applicationContext, "DOLR", button_dolr)
        val Quid = Coin(applicationContext, "QUID", button_quid)
        val Peny = Coin(applicationContext, "PENY", button_peny)

        val coins = listOf(Shil, Dolr, Quid, Peny)

        for (coin in coins) {
            coin.chip.setOnCheckedChangeListener { buttonView, isChecked ->
                snackbar("${buttonView.id} was pressed, now $isChecked")
//                toast("$buttonView was pressed, now $isChecked")
            }
        }

        setSupportActionBar(bottom_app_bar)
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
        map_view.onStart()
    }

    override fun onResume() {
        super.onResume()
        map_view.onResume()
    }

    override fun onPause() {
        super.onPause()
        map_view.onPause()
    }

    override fun onStop() {
        super.onStop()
        map_view.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        map_view.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map_view.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        map_view.onDestroy()
    }

}
