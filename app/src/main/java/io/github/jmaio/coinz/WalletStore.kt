package io.github.jmaio.coinz

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class WalletStore : AnkoLogger {

    var db: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
    }
    lateinit var rates: Rates

    fun getWallet(user: FirebaseUser, callback: (Wallet) -> Unit) {
        // "guaranteed" to work for firebase user
        return getWallet(user.email!!) { w ->
            callback(w!!)
        }
    }

    fun getWallet(id: String, callback: (Wallet?) -> Unit) {
        info("[getWallet] getting $id's wallet...")
        if (id.isNotEmpty())
            db.collection("wallets").document(id).get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            info("[getWallet] task successful -- ${task.result.toString().take(100)}...")
                            var w = task.result?.toObject(Wallet::class.java)
                            if (w != null) {
                                w = Wallet(id, w)
                                w.setIds()
                                info("[getWallet] wallet retrieved for ${w.id} --> ${w.toString().take(100)}}...")
                            }
                            callback(w)
                        } else {
                            info("[getWallet] failed with ${task.result}")
                            callback(null)
                        }
                    }
    }

//    fun donateCoin(coinID: String, senderID: String, receiverID: String) {
//        getWallet(senderID) { wSend ->
//            val coin = wSend!!.coins.find { c ->
//                c.id == coinID
//            }
//            info("[donateCoin] coin is ${coin?.value}, ${coin?.currency}")
//            if (coin != null && !coin.gone) {
//                val rate = rates.toMap()[coin.currency]!!
//                // coin is in the wallet
//                getWallet(receiverID) { w ->
//                    if (w == null) throw Exception("Receiver wallet not found!") // wallet not found
//                    w.addGold(coin.value!! * rate) //coin.currency)
//                }
//            } else throw Exception("You don't have this coin anymore!")
//        }
//    }

}
