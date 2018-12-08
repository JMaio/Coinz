package io.github.jmaio.coinz

data class Wallet (
        val gold: Double? = 0.0,
        val coins: MutableList<Coin>?
) {

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