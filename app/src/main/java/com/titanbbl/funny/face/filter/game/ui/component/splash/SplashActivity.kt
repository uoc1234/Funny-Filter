package com.titanbbl.funny.face.filter.game.ui.component.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.CountDownTimer
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.lifecycle.lifecycleScope
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.ads.RemoteConfigUtils
import com.titanbbl.funny.face.filter.game.app.AppConstants.KEY_SELECT_LANGUAGE
import com.titanbbl.funny.face.filter.game.databinding.ActivitySplashBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import com.titanbbl.funny.face.filter.game.utils.EasyPreferences.get
import com.titanbbl.funny.face.filter.game.utils.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SplashActivity : BaseActivity<ActivitySplashBinding>(), RemoteConfigUtils.Listener {
    private var getConfigSuccess = false
    private var splashActivity = false
    private var adsLoad = false
    private var selectLanguage = false

    private var canPersonalized = true

    override fun getLayoutActivity() = R.layout.activity_splash

    override fun initViews() {
        super.initViews()

        splashActivity = true
        selectLanguage = prefs[KEY_SELECT_LANGUAGE, false] == true


        val imageView = findViewById<ImageView>(R.id.img_background)

        val rotateAnimation = ObjectAnimator.ofFloat(imageView, "rotation", 0f, 360f).apply {
            duration = 3600 // thời gian quay 1 vòng (ms)
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }

        rotateAnimation.start()



        animateProgressBar(mBinding.customProgressBar, onEnd = {

        })

        RemoteConfigUtils.init(this)
        loadingRemoteConfig()
    }

    override fun observerData() {
        super.observerData()



    }
    fun animateProgressBar(progressBar: ProgressBar, onEnd: (() -> Unit)? = null) {
        val animator = ValueAnimator.ofInt(0, 100)
        animator.duration = 3000L // 5 giây
        animator.interpolator = LinearInterpolator()

        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Int
            progressBar.progress = progress
        }

        // Lắng nghe sự kiện kết thúc
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                onEnd?.invoke() // gọi callback nếu có
            }
        })

        animator.start()
    }



    private fun loadingRemoteConfig() {
        object : CountDownTimer(6500, 100) {
            override fun onTick(millisUntilFinished: Long) {
                if (getConfigSuccess && millisUntilFinished < 5500) {
                    checkRemoteConfigResult()
                    cancel()
                }
            }

            override fun onFinish() {
                if (!getConfigSuccess) {
                    checkRemoteConfigResult()
                }
            }
        }.start()
    }


    private fun checkRemoteConfigResult() {
        mBinding.customProgressBar.progress = 100
        lifecycleScope.launch(Dispatchers.IO){
            delay(2000)
            withContext(Dispatchers.Main){

                Routes.startMainActivity(this@SplashActivity)
            }
        }

//        if (adsLoad || AppPurchase.getInstance().isPurchased) {
//            moveActivity()
//            return
//        }
//        adsLoad = true
//
//        if (RemoteConfigUtils.getOnInterSplash() && isNetwork(this@SplashActivity)) {
//            BBLAd.getInstance().loadSplashInterstitialAds(
//                this@SplashActivity,
//                BuildConfig.admob_inter_splash,
//                25000, 500, object : BBLAdCallback() {
//                    override fun onNextAction() {
//                        super.onNextAction()
//                        moveActivity()
//                    }
//
//                })
//        } else {
//            moveActivity()
//        }
    }


    private fun moveActivity() {
//        if (!selectLanguage) {
//            Routes.startLanguageActivity(this, null)
//            finish()
//        } else {
//            Routes.startMainActivity(this)
//            finish()
//        }

    }

    override fun onResume() {
        super.onResume()

    }

    override fun onResizeViews() {
        super.onResizeViews()
    }

    override fun loadSuccess() {
        getConfigSuccess = true
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}