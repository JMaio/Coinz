package io.github.jmaio.coinz

import android.content.Context
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import io.github.jmaio.coinz.R.color as CoinzColor

class Coin(name : String, val chip: Chip) {
    var visible = true

    val color = when(name) {
        R.string.curr_shil.toString() -> CoinzColor.colorShil
        R.string.curr_dolr.toString() -> CoinzColor.colorDolr
        R.string.curr_quid.toString() -> CoinzColor.colorQuid
        R.string.curr_peny.toString() -> CoinzColor.colorPeny
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