package io.github.jmaio.coinz

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager

import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.snackbar

class LoginActivity : AppCompatActivity(), AnkoLogger, PermissionsListener {

    private val fbAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private lateinit var permissionsManager: PermissionsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Coinz)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        enableLocationPermissions()

        text_welcome.setOnClickListener { view ->
            // bypass login for testing
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
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

    private fun signInUser(email: String, password: String) {
        fbAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("id", fbAuth.currentUser?.email)
                startActivity(intent)
            } else {
                alert {
                    title = "Error"
                    message = task.exception?.message.toString()
                }.show()
            }
        }
    }
    private fun registerUser(email: String, password: String) {
        fbAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("id", fbAuth.currentUser?.email)
                startActivity(intent)
            } else {
                alert {
                    title = "Error"
                    message = task.exception?.message.toString()
                }.show()
            }
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
