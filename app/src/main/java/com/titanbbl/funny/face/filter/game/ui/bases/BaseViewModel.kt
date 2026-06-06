package com.titanbbl.funny.face.filter.game.ui.bases

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren

open class BaseViewModel(protected val savedStateHandle: SavedStateHandle = SavedStateHandle()) : ViewModel() {

    //init ViewModel Scope
    var viewModelJob = SupervisorJob()
    var uiDispatchers = Dispatchers.Main
    var ioDispatchers = Dispatchers.IO
    var defaultDispatchers = Dispatchers.Default
    var uiScope = CoroutineScope(uiDispatchers + viewModelJob)
    var bgScope = CoroutineScope(ioDispatchers + viewModelJob)

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
        uiScope.coroutineContext.cancelChildren()
        bgScope.coroutineContext.cancelChildren()
    }

    fun <T> MutableLiveData<T>.toLiveData(): LiveData<T> = this
}