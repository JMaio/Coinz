package io.github.jmaio.coinz

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.content.pm.PackageManager
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.app.LoaderManager.LoaderCallbacks
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView

import java.util.ArrayList
import android.Manifest.permission.READ_CONTACTS
import android.content.Intent
import android.widget.Button
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.snackbar

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity() {

    val fbAuth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        email_sign_in_button.setOnClickListener { view ->
            signIn(view, "user@company.com", "pass")
        }
    }

    fun signIn(view: View, email: String, password: String) {
        login_form.snackbar("Authenticating...")

        fbAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
//            if (task.isSuccessful) {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("id", fbAuth.currentUser?.email)
                startActivity(intent)
//            } else {
//                login_form.indefiniteSnackbar("Error: ${task.exception?.message}", "Retry?") {
//
//                }.show()
//            }

        }

    }


}
