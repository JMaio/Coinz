package io.github.jmaio.coinz

import android.os.Parcelable
import com.google.firebase.firestore.SetOptions
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

@Parcelize
data class Wallet(
        val id: String?,
        val gold: Double,
        val coins: HashMap<String, Coin>,
        var ids: MutableSet<String>,
        val ordered: MutableList<Coin>,
        val bankedToday: Int
) : Parcelable, AnkoLogger {

    constructor() : this(null, 0.0, hashMapOf<String, Coin>(), mutableSetOf(), mutableListOf(), 0)
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
        coins.forEach { id, c ->
            ids.add(id)
        }
    }

    fun setOrdered() {

    }

    fun getCoin(id: String): Coin? {
        info("[getCoin] returning ${coins[id]} for id = $id")
        return coins[id]
    }

    fun availableCoins(): List<Coin> {
        return coins.filterValues { c -> !c.gone }.values.toList()
//        return null
    }

    private fun addGold(amount: Double) {
        if (id != null)
            walletCollection.document(id)
                    .update("gold", gold + amount)
                    .addOnSuccessListener { info("successfully added $amount gold to $id's wallet") }
                    .addOnFailureListener { e -> info("could not add $amount gold to  $id's wallet - $e") }
    }

    // collect coin from map
    fun addCoinToWallet(wildCoin: WildCoin) {
        // will try to add even if coin is already present
        if (id != null) {
            val coins = hashMapOf<String, Any?>(
                    "coins" to wildCoin.toCoin().toMap()
            )
            walletCollection.document(id)
                    .set(coins, SetOptions.merge())
//                    .update("coins", FieldValue.arrayUnion(wildCoin.toCoin().toMap()))
                    .addOnSuccessListener {
                        coins[wildCoin.properties.id] = wildCoin.toCoin()
                        info("successfully added coin ${wildCoin.properties.id} to $id's wallet")
                    }
                    .addOnFailureListener { e -> info("could not add coin ${wildCoin.properties.id} to $id's wallet - $e") }
        }
        info("[addCoinToWallet] method complete")
    }

    fun removeCoinFromWallet(coin: Coin) {
        if (id != null) {
            if (coin.id in coins && !coin.gone) {
                val coins = hashMapOf<String, Any?>(
                        "coins" to coin.apply { gone = true }.toMap()
                )
                info("[removeCoinFromWallet] this wallet has id = $id")
                val w = walletCollection.document(id)
                walletCollection.document(id)
                        .set(coins, SetOptions.merge())
//                    .whereEqualTo("id", coin.id).get()
                        .addOnCompleteListener { t ->
                            if (t.isSuccessful) coins[coin.id!!] = coin.apply { gone = true }
                            info("[removeCoinFromWallet] ${t.isSuccessful} ${t.result}")
                        }

//            info("[removeCoinFromWallet] -- w = ${w.result} ")
            }
        }
//                .update("coins", FieldValue.arrayRemove(coin.id))
//                .addOnSuccessListener { info("successfully removed coin ${coin.id} from $id's wallet") }
//                .addOnFailureListener { e -> info("could not remove coin ${coin.id} from $id's wallet - $e") }

        info("[addCoinToWallet] method complete")
    }

    // exchange a coin for its gold value
    fun bankCoin(coin: Coin) {
        if (bankedToday < 25) {

        } else {

        }
    }

    // transfer a coin to another player
    fun donateCoin(coinID: String, walletID: String, rates: Rates, callback: (Double?) -> Unit) {
        if (walletID == id) throw Exception("You can't send coins to yourself!")
//        var g =.0
        val coin = getCoin(coinID) ?: throw Exception("You don't have this coin!")
        if (coin.gone) throw Exception("You don't have this coin anymore!")
        info("[donateCoin] coin is ${coin.value}, ${coin.currency}")

        val rate = rates.toMap()[coin.currency] ?: throw Exception("Exchange rate error")
        walletStore.getWallet(walletID) { w ->
            if (w == null) callback(null)
            else {
                val g = coin.value!! * rate
                removeCoinFromWallet(coin)
                w.addGold(g)
                callback(g)
            }
        }
//        return g
    }

}