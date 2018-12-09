package io.github.jmaio.coinz

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_wallet.*
import kotlinx.android.synthetic.main.item_bank.*
import org.jetbrains.anko.*
import org.w3c.dom.Text

class WalletActivity : AppCompatActivity(), AnkoLogger {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        val wallet = intent.getParcelableExtra<Wallet>("wallet")

        // set progress
        val p = wallet.coins.size
        wallet_progress_text.text = getString(R.string.coins_collected_progress, p)
        wallet_day_progressbar.progress = (p * 2)

        wallet.gold.toString().split('.').let { (u, d) ->
            gold_chip.text = getString(R.string.value_display, u, d)
                    //"$u.${d.take(3)}"
        }


        viewManager = LinearLayoutManager(this)
        viewAdapter = WalletAdapter(wallet)

        recyclerView = findViewById<RecyclerView>(R.id.wallet_items_view).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
        }

    }

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

    class WalletAdapter(private val wallet: Wallet) :
            RecyclerView.Adapter<WalletAdapter.WalletViewHolder>() {

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder.
        // Each data item is just a string in this case that is shown in a TextView.
        class WalletViewHolder(val view: View,
                               val curr: TextView = view.findViewById(R.id.wallet_curr_text),
                               val currUnits: TextView = view.findViewById(R.id.wallet_curr_units),
                               val currDec: TextView = view.findViewById(R.id.wallet_curr_dec)
        ) : RecyclerView.ViewHolder(view), AnkoLogger {
//        val curr = R.id.wallet_curr_text
//        val currUnits = R.id.wallet_curr_units
//        val currDec = R.id.wallet_curr_dec
        }


        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): WalletAdapter.WalletViewHolder {
            // create a new view
            val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_wallet, parent, false) as View
            // set the view's size, margins, paddings and layout parameters
            itemView.padding = 20
//        val p = 30
//        itemView.setContentPadding(p, p, p, p)

            return WalletViewHolder(itemView)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(holder: WalletViewHolder, position: Int) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            val coin = wallet.coins[position]
            val drawables = mapOf(
                    Pair("shil", R.drawable.marker_shil),
                    Pair("dolr", R.drawable.marker_dolr),
                    Pair("quid", R.drawable.marker_quid),
                    Pair("peny", R.drawable.marker_peny)
            )
            holder.apply {
                curr.apply {
                    text = coin.currency
                    setCompoundDrawablesWithIntrinsicBounds( 0, drawables[coin.currency!!.toLowerCase()]!!,0, 0)
                }
                coin.value.toString().split('.').let { (u, d) ->
                    currUnits.text = u
                    currDec.text = d.take(6)
                }
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = wallet.coins.size
    }

}

