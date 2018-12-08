package io.github.jmaio.coinz

data class Coin(
        val id: String?,
        val currency: String?,
        val value: Double?
) {
    constructor(): this("", "", .0)

    fun toMap(): HashMap<String?, Any?> {
        return hashMapOf(
                Pair("id", id),
                Pair("currency", currency),
                Pair("value", value)
        )
    }
}