package io.github.jmaio.coinz

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Coin(
        val id: String = "",
        val currency: String = "",
        val value: Double = .0,
        var gone: Boolean = false
) : Parcelable {

    fun toMap(): HashMap<String?, Any?> {
        return hashMapOf(
                id to hashMapOf(
                        "id" to id,
                        "currency" to currency,
                        "value" to value,
                        "gone" to gone
                )
        )
    }
}