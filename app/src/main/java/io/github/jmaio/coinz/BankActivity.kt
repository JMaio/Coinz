package io.github.jmaio.coinz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_bank.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.padding
import org.jetbrains.anko.toast

class BankActivity : AppCompatActivity(), AnkoLogger {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private lateinit var wallet: Wallet
    private var rates: Rates? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank)
        setupActionBar()

        rates = intent.extras?.getParcelable("rates")
        setupRates()

        val w: Wallet? = intent.extras?.getParcelable("wallet")
        if (w != null) {
            wallet = w

            // set progress
//            val p = wallet.coins.size
//            wallet_progress_text.text = getString(R.string.coins_collected_progress, p)
//            wallet_day_progressbar.progress = (p * 2)

            wallet.gold.toString().split('.').let { (u, d) ->
                bank_gold_chip.text = getString(R.string.value_display, u, d.take(6))
            }

            viewAdapter = BankActivity.BankAdapter(wallet, rates)
        }


        viewManager = LinearLayoutManager(this)
        viewAdapter = BankAdapter(wallet, rates)

        recyclerView = findViewById<RecyclerView>(R.id.bank_items_view).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
        }
    }

    private fun setupActionBar() {
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRates() {
        val transl = mapOf(
                Pair("SHIL", Pair(shil_curr_value_units, shil_curr_value_decimals)),
                Pair("DOLR", Pair(dolr_curr_value_units, dolr_curr_value_decimals)),
                Pair("QUID", Pair(quid_curr_value_units, quid_curr_value_decimals)),
                Pair("PENY", Pair(peny_curr_value_units, peny_curr_value_decimals))
        )
        if (rates != null) {
            transl.forEach { (curr, fields) ->
                val r = rates?.toMap()?.get(curr).toString().split(".")
                try {
                    fields.apply {
                        first.text = r[0]
                        second.text = r[1].take(3)
                    }
                } catch (e: Exception) {
                    toast("Could not get today's rates. Error: ${e.message}")
                }
            }
        }
    }


    class BankAdapter(private val wallet: Wallet, private val rates: Rates?) :
            RecyclerView.Adapter<BankAdapter.BankViewHolder>(), AnkoLogger {

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder.
        // Each data item is just a string in this case that is shown in a TextView.
        class BankViewHolder(val view: View,
                             val curr: TextView = view.findViewById(R.id.bank_curr_text),
                             val currUnits: TextView = view.findViewById(R.id.bank_curr_units),
                             val currDec: TextView = view.findViewById(R.id.bank_curr_dec)
        ) : RecyclerView.ViewHolder(view), AnkoLogger {
//        val curr = R.id.wallet_curr_text
//        val currUnits = R.id.wallet_curr_units
//        val currDec = R.id.wallet_curr_dec
        }


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
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = wallet.availableCoins().size


    }
}
