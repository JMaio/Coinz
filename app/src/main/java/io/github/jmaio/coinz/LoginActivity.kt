package io.github.jmaio.coinz

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

import android.content.Intent
import com.google.firebase.auth.FirebaseAuth

import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.snackbar

class LoginActivity : AppCompatActivity(), AnkoLogger {

    private val fbAuth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Coinz)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

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

}
