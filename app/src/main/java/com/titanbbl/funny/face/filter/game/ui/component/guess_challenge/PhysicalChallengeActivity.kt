package com.titanbbl.funny.face.filter.game.ui.component.guess_challenge

import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ActivityPhysicalChallengeBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import com.titanbbl.funny.face.filter.game.ui.component.guess_challenge.adapter.PhysicalFeatureAdapter
import com.titanbbl.funny.face.filter.game.model.PhysicalFeatureItem
import com.titanbbl.funny.face.filter.game.model.PhysicalFeatureType
import com.titanbbl.funny.face.filter.game.utils.Routes

class PhysicalChallengeActivity : BaseActivity<ActivityPhysicalChallengeBinding>() {

    private lateinit var adapter: PhysicalFeatureAdapter
    private var selectedFeature: PhysicalFeatureType? = null


    override fun getLayoutActivity(): Int = R.layout.activity_physical_challenge

    override fun initViews() {
        super.initViews()
        setupRecyclerView()
        setupPhysicalFeatureData()
    }

    override fun onClickViews() {
        super.onClickViews()
        
        mBinding.apply {
            btnBack.setOnClickListener {
                finish()
            }
            
            btnCheck.setOnClickListener {
                selectedFeature?.let { feature ->
                    handlePhysicalFeatureSelection(feature)
                } ?: run {
                    Toast.makeText(this@PhysicalChallengeActivity, getString(R.string.please_select_feature), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = PhysicalFeatureAdapter().apply {
            onItemClick = { item ->
                selectFeature(item)
                Toast.makeText(this@PhysicalChallengeActivity, "${item.title} feature selected", Toast.LENGTH_SHORT).show()
            }
        }
        
        mBinding.recyclerViewFeatures.apply {
            adapter = this@PhysicalChallengeActivity.adapter
            layoutManager = GridLayoutManager(this@PhysicalChallengeActivity, 2)
        }
    }

    private fun setupPhysicalFeatureData() {
        val physicalFeatures = listOf(
            PhysicalFeatureItem(
                id = 1,
                type = PhysicalFeatureType.LIPS,
                title = "LIPS",
                imageRes = R.drawable.physical_lip, // Replace with actual lips image
                overlayImageRes = null // Add lips outline drawable if available
            ),
            PhysicalFeatureItem(
                id = 2,
                type = PhysicalFeatureType.NOSE,
                title = "NOSE", 
                imageRes = R.drawable.physical_nose, // Replace with actual nose image
                overlayImageRes = null // Add nose outline drawable if available
            ),
            PhysicalFeatureItem(
                id = 3,
                type = PhysicalFeatureType.HAND,
                title = "HAND",
                imageRes = R.drawable.physical_hand, // Replace with actual hand image
                overlayImageRes =null // Use hand icon as overlay
            )
        )
        
        adapter.submitList(physicalFeatures)
    }

    private fun selectFeature(physicalFeatureItem: PhysicalFeatureItem) {

        Routes.startNationalActivity(this, physicalFeatureItem)
    }

    private fun handlePhysicalFeatureSelection(feature: PhysicalFeatureType) {
        when (feature) {
            PhysicalFeatureType.LIPS -> {
                Toast.makeText(this, getString(R.string.starting_lips_detection), Toast.LENGTH_SHORT).show()
                // Navigate to lips detection activity
                // startActivity(Intent(this, LipsDetectionActivity::class.java))
            }
            PhysicalFeatureType.NOSE -> {
                Toast.makeText(this, getString(R.string.starting_nose_detection), Toast.LENGTH_SHORT).show()
                // Navigate to nose detection activity
                // startActivity(Intent(this, NoseDetectionActivity::class.java))
            }
            PhysicalFeatureType.HAND -> {
                Toast.makeText(this, getString(R.string.starting_hand_detection), Toast.LENGTH_SHORT).show()
                // Navigate to hand detection activity
                // startActivity(Intent(this, HandDetectionActivity::class.java))
            }
        }
    }
}