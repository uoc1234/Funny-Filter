package com.titanbbl.funny.face.filter.game.ui.component.guess_challenge

import android.widget.Toast
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ActivityLipFallChallengeBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import com.titanbbl.funny.face.filter.game.ui.component.guess_challenge.adapter.LipFallAdapter
import com.titanbbl.funny.face.filter.game.model.LipFallItem
import com.titanbbl.funny.face.filter.game.model.LipFallType

class LipFallChallengeActivity : BaseActivity<ActivityLipFallChallengeBinding>() {

    private val adapter = LipFallAdapter()
    private var selectedItem: LipFallItem? = null

    override fun getLayoutActivity(): Int {
        return R.layout.activity_lip_fall_challenge
    }

    override fun onClickViews() {
        super.onClickViews()
        
        mBinding.apply {
            btnBack.setOnClickListener {
                finish()
            }
            
            btnCheck.setOnClickListener {
                if (selectedItem != null) {
                    handleLipFallChallenge(selectedItem!!)
                } else {
                    Toast.makeText(this@LipFallChallengeActivity, "Please select a challenge", Toast.LENGTH_SHORT).show()
                }
            }
        }

        adapter.onItemClick = { item ->
            when (item.type) {
                LipFallType.FACEPUZZLE -> {
                    selectedItem = item
                    Toast.makeText(this, "Face Puzzle selected", Toast.LENGTH_SHORT).show()
                }
                LipFallType.ZOOMPUZZLE -> {
                    selectedItem = item
                    Toast.makeText(this, "Zoom Puzzle selected", Toast.LENGTH_SHORT).show()
                }
                LipFallType.SPINPUZZLE -> {
                    selectedItem = item
                    Toast.makeText(this, "Spin Puzzle selected", Toast.LENGTH_SHORT).show()
                }

            }
        }
    }

    override fun initViews() {
        super.initViews()
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        mBinding.rvLipFallItems.adapter = adapter
        
        // Add items to adapter including ads item
        val items = listOf(
            LipFallItem(
                R.drawable.img_cartoon_challenge, // You can replace with actual images
                getString(R.string.face_puzzle),
                LipFallType.FACEPUZZLE
            ),
            LipFallItem(
                R.drawable.img_season_challenge, // You can replace with actual images
                getString(R.string.zoom_puzzle),
                LipFallType.ZOOMPUZZLE
            ),
            LipFallItem(
                R.drawable.img_y2k_challenge, // You can replace with actual images
                getString(R.string.spin_puzzle),
                LipFallType.SPINPUZZLE
            ),

        )
        adapter.submitList(items)
    }
    
    private fun handleLipFallChallenge(item: LipFallItem) {
        when (item.type) {
            LipFallType.FACEPUZZLE -> {
                // Navigate to Face Puzzle activity or start challenge
                Toast.makeText(this, "Starting Face Puzzle Challenge", Toast.LENGTH_SHORT).show()
            }
            LipFallType.ZOOMPUZZLE -> {
                // Navigate to Zoom Puzzle activity or start challenge  
                Toast.makeText(this, "Starting Zoom Puzzle Challenge", Toast.LENGTH_SHORT).show()
            }
            LipFallType.SPINPUZZLE -> {
                // Navigate to Spin Puzzle activity or start challenge
                Toast.makeText(this, "Starting Spin Puzzle Challenge", Toast.LENGTH_SHORT).show()
            }

        }
    }
}