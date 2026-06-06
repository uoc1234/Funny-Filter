package com.titanbbl.funny.face.filter.game.ui.bases.ext

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun <T> CoroutineScope.observeFlow(flow: Flow<T>,  action: (t: T) -> Unit) {
    launch {
        flow.collect { value -> action(value) }
    }
}