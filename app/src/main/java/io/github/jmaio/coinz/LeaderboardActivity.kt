package io.github.jmaio.coinz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_leaderboard.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.padding

class LeaderboardActivity : AppCompatActivity(), AnkoLogger {

    val walletStore = WalletStore()
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private var top: List<Wallet> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)
        walletStore.getTopWallets(10) { t ->
            top = t ?: top
            viewAdapter = LeaderboardAdapter(top)
            viewManager = LinearLayoutManager(this)
            recyclerView = leaderboard_items_view.apply {
                // use this setting to improve performance if you know that changes
                // in content do not change the layout size of the RecyclerView
                setHasFixedSize(true)
                // use a linear layout manager
                layoutManager = viewManager
                // specify an viewAdapter (see also next example)
                adapter = viewAdapter
            }
        }
    }


    class LeaderboardAdapter(private val wallets: List<Wallet>) :
            RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>(), AnkoLogger {

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder.
        // Each data item is just a string in this case that is shown in a TextView.
        class LeaderboardViewHolder(val view: View,
                                    val rank: TextView = view.findViewById(R.id.leader_rank),
                                    val user: TextView = view.findViewById(R.id.leader_user),
                                    val gold: TextView = view.findViewById(R.id.leader_gold)
        ) : RecyclerView.ViewHolder(view), AnkoLogger

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): LeaderboardAdapter.LeaderboardViewHolder {
            // create a new view
            val itemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_leaderboard, parent, false) as View
            // set the view's size, margins, padding and layout parameters
            itemView.padding = 20
            return LeaderboardViewHolder(itemView)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            val w = wallets[position]
            info("[onBindViewHolder] processing ${w.id}")

            holder.apply {
                rank.text = (position + 1).toString()
                user.text = w.id
                gold.text = "%.2f".format(w.gold)
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = wallets.size

    }

}
