package io.github.jmaio.coinz

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class WalletStore : AnkoLogger {

    var db: FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
    }

    fun getTopWallets(n: Long, callback: (List<Wallet>?) -> Unit) {
        db.collection("wallets").orderBy("gold", Query.Direction.DESCENDING).limit(n).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val l = task.result?.toObjects(Wallet::class.java)
                        callback(l)
                    } else {
                        callback(null)
                    }
                }
    }

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
                                info("[getWallet] wallet retrieved for ${w.id} --> ${w.toString().take(100)}}...")
                            }
                            callback(w)
                        } else {
                            info("[getWallet] failed with ${task.result}")
                            callback(null)
                        }
                    }
    }

}
