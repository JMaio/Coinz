package io.github.jmaio.coinz

import android.content.Context
import android.content.res.Resources
import com.google.android.material.chip.Chip
import org.jetbrains.anko.toast
import io.github.jmaio.coinz.R.color as CoinzColor

class CoinButton(context: Context, val name: String, val chip: Chip) {
    var visible = true

    val color = when(name) {
         context.getString(R.string.curr_shil) -> CoinzColor.colorShil
         context.getString(R.string.curr_dolr) -> CoinzColor.colorDolr
         context.getString(R.string.curr_quid) -> CoinzColor.colorQuid
         context.getString(R.string.curr_peny) -> CoinzColor.colorPeny
        else -> CoinzColor.colorDisabled
    }
    
    val toggle = chip.setOnCheckedChangeListener { buttonView, isChecked ->
        context.toast("${name} was pressed, now $isChecked")
        toggleVisibility()
    }
    
    fun toggleVisibility() {
//        TODO("toggle coin markers visibility")
    }
}