package io.github.jmaio.coinz

import org.jetbrains.anko.AnkoLogger

data class Wallet(
        val gold: Double,
        val coins: MutableList<Coin>,
        var ids: MutableSet<String>
) : AnkoLogger {

    constructor() : this(0.0, mutableListOf(), mutableSetOf())

    fun setIds() {
        coins.forEach { c ->
            ids.add(c.id!!)
        }
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