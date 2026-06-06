package com.titanbbl.funny.face.filter.game.utils

import android.app.Activity
import android.content.Intent
import com.titanbbl.funny.face.filter.game.app.AppConstants
import com.titanbbl.funny.face.filter.game.model.api.PredictionResponseItem
import com.titanbbl.funny.face.filter.game.ui.component.filter_activity.draw.activity.DrawByNoseActivity
import com.titanbbl.funny.face.filter.game.ui.component.filter_activity.draw.activity.HandDetectActivity
import com.titanbbl.funny.face.filter.game.ui.component.filter_activity.draw.activity.HandDrawingActivity
import com.titanbbl.funny.face.filter.game.ui.component.filter_activity.frame_delay.TimeDelayGridActivity
import com.titanbbl.funny.face.filter.game.ui.component.filter_activity.frame_delay.TimeDelayVerticalActivity
import com.titanbbl.funny.face.filter.game.ui.component.filter_activity.pokarun.PokaRunActivity
import com.titanbbl.funny.face.filter.game.ui.component.filter_activity.prediction_national.HumanNationalActivity
import com.titanbbl.funny.face.filter.game.ui.component.filter_activity.who_u_like.WhoLookLikeActivity
import com.titanbbl.funny.face.filter.game.ui.component.guess_challenge.PhysicalChallengeActivity
import com.titanbbl.funny.face.filter.game.ui.component.guess_challenge.PredictionChallengeActivity
import com.titanbbl.funny.face.filter.game.model.PhysicalFeatureItem
import com.titanbbl.funny.face.filter.game.ui.component.loading.LoadingGameActivity
import com.titanbbl.funny.face.filter.game.ui.component.main.MainActivity
import com.titanbbl.funny.face.filter.game.ui.component.music.SelectMusicActivity
import com.titanbbl.funny.face.filter.game.ui.component.result.ResultActivity

object Routes {
    fun startMainActivity(fromActivity: Activity) =
        Intent(fromActivity, MainActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            fromActivity.startActivity(this)
        }


    fun startFilterActivity(type: String, fromActivity: Activity) {
        when (type) {
            "LIP_FALL_CHALLENGE_FACE_PUZZLE",
            "LIP_FALL_CHALLENGE_ZOOM_PUZZLE",

            "DIY_DRAW_CHALLENGE_USE_NOSE",
            "DIY_DRAW_CHALLENGE_USE_FINGER",
            "DIY_DRAW_CHALLENGE_DRAWING",

            "POKA_RUNNN",

            "FACE_DELAY_VERTICAL",
            "FACE_DELAY_GRID",

            "PHYSICAL_FEATURES_LIP",
            "PHYSICAL_FEATURES_HAND",
            "PHYSICAL_FEATURES_NOSE"
                -> {
                startLoadingActivity(fromActivity, type)
            }


            else -> {
                startPredictionChallengeActivity(fromActivity)
            }
        }
    }

    fun startLoadingActivity(fromActivity: Activity, category: String, item: PredictionResponseItem ? = null) =
        Intent(fromActivity, LoadingGameActivity::class.java).apply {
            putExtra(AppConstants.KEY_CATEGORY, category)
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)

            if (item != null) {
                putExtra(AppConstants.KEY_PREDICTION_ITEM, item)
            }

            fromActivity.startActivity(this)
        }

    fun startPhysicalChallengeActivity(fromActivity: Activity) =
        Intent(fromActivity, PhysicalChallengeActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            fromActivity.startActivity(this)
        }

    fun startPredictionChallengeActivity(fromActivity: Activity) {
        Intent(fromActivity, PredictionChallengeActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            fromActivity.startActivity(this)
        }
    }

    fun startNationalActivity(fromActivity: Activity, physicalFeature: PhysicalFeatureItem ) =
        Intent(fromActivity, HumanNationalActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            putExtra("physicalFeature", physicalFeature)
            fromActivity.startActivity(this)
        }

    fun startTimeDelayActivityVertical(fromActivity: Activity) =
        Intent(fromActivity, TimeDelayVerticalActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            fromActivity.startActivity(this)
        }

    fun startTimeDelayActivityGrid(fromActivity: Activity) =
        Intent(fromActivity, TimeDelayGridActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            fromActivity.startActivity(this)
        }
    fun addTrackingMoveScreen(fromActivity: String, toActivity: String) {
        BBLTrackingHelper.fromScreenToScreen(fromActivity,toActivity)
    }

    fun startResultActivity(fromActivity: Activity, videoPath: String) =
        Intent(fromActivity, ResultActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            putExtra("video_path", videoPath)
            fromActivity.startActivity(this)
        }

    fun startDrawByNoseActivity(activity: Activity) {
        Intent(activity, DrawByNoseActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, activity::class.java.simpleName)
            activity.startActivity(this)
        }
    }


    fun startDrawByHandDetectActivity(activity: Activity) {
        Intent(activity, HandDetectActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, activity::class.java.simpleName)
            activity.startActivity(this)
        }
    }

    fun startDrawActivity(activity: Activity) {
        Intent(activity, HandDrawingActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, activity::class.java.simpleName)
            activity.startActivity(this)
        }
    }

    fun startPokaRunActivity(activity: Activity) {
        Intent(activity, PokaRunActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, activity::class.java.simpleName)
            activity.startActivity(this)
        }
    }

    fun startHandDetectActivity(activity: Activity) {
        Intent(activity, HandDetectActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, activity::class.java.simpleName)
            activity.startActivity(this)
        }
    }

    fun startSelectMusicActivity(activity: Activity) {
        Intent(activity, SelectMusicActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, activity::class.java.simpleName)
            activity.startActivity(this)
        }

    }

    fun startWhoYouLookLikeActivity(activity: Activity) {
        Intent(activity, WhoLookLikeActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, activity::class.java.simpleName)
            activity.startActivity(this)
        }
    }

    fun startHomeActivity(activity: Activity) {
        Intent(activity, MainActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, activity::class.java.simpleName)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(this)
        }
    }

}