package io.github.jmaio.coinz

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.jetbrains.anko.AnkoLogger

@Parcelize
data class Wallet(
        val gold: Double,
        val coins: MutableList<Coin>,
        var ids: MutableSet<String>,
        val ordered: MutableList<Coin>
) : Parcelable, AnkoLogger {

    constructor() : this(0.0, mutableListOf(), mutableSetOf(), mutableListOf())

    fun init() {
        this.apply {
            setIds()
            setOrdered()
        }
    }

    fun setIds() {
        coins.forEach { c ->
            ids.add(c.id!!)
        }
    }

    fun setOrdered() {

    }

    // collect or earn coin from another player
    fun getCoin(coin: Coin) {

    }

    // exchange a coin for its gold value
    fun bankCoin(coin: Coin) {

    }

    // transfer a coin to another player
    fun donateCoin(coin: Coin, wallet: Wallet) {

    }

}