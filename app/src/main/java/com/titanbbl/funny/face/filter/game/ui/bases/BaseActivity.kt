package com.titanbbl.funny.face.filter.game.ui.bases

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.MutableLiveData
import com.titanbbl.funny.face.filter.game.app.AppConstants
import com.titanbbl.funny.face.filter.game.app.MusicManagerApp
import com.titanbbl.funny.face.filter.game.ui.bases.ext.isNetwork
import com.titanbbl.funny.face.filter.game.ui.component.dialog.DialogConnect
import com.titanbbl.funny.face.filter.game.ui.component.dialog.DialogNetworkManager
import com.titanbbl.funny.face.filter.game.utils.BBLTrackingHelper
import com.titanbbl.funny.face.filter.game.utils.EasyPreferences
import com.titanbbl.funny.face.filter.game.utils.Routes
import java.util.Locale

private const val TAG = "BaseActivity"


abstract class BaseActivity<VB : ViewDataBinding> : AppCompatActivity() {


    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var legacyReceiver: BroadcastReceiver? = null


    lateinit var mBinding: VB
    lateinit var prefs: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = EasyPreferences.defaultPrefs(this)

        setLocal()

        requestWindow()
        val layoutView = getLayoutActivity()
        mBinding = DataBindingUtil.setContentView(this, layoutView)
        Log.d(TAG, "onCreate: name Class: ${this::class.java.simpleName}")
        BBLTrackingHelper.addScreenTrack(this::class.java.simpleName)
        if (intent.getStringExtra(AppConstants.KEY_TRACKING_SCREEN_FROM)!=null){
            Routes.addTrackingMoveScreen(intent.getStringExtra(AppConstants.KEY_TRACKING_SCREEN_FROM).toString(),this::class.java.simpleName )
        }
        mBinding.lifecycleOwner = this

        initViews()
        onResizeViews()
        onClickViews()
        observerData()

        DialogNetworkManager.initDialogLoading(this)

        connectivityManager = getSystemService(ConnectivityManager::class.java)

        liveDataNetwork.observe(this) {
            if (it){
                DialogNetworkManager.dismissDialogLoading()
            }else{
                DialogNetworkManager.showDialogLoading()
            }
        }


        if (!isNetwork()) {
            Log.d(TAG, "onCreate: No internet connection")
            DialogConnect.show(this) {
                openInternetSettings()
            }
        } else {
            Log.d(TAG, "onCreate: Internet connection is available")
        }
    }


    private val internetPanelLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (isNetwork()){

        }else {
            DialogConnect.show(this) {
                openInternetSettings()
            }

        }
    }

    private fun openInternetSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: Internet Connectivity Panel (bật Wi-Fi/Data tại chỗ)
            val intent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            // startActivity(intent) // đơn giản
            internetPanelLauncher.launch(intent) // nếu muốn callback khi quay lại
        } else {
            // Fallback cho máy cũ
            try {
                startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            } catch (_: Exception) {
                try {
                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                } catch (_: Exception) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }
    }


    override fun onStart() {
        super.onStart()
        registerNetworkObserver()
        // bắn trạng thái hiện tại ngay khi vào màn
        sendNetworkBroadcast(isCurrentlyConnected())
    }

    override fun onStop() {
        super.onStop()
        unregisterNetworkObserver()
    }

    val liveDataNetwork: MutableLiveData<Boolean> = MutableLiveData(true)


    private fun registerNetworkObserver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val request =
                NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    sendNetworkBroadcast(true)
                    liveDataNetwork.postValue(true)
                }

                override fun onLost(network: Network) {
                    sendNetworkBroadcast(false)
                    liveDataNetwork.postValue(false)
                }

                override fun onUnavailable() {
                    sendNetworkBroadcast(false)
                    liveDataNetwork.postValue(false)
                }
            }
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } else {
            // Legacy cho < 24
            legacyReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: android.content.Context?, intent: Intent?) {
                    sendNetworkBroadcast(isCurrentlyConnected())
                }
            }
            @Suppress("DEPRECATION") registerReceiver(
                legacyReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            )
        }
    }

    private fun unregisterNetworkObserver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback?.let {
                try {
                    connectivityManager.unregisterNetworkCallback(it)
                } catch (_: Exception) {
                }
            }
            networkCallback = null
        } else {
            legacyReceiver?.let {
                try {
                    unregisterReceiver(it)
                } catch (_: Exception) {
                }
            }
            legacyReceiver = null
        }
    }

    private fun isCurrentlyConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = connectivityManager.activeNetwork ?: return false
            val caps = connectivityManager.getNetworkCapabilities(nw) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )
        } else {
            @Suppress("DEPRECATION") connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true
        }
    }

    private fun sendNetworkBroadcast(isConnected: Boolean) {

    }


    open fun setUpViews() {}

    abstract fun getLayoutActivity(): Int

    open fun requestWindow() {}

    open fun initViews() {}

    open fun onResizeViews() {}

    open fun onClickViews() {}

    open fun observerData() {}

    private fun setLocal(){
        val language: String? = prefs.getString(AppConstants.KEY_LANGUAGE, "")
        if (language == "") {
            val config = Configuration()
            val locale = Locale.getDefault()
            Locale.setDefault(locale)
            config.locale = locale
            resources.updateConfiguration(config, resources.displayMetrics)
        } else {
            if (language.equals("", ignoreCase = true)) return
            val locale = Locale(language)
            Locale.setDefault(locale)
            val config = Configuration()
            config.locale = locale
            resources.updateConfiguration(config, resources.displayMetrics)
        }
    }


    override fun onResume() {
        super.onResume()
        hideNavigationBar()

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hideNavigationBar()

    }

    private fun hideNavigationBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            hideSystemUIBeloR()
        }

    }

    private fun hideSystemUIBeloR() {
        val decorView: View = window.decorView
        val uiOptions = decorView.systemUiVisibility
        var newUiOptions = uiOptions
        newUiOptions = newUiOptions or View.SYSTEM_UI_FLAG_LOW_PROFILE
        newUiOptions = newUiOptions or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        newUiOptions = newUiOptions or View.SYSTEM_UI_FLAG_IMMERSIVE
        newUiOptions = newUiOptions or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        decorView.systemUiVisibility = newUiOptions
    }

    override fun onDestroy() {
        super.onDestroy()
        DialogNetworkManager.dismissDialogLoading()
        MusicManagerApp.stopMusic()
    }
}