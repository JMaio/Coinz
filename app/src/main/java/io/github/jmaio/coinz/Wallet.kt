package io.github.jmaio.coinz

import android.os.Parcelable
import com.google.firebase.firestore.FieldValue
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

@Parcelize
data class Wallet(
        val id: String?,
        val gold: Double,
        val coins: MutableList<Coin>,
        var ids: MutableSet<String>,
        val ordered: MutableList<Coin>,
        val bankedToday: Int
) : Parcelable, AnkoLogger {

    constructor() : this(null, 0.0, mutableListOf(), mutableSetOf(), mutableListOf(), 0)
    constructor(id: String?, w: Wallet) : this(id, w.gold, w.coins, w.ids, w.ordered, w.bankedToday)

    @IgnoredOnParcel
    private val walletStore = WalletStore()
    @IgnoredOnParcel
    private val walletCollection = walletStore.db.collection("wallets")

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

    private fun addGold(amount: Double) {
        if (id != null)
        walletCollection.document(id)
                .update("gold", gold + amount)
                .addOnSuccessListener { info("successfully added $amount gold to $id's wallet") }
                .addOnFailureListener { e -> info("could not add $amount gold to  $id's wallet - $e") }
    }

    // collect or earn coin from another player
    fun addCoinToWallet(wildCoin: WildCoin) {
        // will try to add even if coin is already present
        if (id != null)
        walletCollection.document(id)
                .update("coins", FieldValue.arrayUnion(wildCoin.toCoin().toMap()))
                .addOnSuccessListener { info("successfully added coin ${wildCoin.properties.id} to $id's wallet") }
                .addOnFailureListener { e -> info("could not add coin ${wildCoin.properties.id} to $id's wallet - $e") }

        info("[addCoinToWallet] method complete")
    }

    // exchange a coin for its gold value
    fun bankCoin(coin: Coin) {

    }

    // transfer a coin to another player
    fun donateCoin(coinID: String, walletID: String, rates: Rates): Double {
        var g = .0
        val coin = coins.find { c ->
            c.id == coinID
        }
        info("[donateCoin] coin is ${coin?.value}, ${coin?.currency}")
        if (coin != null && !coin.gone) {
            val rate = rates.toMap()[coin.currency]!!
            // coin is in the wallet
            walletStore.getWallet(walletID) { w ->
                if (w == null) throw Exception("Receiver wallet not found!") // wallet not found
                if (w == this) throw Exception("You can't send a coin to yourself!")
                g = coin.value!! * rate
                removeCoinFromWallet(coin)
                w.addGold(g) //coin.currency)
            }
        } else throw Exception("You don't have this coin anymore!")
        return g
    }

}