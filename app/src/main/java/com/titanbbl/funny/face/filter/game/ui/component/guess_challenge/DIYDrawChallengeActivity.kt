package com.titanbbl.funny.face.filter.game.ui.component.guess_challenge

import android.content.Intent
import android.widget.Toast
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ActivityDiydrawChallengeBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import com.titanbbl.funny.face.filter.game.ui.bases.ext.click
import com.titanbbl.funny.face.filter.game.ui.component.filter_activity.draw.activity.DrawByNoseActivity
import com.titanbbl.funny.face.filter.game.ui.component.filter_activity.draw.activity.HandDetectActivity
import com.titanbbl.funny.face.filter.game.ui.component.filter_activity.draw.activity.HandDrawingActivity
import com.titanbbl.funny.face.filter.game.ui.component.guess_challenge.adapter.ChallengeAdapter
import com.titanbbl.funny.face.filter.game.model.ChallengeClickListener
import com.titanbbl.funny.face.filter.game.model.ChallengeItem

class DIYDrawChallengeActivity : BaseActivity<ActivityDiydrawChallengeBinding>() {

    private val challengeAdapter = ChallengeAdapter()
    private var selectedChallengeId: Int? = null

    override fun getLayoutActivity(): Int {
        return R.layout.activity_diydraw_challenge
    }

    override fun initViews() {
        super.initViews()

        mBinding.apply {
            // Set activity for data binding


            // Add sample data with image URLs
            val challenges = listOf(
                ChallengeItem(
                    id = 1, imageUrl = R.drawable.draw_hand, // Replace with actual URLs
                    label = "DRAWING"
                ), ChallengeItem(
                    id = 2, imageUrl = R.drawable.draw_nose, // Replace with actual URLs,
                    label = "USE NOSE"
                ), ChallengeItem(
                    id = 3, imageUrl = R.drawable.draw_finger, // Replace with actual URLs,
                    label = "USE FINGER"
                )
            )
            challengeAdapter.submitList(challenges)
            mBinding.rvChallenges.adapter = challengeAdapter
        }
    }

    override fun onClickViews() {
        super.onClickViews()

        mBinding.apply {
            // Handle challenge item clicks
            challengeAdapter.setOnChallengeClickListener(object : ChallengeClickListener {
                override fun onChallengeClick(item: ChallengeItem) {
                    selectedChallengeId = item.id
                    when (selectedChallengeId) {
                        1 -> {
                            // Navigate to DIYDrawActivity with DRAWING challenge
                            startActivity(
                                Intent(
                                    this@DIYDrawChallengeActivity, HandDrawingActivity::class.java
                                )
                            )
                        }

                        2 -> {
                            // Navigate to DIYDrawActivity with USE NOSE challenge
                            startActivity(
                                Intent(
                                    this@DIYDrawChallengeActivity, DrawByNoseActivity::class.java
                                )
                            )
                        }

                        3 -> {
                            // Navigate to DIYDrawActivity with USE FINGER challenge

                            startActivity(
                                Intent(
                                    this@DIYDrawChallengeActivity, HandDetectActivity::class.java
                                )
                            )

                        }

                        else -> {
                            Toast.makeText(
                                this@DIYDrawChallengeActivity,
                                getString(R.string.unknown_challenge_selected),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            })

            mBinding.btnBack.click {
                onBackPressed()
            }

            // Handle done button click
            mBinding.btnDone.setOnClickListener {
                if (selectedChallengeId != null) {
                    when (selectedChallengeId) {
                        1 -> {
                            // Navigate to DIYDrawActivity with DRAWING challenge
                            startActivity(
                                Intent(
                                    this@DIYDrawChallengeActivity, HandDrawingActivity::class.java
                                )
                            )
                        }

                        2 -> {
                            // Navigate to DIYDrawActivity with USE NOSE challenge
                            startActivity(
                                Intent(
                                    this@DIYDrawChallengeActivity, DrawByNoseActivity::class.java
                                )
                            )
                        }

                        3 -> {
                            // Navigate to DIYDrawActivity with USE FINGER challenge

                        }

                        else -> {
                            Toast.makeText(
                                this@DIYDrawChallengeActivity,
                                getString(R.string.unknown_challenge_selected),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                } else {
                    Toast.makeText(
                        this@DIYDrawChallengeActivity,
                        getString(R.string.please_select_a_challenge),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}