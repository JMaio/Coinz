package io.github.jmaio.coinz

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_bank.*
import org.jetbrains.anko.toast

class BankActivity : AppCompatActivity() {

    private var coinMap: CoinMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coinMap = intent.extras?.getParcelable("coinMap")
        setContentView(R.layout.activity_bank)
//        setSupportActionBar(toolbar)
        setupActionBar()
        setupRates()
    }

    private fun setupActionBar() {
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRates() {
        val transl = mapOf(
                Pair("SHIL", Pair(shil_curr_value_units, shil_curr_value_decimals)),
                Pair("DOLR", Pair(dolr_curr_value_units, dolr_curr_value_decimals)),
                Pair("QUID", Pair(quid_curr_value_units, quid_curr_value_decimals)),
                Pair("PENY", Pair(peny_curr_value_units, peny_curr_value_decimals))
        )
        if (coinMap != null) {
            transl.forEach { (curr, fields) ->
                val r = coinMap?.rates?.toMap()?.get(curr).toString().split(".")
                try {
                    fields.apply {
                        first.text = r[0]
                        second.text = r[1].substring(0, 2)
                    }
                } catch (e: Exception) {
                    toast("Could not get today's rates. Error: ${e.message}")
                }
            }
//            coinMap?.rates?.toMap()?.forEach {
//                val units = findViewById(R.id."shil_curr_value_units")
//                shil_curr_value_decimals
//            }
//            r.SHIL
        }
    }

}
