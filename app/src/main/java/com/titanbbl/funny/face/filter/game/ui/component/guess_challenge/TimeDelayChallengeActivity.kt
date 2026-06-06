package com.titanbbl.funny.face.filter.game.ui.component.guess_challenge

import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ActivityTimeDelayChallengeBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import com.titanbbl.funny.face.filter.game.utils.Routes

class TimeDelayChallengeActivity : BaseActivity<ActivityTimeDelayChallengeBinding>() {

    override fun getLayoutActivity(): Int = R.layout.activity_time_delay_challenge

    override fun initViews() {

    }

    override fun onClickViews() {
        // Back button
        mBinding.btnBack.setOnClickListener {
            finish()
        }


        // First function card - Face Delay Effect
        mBinding.cardFaceVertical.setOnClickListener {
            Routes.startTimeDelayActivityVertical(this)
        }

        // Second function card - Face Tracking
        mBinding.cardFaceGrid.setOnClickListener {
            Routes.startTimeDelayActivityGrid(this)
        }
    }
}