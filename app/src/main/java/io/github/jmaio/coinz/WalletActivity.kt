package io.github.jmaio.coinz

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_wallet.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.snackbar

class WalletActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        fab.setOnClickListener {
            val mcv = MaterialCardView(this).apply {
                addView(TextView(this@WalletActivity).apply {
                    text = "hello"
                })
                minimumHeight = dip(50)
            }
            wallet_linear_layout.addView(mcv)
        }
    }
}
