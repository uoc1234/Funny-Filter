package com.titanbbl.funny.face.filter.game.ui.bases
open class BaseState(val msg: String? = null) {
    fun getTitle(): String =
        if (msg != null) {
            val list = msg.split("|")
            if (list.size == 2) list[0] else ""
        } else ""

    fun getError(): String = if (msg != null) {
        val list = msg.split("|")
        if (list.size == 2) list[1] else ""
    } else ""
}