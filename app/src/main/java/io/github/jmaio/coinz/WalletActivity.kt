package io.github.jmaio.coinz

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import kotlinx.android.synthetic.main.activity_wallet.*
import org.jetbrains.anko.dip

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
