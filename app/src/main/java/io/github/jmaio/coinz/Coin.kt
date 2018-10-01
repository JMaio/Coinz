package io.github.jmaio.coinz

import android.content.Context
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import io.github.jmaio.coinz.R.color as CoinzColor

class Coin(name : String, val chip: Chip) {
    var visible = true

    val color = when(name) {
        "SHIL" -> CoinzColor.colorShil
        "DOLR" -> CoinzColor.colorDolr
        "QUID" -> CoinzColor.colorQuid
        "PENY" -> CoinzColor.colorPeny
        else -> CoinzColor.colorDisabled
    }

    fun setDayMarketValue() {
        // TODO: set this to be dynamically allocated
        var value = ""
    }

    fun toggleVisibility() {
        chip.setOnCheckedChangeListener { _, isChecked ->
            MainActivity().toast("Shil was pressed, it's now $isChecked! ")
        }
    }
}