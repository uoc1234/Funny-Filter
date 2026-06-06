package com.titanbbl.funny.face.filter.game.ui.component.dialog

import android.content.Context
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.DialogLoadingBinding
import com.titanbbl.funny.face.filter.game.ui.bases.BaseDialog

/**
\\ \\ \\ \\ \\ \\ \\ \\ || || || || || || // // // // // // // //
\\ \\ \\ \\ \\ \\ \\        _ooOoo_          // // // // // // //
\\ \\ \\ \\ \\ \\          o8888888o            // // // // // //
\\ \\ \\ \\ \\             88" . "88               // // // // //
\\ \\ \\ \\                (| -_- |)                  // // // //
\\ \\ \\                   O\  =  /O                     // // //
\\ \\                   ____/`---'\____                     // //
\\                    .'  \\|     |//  `.                      //
==                   /  \\|||  :  |||//  \                     ==
==                  /  _||||| -:- |||||-  \                    ==
==                  |   | \\\  -  /// |   |                    ==
==                  | \_|  ''\---/''  |   |                    ==
==                  \  .-\__  `-`  ___/-. /                    ==
==                ___`. .'  /--.--\  `. . ___                  ==
==              ."" '<  `.___\_<|>_/___.'  >'"".               ==
==            | | :  `- \`.;`\ _ /`;.`/ - ` : | |              \\
//            \  \ `-.   \_ __\ /__ _/   .-` /  /              \\
//      ========`-.____`-.___\_____/___.-`____.-'========      \\
//                           `=---='                           \\
// //   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^  \\ \\
// // //    Buddha blessed    Never BUG    Never modify   \\ \\ \\
 **/
class DialogLoading(val mContext: Context) : BaseDialog<DialogLoadingBinding>(mContext, R.style.ThemeDialogWrapcontent) {

    override fun getLayoutDialog(): Int = R.layout.dialog_loading

    override fun initViews() {
        // Initialize views if needed
    }

    override fun onResizeViews() {
        // Handle view resizing if needed
    }

    override fun onClickViews() {
        // Handle click events if needed
    }
}