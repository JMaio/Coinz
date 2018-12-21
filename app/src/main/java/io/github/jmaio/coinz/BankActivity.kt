package io.github.jmaio.coinz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_bank.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.design.snackbar
import org.jetbrains.anko.info
import org.jetbrains.anko.longToast
import org.jetbrains.anko.padding

class BankActivity : AppCompatActivity(), AnkoLogger {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private var wallet: Wallet? = null
    private var rates: Rates? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank)

        rates = intent.extras?.getParcelable("rates")
        wallet = intent.extras?.getParcelable("wallet")
        info("[BankActivity] onCreate - rates = $rates")
        info("[BankActivity] onCreate - wallet = $wallet")

        if (rates != null) {
            setupRates()
        }
        if (wallet != null) {
            // set progress
//            val p = wallet.coins.size
//            wallet_progress_text.text = getString(R.string.coins_collected_progress, p)
//            wallet_day_progressbar.progress = (p * 2)

            updateGoldChip()
            updateProgressBar()

            viewManager = LinearLayoutManager(this)
            viewAdapter = BankAdapter(wallet!!, rates)

            recyclerView = bank_items_view.apply {
                // use this setting to improve performance if you know that changes
                // in content do not change the layout size of the RecyclerView
                setHasFixedSize(true)

                // use a linear layout manager
                layoutManager = viewManager

                // specify an viewAdapter (see also next example)
                adapter = viewAdapter
            }
            recyclerView.setRecyclerListener {
                updateGoldChip()
                updateProgressBar()
            }
        }

    }

    private fun setupRates() {
        info("[setupRates] starting")
        val transl = mapOf(
                "SHIL" to Pair(shil_curr_value_units, shil_curr_value_decimals),
                "DOLR" to Pair(dolr_curr_value_units, dolr_curr_value_decimals),
                "QUID" to Pair(quid_curr_value_units, quid_curr_value_decimals),
                "PENY" to Pair(peny_curr_value_units, peny_curr_value_decimals)
        )
        transl.forEach { (curr, fields) ->
            val r = rates!!.toMap()[curr].toString().split(".")
            try {
                fields.apply {
                    first.text = r[0]
                    second.text = r[1].take(2)
                }
            } catch (e: Exception) {
                fields.apply {
                    first.text = "-"
                    second.text = "-"
                }
                bank_items_view.indefiniteSnackbar("Could not get today's rates. Please retry loading the map from the map screen.")
            }
        }
    }

    fun updateGoldChip() {
        wallet?.gold.toString().split('.').let { (u, d) ->
            bank_gold_chip.text = getString(R.string.value_display, u, d.take(6))
        }
    }

    fun updateProgressBar() {
        bank_progress_text.text = getString(R.string.coins_banked_progress, wallet!!.bankedToday)
        bank_day_progressbar.progress = (wallet!!.bankedToday * 4)
    }


    class BankAdapter(private val wallet: Wallet,
                      private val rates: Rates?) :
            RecyclerView.Adapter<BankAdapter.BankViewHolder>(), AnkoLogger {

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder.
        // Each data item is just a string in this case that is shown in a TextView.
        class BankViewHolder(val view: View,
                             val curr: TextView = view.findViewById(R.id.bank_curr_text),
                             val currUnits: TextView = view.findViewById(R.id.bank_curr_units),
                             val currDec: TextView = view.findViewById(R.id.bank_curr_dec),
                             val goldUnits: TextView = view.findViewById(R.id.bank_gold_units),
                             val goldDec: TextView = view.findViewById(R.id.bank_gold_dec),
                             val button: ImageButton = view.findViewById(R.id.bank_button_send)
        ) : RecyclerView.ViewHolder(view), AnkoLogger

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): BankAdapter.BankViewHolder {
            // create a new view
            val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_bank, parent, false) as View
            // set the view's size, margins, padding and layout parameters
            itemView.padding = 20
//        val p = 30
//        itemView.setContentPadding(p, p, p, p)

            return BankViewHolder(itemView)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(holder: BankViewHolder, position: Int) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            info("[onBindViewHolder] wallet size ${wallet.availableCoins().size}")
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
                try {
                    wallet.coinGoldValue(coin, rates).toString().split('.').let { (u, d) ->
                        goldUnits.text = u
                        goldDec.text = d.take(3)
                    }
                } catch (e: Exception) {
                    holder.view.snackbar("Error: ${e.message}", "Dismiss", {})
                }
                button.setOnClickListener { v ->
                    if (rates != null) {
                        try {
                            wallet.bankCoin(coin, rates) { g ->
                                info("banking coin $coin")
                                holder.view.context.longToast("Banked ${g.toString().take(7)} Gold from ${coin.value.toString().take(5)} ${coin.currency}")
                                notifyItemRemoved(holder.adapterPosition)
                            }
                        } catch (e: Exception) {
                            info("[bankCoin] -- exception ${e.message}")
                            holder.view.context.longToast(e.message.toString())
                        }
                    } else {
                        v.indefiniteSnackbar("Exchange rates missing! Please wait until the map has been downloaded and try again.").show()
                    }
//                } else {
//                    button.apply {
//                        isClickable = false
//                        isEnabled = false
//                    }
                }
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = wallet.availableCoins().size

    }
}
