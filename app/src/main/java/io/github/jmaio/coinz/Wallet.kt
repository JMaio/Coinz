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
        var gold: Double,
        val coins: HashMap<String, Coin>,
        val ordered: MutableList<Coin>,
        var bankedToday: Int
) : Parcelable, AnkoLogger {

    constructor() : this(null, 0.0, hashMapOf<String, Coin>(), mutableListOf(), 0)
    constructor(id: String?, w: Wallet) : this(id, w.gold, w.coins, w.ordered, w.bankedToday)

    @IgnoredOnParcel
    private val walletStore = WalletStore()
    @IgnoredOnParcel
    private val walletCollection = walletStore.db.collection("wallets")
    val size get() = coins.size

    val ids: Set<String>
        get() = coins.keys

    private fun getCoin(id: String): Coin? {
        info("[getCoin] returning ${coins[id]} for id = $id")
        return coins[id]
    }

    fun availableCoins(): List<Coin> {
        return coins.filterValues { c -> !c.gone }.values.toList()
    }

    private fun addGold(amount: Double) {
        if (id != null) {
            gold += amount
            walletCollection.document(id)
                    .update("gold", gold + amount)
                    .addOnSuccessListener { info("successfully added $amount gold to $id's wallet") }
                    .addOnFailureListener { e -> info("could not add $amount gold to  $id's wallet - $e") }
        }
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
                    .addOnSuccessListener {
                        coins[wildCoin.properties.id] = wildCoin.toCoin()
                        info("successfully added coin ${wildCoin.properties.id} to $id's wallet")
                    }
                    .addOnFailureListener { e -> info("could not add coin ${wildCoin.properties.id} to $id's wallet - $e") }
        }
        info("[addCoinToWallet] method complete")
    }

    private fun removeCoinFromWallet(coin: Coin) {
        if (id != null) {
            if (coin.id in coins && !coin.gone) {
                val coins = hashMapOf<String, Any?>(
                        "coins" to coin.apply { gone = true }.toMap()
                )
                info("[removeCoinFromWallet] this wallet has id = $id")
                walletCollection.document(id)
                        .set(coins, SetOptions.merge())
                        .addOnCompleteListener { t ->
                            if (t.isSuccessful) coins[coin.id] = coin.apply { gone = true }
                            info("[removeCoinFromWallet] ${t.isSuccessful} ${t.result}")
                        }
            }
        }
        info("[addCoinToWallet] method complete")
    }

    private fun incrementBankedToday() {
        updateBankedToday(++bankedToday)
    }

    private fun updateBankedToday(q: Int) {
        if (id != null) {
            bankedToday = q
            walletCollection.document(id)
                    .update("bankedToday", bankedToday)
                    .addOnSuccessListener { info("successfully reset bankedToday for $id") }
                    .addOnFailureListener { info("could not reset bankedToday for $id") }
        }
    }

    // exchange a coin for its gold value
    fun bankCoin(coin: Coin, rates: Rates, callback: (Double?) -> Unit) {
        if (bankedToday < 25 && !coin.gone) {
            val g = coinGoldValue(coin, rates)
            removeCoinFromWallet(coin)
            incrementBankedToday()
            addGold(g)
            callback(g)
        } else {
            throw Exception("Coin banking limit exceeded!")
        }
    }

    // transfer a coin to another player
    fun donateCoin(coinID: String, walletID: String, rates: Rates, callback: (Double?) -> Unit) {
        if (walletID == id) throw Exception("You can't send coins to yourself!")
        val coin = getCoin(coinID) ?: throw Exception("You don't have this coin!")
        if (coin.gone) throw Exception("You don't have this coin anymore!")
        info("[donateCoin] coin is ${coin.value}, ${coin.currency}")

        walletStore.getWallet(walletID) { w ->
            if (w == null) callback(null)
            else {
                val g = coinGoldValue(coin, rates)
                removeCoinFromWallet(coin)
                w.addGold(g)
                callback(g)
            }
        }
    }

    private fun resetCoins() {
        if (id != null) {
            val empty = hashMapOf<String, Any?>("coins" to emptyMap<String, Any?>())
            walletCollection.document(id)
                    .update(empty)
                    .addOnSuccessListener { info("successfully reset coins for $id's wallet") }
                    .addOnFailureListener { e -> info("could not reset coins for $id's wallet - $e") }
        }
    }

    fun coinGoldValue(coin: Coin, rates: Rates?): Double {
        var g = .0
        if (rates != null) {
            val rate = rates.toMap()[coin.currency] ?: throw Exception("Exchange rate error")
            g = coin.value * rate
        }
        return g
    }

    fun resetWalletNextDay() {
        updateBankedToday(0)
        resetCoins()
    }

}