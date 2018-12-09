package io.github.jmaio.coinz

import android.content.Context
import android.graphics.Canvas
import android.media.Image
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import kotlinx.android.synthetic.main.activity_wallet.*
import kotlinx.android.synthetic.main.fragment_item.view.*
import org.jetbrains.anko.*
import org.jetbrains.anko.internals.AnkoInternals.addView

class WalletActivity : AppCompatActivity(), AnkoLogger {

    fun makeCoinCardView(coin: Coin): CardView {
        val cardView = CardView(this).apply {
            addView(LinearLayout(this@WalletActivity).apply {
                addView(ImageView(this@WalletActivity).apply {
                    imageResource = resources.getIdentifier("marker_${coin.currency!!.toLowerCase()}", "drawable", packageName)
                })
                addView(TextView(this@WalletActivity).apply {
                    text = "hello"
                })
            })
//                minimumHeight = dip(50)
            dip(16).let { d -> setContentPadding(d, d, d, d) }
        }
        val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dip(6)
        }
        cardView.layoutParams = layoutParams
        return cardView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        fab.setOnClickListener {
//            val cardView = CardView(this).apply {
//                addView(TextView(this@WalletActivity).apply {
//                    text = "hello"
//                })
////                minimumHeight = dip(50)
//                dip(20).let { d -> setContentPadding(d, d, d, d) }
//            }
//            val layoutParams = LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.MATCH_PARENT,
//                    LinearLayout.LayoutParams.WRAP_CONTENT
//            ).apply {
//                topMargin = dip(6)
//            }
//            cardView.layoutParams = layoutParams
            val cardView = makeCoinCardView(Coin("1234", "SHIL", 10.0))
            wallet_linear_layout.addView(cardView)
        }
    }

}

