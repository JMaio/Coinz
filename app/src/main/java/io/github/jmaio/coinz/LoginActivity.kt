package io.github.jmaio.coinz

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.alert
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.info
import org.jetbrains.anko.yesButton


class LoginActivity : AppCompatActivity(), AnkoLogger, PermissionsListener {

    private val fbAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
    }

    val user = fbAuth.currentUser

    private lateinit var permissionsManager: PermissionsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Coinz)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        enableLocationPermissions()

        if (user != null) {
            gotoMain()
        }

        // bypass login for testing
        text_welcome.setOnClickListener { view ->
            gotoMain()
        }

        sign_in_button.setOnClickListener { view ->
            info("[login] email '${email_input.text}', password length = ${password_input.text.length}")
            view.snackbar("Authenticating...")

            val email = email_input.text.toString()
            val password = password_input.text.toString()
            if (email.isNotEmpty() and password.isNotEmpty()) {
                signInUser(email_input.text.toString(), password_input.text.toString())
            } else {
                view.snackbar("Please enter a valid email and password")
            }
        }

        register_button.setOnClickListener { view ->
            info("[register] email '${email_input.text}', password length = ${password_input.text.length}")
            view.snackbar("Registering...")

            val email = email_input.text.toString()
            val password = password_input.text.toString()
            if (email.isNotEmpty() and password.isNotEmpty()) {
                registerUser(email_input.text.toString(), password_input.text.toString())
            } else {
                view.snackbar("Please enter a valid email and password")
            }
        }
    }

    private fun gotoMain() {
        val intent = Intent(this, MainActivity::class.java)
        // try to pass the id of the current user - otherwise, pass "defaultUser"
//        intent.putExtra("id", if (fbAuth.currentUser?.email != null) fbAuth.currentUser?.email else "defaultUser")
        startActivity(intent)
        this.finish()
    }

    private fun signInUser(email: String, password: String) {
        fbAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                gotoMain()
            } else {
                alert {
                    title = "Error"
                    message = task.exception?.message.toString()
                }.show()
            }
        }
    }

    private fun registerUser(email: String, password: String) {
        // register the user
        fbAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val newUser = fbAuth.currentUser
                info("new user has id ${newUser?.uid}")
                // create a new wallet with this ID
                try {
                    createFireBaseWallet(newUser!!.email!!)
                    gotoMain()
                } catch (e: Exception) {
                    newUser!!.delete()
                    alert {
                        title = "Error"
                        message = e.toString()
                    }.show()
                }
            } else {
                alert {
                    title = "Error"
                    message = task.exception?.message.toString()
                }.show()
            }
        }
    }

    private fun createFireBaseWallet(email: String) {
        // and create a wallet
        val wallet = HashMap<String, Any>()
        wallet["coins"] = emptyList<Coin>()
        wallet["gold"] = 0.0
        info("[createFireBaseWallet] entered create method + set wallet val")

        // Add a new document with a generated ID
        db.collection("wallets")
                .document(email).set(wallet)
                .addOnSuccessListener {
                    info("[createFireBaseWallet] DocumentSnapshot added with ID: $email")
                }
                .addOnFailureListener { e ->
                    info("[createFireBaseWallet] Error adding document: $e")
                    throw Exception("something happened")
                }
    }

    fun areLocationPermissionsGranted(): Boolean =
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED


    fun enableLocationPermissions() {
        if (areLocationPermissionsGranted()) {
            info("[enableLocation] Location Permission [ON]")
        } else {
            info("[enableLocation] Location Permission [OFF] -- requesting")
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    // mapbox / permissions
    override fun onExplanationNeeded(permsToExplain: MutableList<String>?) {}

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onPermissionResult(granted: Boolean) {
        if (!granted) {
            alert {
                title = "Please enable location!"
                message = getString(R.string.location_explanation)
                isCancelable = false
                yesButton { enableLocationPermissions() }
            }.show()
        }
    }


}
