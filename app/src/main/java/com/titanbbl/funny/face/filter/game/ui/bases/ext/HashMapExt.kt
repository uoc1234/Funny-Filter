package com.titanbbl.funny.face.filter.game.ui.bases.ext

fun <K, V> MutableMap<K, V>.replaceKey(oldKey: K, newKey: K) {
    if (this.containsKey(oldKey)) {
        val value = this[oldKey]
        this.remove(oldKey)
        if (value != null) {
            this[newKey] = value
        }
    }
}