package io.github.jmaio.coinz

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Coin(
        val id: String?,
        val currency: String?,
        val value: Double?,
        var gone: Boolean = false
) : Parcelable {
    constructor() : this("", "", .0)

    fun toMap(): HashMap<String?, Any?> {
        return hashMapOf(
                id to hashMapOf(
                        "currency" to currency,
                        "value" to value,
                        "gone" to gone
                )
        )
    }
}