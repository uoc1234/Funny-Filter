package com.titanbbl.funny.face.filter.game.ui.component.main.fragments

import android.app.Activity
import android.content.Intent
import androidx.recyclerview.widget.GridLayoutManager
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.FragmentGameBinding
import com.titanbbl.funny.face.filter.game.model.GameItem
import com.titanbbl.funny.face.filter.game.ui.bases.BaseFragment
import com.titanbbl.funny.face.filter.game.ui.component.guess_challenge.DIYDrawChallengeActivity
import com.titanbbl.funny.face.filter.game.ui.component.guess_challenge.TimeDelayChallengeActivity
import com.titanbbl.funny.face.filter.game.ui.component.main.adapter.GameAdapter
import com.titanbbl.funny.face.filter.game.utils.Routes
import com.titanbbl.funny.face.filter.game.utils.Routes.startLoadingActivity
import kotlin.jvm.java

class GameFragment : BaseFragment<FragmentGameBinding>() {

    private lateinit var gameAdapter: GameAdapter

    override fun getLayoutFragment(): Int {
        return R.layout.fragment_game
    }

    override fun initViews() {
        super.initViews()
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val gameItems = listOf(
            GameItem(1, getString(R.string.guess_challenge), R.drawable.img_game_guess_challenge),
            GameItem(3, getString(R.string.diy_draw_challenge), R.drawable.img_game_diy_draw),
            GameItem(4, getString(R.string.poka_runnn), R.drawable.img_game_poka_runnn),
            GameItem(5, getString(R.string.physical_features), R.drawable.img_game_physical_features),
            GameItem(6, getString(R.string.face_delay), R.drawable.img_game_face_delay),
            GameItem(7, getString(R.string.who_you_look_like), R.drawable.img_game_who_you_look_like)
        )

        gameAdapter = GameAdapter(gameItems) { gameItem ->
            // Handle game item click
            // Example: Navigate to game detail/start game

            when(gameItem.id) {
                1 ->{
                    Routes.startPredictionChallengeActivity(requireContext() as Activity)
                }
                2 ->{

                }
                3 ->{
                    startActivity(Intent(requireContext(), DIYDrawChallengeActivity::class.java))
                }
                4 ->{
                    startLoadingActivity(requireContext() as Activity, "POKA_RUNNN",)
                }
                5 ->{
                    Routes.startPhysicalChallengeActivity(requireContext() as Activity)
                }
                6 ->{
                    startActivity(Intent(requireContext(), TimeDelayChallengeActivity::class.java))
                }
                7 ->{
                    startLoadingActivity(requireContext() as Activity, "WHO_YOU_LOOK_LIKE", )
                }
                else -> {
                    // Handle other game items if needed
                }
            }
        }

        mBinding.rvGames.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = gameAdapter
        }
    }

    companion object {
        fun newInstance() = GameFragment()
    }
}