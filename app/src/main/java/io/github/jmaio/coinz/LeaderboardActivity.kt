package io.github.jmaio.coinz

import android.os.Bundle
import android.app.Activity

import kotlinx.android.synthetic.main.activity_leaderboard.*

class LeaderboardActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

}
