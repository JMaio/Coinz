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
                        val w = task.result!!.toObject(Wallet::class.java)!!
                        w.setID(id)
                        w.setIds()
                        callback(w)
                    } else {
                        info("[getWallet] failed with ${task.result}")
                    }
                }
    }

}
