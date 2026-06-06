package com.titanbbl.funny.face.filter.game.ui.component.main

import android.graphics.Color
import android.util.Log
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.titanbbl.funny.face.filter.game.BuildConfig
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ActivityMainBinding
import com.titanbbl.funny.face.filter.game.app.MusicManagerApp
import com.titanbbl.funny.face.filter.game.model.api.Song
import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import com.titanbbl.funny.face.filter.game.ui.bases.ext.isNetwork
import com.titanbbl.funny.face.filter.game.ui.component.guess_challenge.viewmodel.PredictionViewModel
import com.titanbbl.funny.face.filter.game.ui.component.main.fragments.CollectionFragment
import com.titanbbl.funny.face.filter.game.ui.component.main.fragments.GameFragment
import com.titanbbl.funny.face.filter.game.ui.component.main.fragments.HomeFragment
import com.titanbbl.funny.face.filter.game.ui.component.main.fragments.SettingFragment
import com.downloader.Error
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : BaseActivity<ActivityMainBinding>() {


    var tabSelection = 0


    private val homeFragment by lazy { HomeFragment.newInstance() }
    private val gameFragment by lazy { GameFragment.newInstance() }
    private val collectionFragment by lazy { CollectionFragment.newInstance() }
    private val settingFragment by lazy { SettingFragment.newInstance() }

    private val viewModel: PredictionViewModel by viewModels { PredictionViewModel.provideFactory() }

    private var activeFragment: Fragment = homeFragment

    override fun getLayoutActivity(): Int = R.layout.activity_main

    override fun initViews() {
        super.initViews()
        setupFragments()
        setupNavigation()
        if (isNetwork()) {
            downloadSound1()
        }
    }

    fun downloadSound1() {
        val song = Song(
            id = 1,
            title = "Super man",
            url = "https://storage.bunnycdn.com/bblprivate/funnyfilter/sound/sound_1.mp3",
            duration = "01:00"
        )

        val fileName = "${song.id}_${song.title.replace(" ", "_")}.mp3"
        val downloadDir = File(filesDir, "music")
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }

        PRDownloader.download(song.url, downloadDir.absolutePath, fileName)
            .setHeader("accessKey", BuildConfig.BUNNY_CDN_ACCESS_KEY).build()
            .setOnStartOrResumeListener {

            }.setOnProgressListener { progress ->


            }.start(object : OnDownloadListener {
                override fun onDownloadComplete() {
                    MusicManagerApp.saveSelectedSong(song)
                }

                override fun onError(p0: Error?) {

                }

            })
    }


    override fun observerData() {
        super.observerData()

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                // Observe predictions
                viewModel.predictions.collect { predictions ->
                    Log.d("PredictionChallenge", "Received ${predictions.size} predictions")

                }
            }
        }


    }


    private fun setupFragments() {
        // Initialize fragments
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, homeFragment)
            add(R.id.fragment_container, gameFragment).hide(gameFragment)
            add(R.id.fragment_container, collectionFragment).hide(collectionFragment)
            add(R.id.fragment_container, settingFragment).hide(settingFragment)
        }.commit()
    }

    private fun setupNavigation() {
        // Set up bottom navigation click listeners
        mBinding.navHome.setOnClickListener {
            homeFragment.onResume()
            switchFragment(homeFragment)
            updateNavigationUI(0)
            homeFragment.onResume()
        }

        mBinding.navGame.setOnClickListener {
            homeFragment.onPause()
            switchFragment(gameFragment)
            updateNavigationUI(1)
        }

        mBinding.navCollection.setOnClickListener {
            homeFragment.onPause()
            switchFragment(collectionFragment)
            updateNavigationUI(2)
        }

        mBinding.navSetting.setOnClickListener {
            homeFragment.onPause()
            switchFragment(settingFragment)
            updateNavigationUI(3)
        }

        // Set initial selection
        updateNavigationUI(0)
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().hide(activeFragment).show(fragment).commit()
        activeFragment = fragment
    }


    private fun updateNavigationUI(selectedIndex: Int) {
        tabSelection = selectedIndex
        // Reset all icons to default
        mBinding.imgHome.setColorFilter(getColor(android.R.color.white))
        mBinding.imgGame.setColorFilter(getColor(android.R.color.white))
        mBinding.imgCollection.setColorFilter(getColor(android.R.color.white))
        mBinding.imgSetting.setColorFilter(getColor(android.R.color.white))
        mBinding.tvHome.setTextColor(getColor(android.R.color.white))
        mBinding.tvHomeGame.setTextColor(getColor(android.R.color.white))
        mBinding.tvHomeCollection.setTextColor(getColor(android.R.color.white))
        mBinding.tvHomeSetting.setTextColor(getColor(android.R.color.white))

        mBinding.bottomNavigation.setBackgroundColor(Color.BLACK)

        // Highlight selected icon
        when (selectedIndex) {
            0 -> {
                mBinding.imgHome.setColorFilter(ContextCompat.getColor(this, R.color.color_FFE761))
                mBinding.tvHome.setTextColor(ContextCompat.getColor(this, R.color.color_FFE761))
            }

            1 -> {
                setBlackView()
                mBinding.bottomNavigation.setBackgroundColor(Color.WHITE)
                mBinding.imgGame.setColorFilter(ContextCompat.getColor(this, R.color.color_FFE761))
                mBinding.tvHomeGame.setTextColor(ContextCompat.getColor(this, R.color.color_FFE761))
            }

            2 -> {
                setBlackView()
                mBinding.bottomNavigation.setBackgroundColor(Color.WHITE)
                mBinding.imgCollection.setColorFilter(
                    ContextCompat.getColor(
                        this, R.color.color_FFE761
                    )
                )
                mBinding.tvHomeCollection.setTextColor(
                    ContextCompat.getColor(
                        this, R.color.color_FFE761
                    )
                )
            }

            3 -> {
                setBlackView()
                mBinding.bottomNavigation.setBackgroundColor(Color.WHITE)
                mBinding.imgSetting.setColorFilter(
                    ContextCompat.getColor(
                        this, R.color.color_FFE761
                    )
                )
                mBinding.tvHomeSetting.setTextColor(
                    ContextCompat.getColor(
                        this, R.color.color_FFE761
                    )
                )
            }
        }
    }

    fun setBlackView() {

        mBinding.imgHome.setColorFilter(getColor(android.R.color.black))
        mBinding.imgGame.setColorFilter(getColor(android.R.color.black))
        mBinding.imgCollection.setColorFilter(getColor(android.R.color.black))
        mBinding.imgSetting.setColorFilter(getColor(android.R.color.black))

        mBinding.tvHome.setTextColor(getColor(android.R.color.black))
        mBinding.tvHomeGame.setTextColor(getColor(android.R.color.black))
        mBinding.tvHomeCollection.setTextColor(getColor(android.R.color.black))
        mBinding.tvHomeSetting.setTextColor(getColor(android.R.color.black))
    }
}
