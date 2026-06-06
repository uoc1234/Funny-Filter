package com.titanbbl.funny.face.filter.game.ui.component.main.fragments

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.FragmentSettingBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseFragment

class SettingFragment : BaseFragment<FragmentSettingBinding>() {

    override fun getLayoutFragment(): Int = R.layout.fragment_setting

    override fun initViews() {
        // Initialize views if needed
    }

    override fun onClickViews() {
        // Feedback & Support
        mBinding.llFeedback.setOnClickListener {
            openFeedbackAndSupport()
        }

        // Privacy Policy
        mBinding.llPrivacy.setOnClickListener {
            openPrivacyPolicy()
        }

        // Share App
        mBinding.llShare.setOnClickListener {
            shareApp()
        }

        // Rate App
        mBinding.llRate.setOnClickListener {
            rateApp()
        }
    }

    private fun openFeedbackAndSupport() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // Chỉ định rõ ràng scheme để tránh lỗi
            putExtra(Intent.EXTRA_EMAIL, arrayOf("binarybirdge@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Feedback for " + getString(R.string.app_name))
        }

        context?.packageManager?.let {
            if (intent.resolveActivity(it) != null) {
                context?.startActivity(intent)
            } else {
                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openPrivacyPolicy() {
        val url = "https://sites.google.com/view/guess-filter-funny-face-titan/home"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    private fun shareApp() {
        // Share app functionality
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_text, requireContext().packageName))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_app_subject))
        }
        
        startActivity(Intent.createChooser(intent, getString(R.string.share_app_chooser_title)))
    }

    private fun rateApp() {
        // Open app in Play Store for rating
        val packageName = requireContext().packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    companion object {
        fun newInstance() = SettingFragment()
    }
} 