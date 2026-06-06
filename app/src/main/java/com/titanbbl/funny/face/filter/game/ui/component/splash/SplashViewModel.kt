package com.titanbbl.funny.face.filter.game.ui.component.splash

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.titanbbl.funny.face.filter.game.ui.bases.BaseViewModel

class SplashViewModel : BaseViewModel() {
    companion object {
        fun provideFactory(): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SplashViewModel()
            }
        }
    }
}