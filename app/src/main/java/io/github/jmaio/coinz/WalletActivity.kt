package io.github.jmaio.coinz

import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_wallet.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.sdk27.coroutines.onClick

class WalletActivity : AppCompatActivity(), AnkoLogger {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private lateinit var wallet: Wallet
    private var rates: Rates? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        val w: Wallet? = intent.extras?.getParcelable("wallet")
        rates = intent.extras?.getParcelable("rates")

        if (w != null) {
            wallet = w

            // set progress
            val p = wallet.coins.size
            wallet_progress_text.text = getString(R.string.coins_collected_progress, p)
            wallet_day_progressbar.progress = (p * 2)

            wallet.gold.toString().split('.').let { (u, d) ->
                wallet_gold_chip.text = getString(R.string.value_display, u, d.take(6))
            }

            viewAdapter = WalletAdapter(wallet, rates)
        }

        viewManager = LinearLayoutManager(this)

        recyclerView = findViewById<RecyclerView>(R.id.wallet_items_view).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)
            // use a linear layout manager
            layoutManager = viewManager
            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
            // add click listeners?
        }
    }

    class WalletAdapter(private val wallet: Wallet, private val rates: Rates?) :
            RecyclerView.Adapter<WalletAdapter.WalletViewHolder>() {

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder.
        // Each data item is just a string in this case that is shown in a TextView.
        class WalletViewHolder(val view: View,
                               val curr: TextView = view.findViewById(R.id.wallet_curr_text),
                               val currUnits: TextView = view.findViewById(R.id.wallet_curr_units),
                               val currDec: TextView = view.findViewById(R.id.wallet_curr_dec),
                               val button: ImageButton = view.findViewById(R.id.wallet_button_send)
        ) : RecyclerView.ViewHolder(view), AnkoLogger, View.OnClickListener {
            init {
                button.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                v?.context?.toast("hello")
            }
        }

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): WalletAdapter.WalletViewHolder {
            // create a new view
            val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_wallet, parent, false) as View
            // set the view's size, margins, padding and layout parameters
            return WalletViewHolder(itemView)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(holder: WalletViewHolder, position: Int) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            val coin = wallet.availableCoins()[position]
            val drawables = mapOf(
                    "shil" to R.drawable.marker_shil,
                    "dolr" to R.drawable.marker_dolr,
                    "quid" to R.drawable.marker_quid,
                    "peny" to R.drawable.marker_peny
            )
            holder.apply {
                curr.apply {
                    text = coin.currency
                    setCompoundDrawablesWithIntrinsicBounds(0, drawables[coin.currency.toLowerCase()]!!, 0, 0)
                }
                coin.value.toString().split('.').let { (u, d) ->
                    currUnits.text = u
                    currDec.text = d.take(6)

                }
                // handle send button click logic fully within this component
                button.setOnClickListener { v ->
                    if (rates != null) {
                        lateinit var dialog: DialogInterface
                        dialog = v.context.alert {
                            title = "Sending ${coin.currency} (${coin.value.toString().take(8)})"
                            message = "Please specify the username of the receiver:"
                            customView {
                                verticalLayout {
                                    val receiver = editText { hint = "Receiver username" }
                                            .lparams(width = matchParent) {
                                                horizontalPadding = dip(32)
                                            }
                                    button("Send") {
                                        onClick {
                                            info("[donateCoin] wallet is ${wallet.toString().take(100)}")
                                            val recv = receiver.text.toString().trim()
                                            if (recv.isBlank()) ctx.toast("Please enter a valid username")
                                            else {
                                                try {
                                                    wallet.donateCoin(coin.id, recv, rates) { g ->
                                                        info("sending coin $coin")
                                                        if (g == null) {
                                                            ctx.longToast("Could not send this coin!")
                                                        } else {
                                                            notifyItemRemoved(holder.adapterPosition)
                                                            ctx.longToast("Sending ${g.toString().take(7)} Gold to ${receiver.text}")
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    info("[donateCoin] -- exception ${e.message}")
                                                    ctx.longToast(e.message.toString())
                                                } finally {
                                                    dialog.dismiss()
                                                    info("[donateCoin] dialog dismissed")
                                                }
                                            }
                                        }
                                    }.lparams(width = wrapContent) {
                                        gravity = Gravity.CENTER
                                    }
                                }
                            }
                        }.show()
                    } else {
                        v.indefiniteSnackbar("Exchange rates missing! Please wait until the map has been downloaded and try again.").show()
                    }
                }
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = wallet.availableCoins().size
    }

}

