package com.titanbbl.funny.face.filter.game.ui.component.guess_challenge

import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ActivityGuessChallengeBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import com.titanbbl.funny.face.filter.game.ui.component.dialog.DialogLoading
import com.titanbbl.funny.face.filter.game.ui.component.guess_challenge.adapter.GuessItemAdapter
import com.titanbbl.funny.face.filter.game.ui.component.guess_challenge.viewmodel.PredictionViewModel
import com.titanbbl.funny.face.filter.game.utils.Routes
import kotlinx.coroutines.launch

class PredictionChallengeActivity : BaseActivity<ActivityGuessChallengeBinding>() {

    private val adapter = GuessItemAdapter()
    private val dialogLoading by lazy { DialogLoading(this) }

    private val viewModel: PredictionViewModel by viewModels { PredictionViewModel.provideFactory() }

    override fun getLayoutActivity(): Int {
        return R.layout.activity_guess_challenge
    }

    override fun onClickViews() {
        super.onClickViews()
        
        mBinding.apply {
            btnBack.setOnClickListener {
                finish()
            }

            btnCheck.setOnClickListener {

            }
        }


//intent.parcelable(AppConstants.KEY_PREDICTION_ITEM)

        adapter.onItemClick = { item ->

            Log.d("PredictionChallenge", "Item clicked: ${item.question}")
            Routes.startLoadingActivity(this, "PREDICTION_CHALLENGE", item)
        }
        dialogLoading.show()

    }

    override fun initViews() {
        super.initViews()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        mBinding.rvGuessItems.adapter = adapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                // Observe predictions
                viewModel.predictions.collect { predictions ->
                    Log.d("PredictionChallenge", "Received ${predictions.size} predictions")
                    adapter.submitList(predictions)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                // Observe loading state
                viewModel.isLoading.collect { isLoading ->
                    Log.d("PredictionChallenge", "Loading: $isLoading")
                    // You can show/hide loading indicator here
                    if (isLoading) {
                        dialogLoading.show()

                    } else {
                        dialogLoading.dismiss()

                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                // Observe error state
                viewModel.error.collect { error ->
                    error?.let {
                        Log.e("PredictionChallenge", "Error: $it")
                        Toast.makeText(this@PredictionChallengeActivity, "Error: $it", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (dialogLoading.isShowing){
            dialogLoading.dismiss()
        }

        super.onDestroy()

    }
}