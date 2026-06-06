package com.titanbbl.funny.face.filter.game.ui.component.custom

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.titanbbl.funny.face.filter.game.R

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

class AdapterInfinity(
    private val context: Context,
    private val onFlagSelected: ((String) -> Unit)? = null
) : RecyclerView.Adapter<AdapterInfinity.FlagViewHolder>() {

    private val flagList = mutableListOf<String>()
    private var selectedPosition = -1
    private var isScrolling = false

    init {
        setupFlagList()
    }

    private fun setupFlagList() {
        // Thêm một số item cuối vào đầu để tạo hiệu ứng vô cực mượt mà
        for (i in 60..64) {
            flagList.add("flat_$i.png") // Thêm 5 item cuối vào đầu
        }
        
        // Danh sách cờ chính
        for (i in 1..64) {
            flagList.add("flat_$i.png")
        }
        
        // Thêm một số item đầu vào cuối để tạo hiệu ứng vô cực mượt mà
        for (i in 1..5) {
            flagList.add("flat_$i.png") // Thêm 5 item đầu vào cuối
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlagViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_flag_infinity, parent, false)
        return FlagViewHolder(view)
    }

    override fun onBindViewHolder(holder: FlagViewHolder, position: Int) {
        if (position >= 0 && position < flagList.size) {
            val flagName = flagList[position]
            val realPosition = getRealPosition(position)
            val flagIndex = realPosition + 1 // Cờ bắt đầu từ 1 (1..64)
            
            holder.bind(flagName, position == selectedPosition, isScrolling, flagIndex)
        }
    }

    override fun getItemCount(): Int = flagList.size

    fun setScrolling(scrolling: Boolean) {
        isScrolling = scrolling
        notifyDataSetChanged()
    }

    fun setSelectedPosition(position: Int) {
        if (position >= 0 && position < flagList.size) {
            selectedPosition = position
            notifyDataSetChanged()
            val flagIndex = getRealPosition(position) + 1
            onFlagSelected?.invoke("flat_$flagIndex")
        }
    }

    fun getRealPosition(position: Int): Int {
        // Do có 5 item padding ở đầu, trừ đi 5 để lấy vị trí thực trong 0..63
        return ((position - 5 + 64) % 64)
    }

    inner class FlagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val flagImageView: ImageView = itemView.findViewById(R.id.ivFlag)

        fun bind(flagName: String, isSelected: Boolean, isScrolling: Boolean, flagIndex: Int) {
            try {
                // Load image từ assets
                val inputStream = context.assets.open("flat_natitional/$flagName")
                val drawable = android.graphics.drawable.Drawable.createFromStream(inputStream, null)
                flagImageView.setImageDrawable(drawable)
                inputStream.close()
            } catch (e: Exception) {
                // Fallback nếu không load được image
                flagImageView.setImageResource(R.drawable.ic_launcher_background)
                e.printStackTrace()
            }

            // Xử lý hiệu ứng mờ/sáng
            try {
                when {
                    isScrolling -> {
                        // Khi đang scroll - tất cả đều sáng
                        flagImageView.alpha = 1.0f
                        flagImageView.scaleX = 1.0f
                        flagImageView.scaleY = 1.0f
                    }
                    isSelected -> {
                        // Khi dừng và được chọn (ở giữa) - sáng và to hơn
                        flagImageView.alpha = 1.0f
                        flagImageView.scaleX = 1.2f
                        flagImageView.scaleY = 1.2f
                    }
                    else -> {
                        // Khi dừng và không được chọn (xung quanh) - mờ
                        flagImageView.alpha = 0.4f
                        flagImageView.scaleX = 1.0f
                        flagImageView.scaleY = 1.0f
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            itemView.setOnClickListener {
                try {
                    val currentPosition = adapterPosition
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        // Dừng scroll và chọn item ngay lập tức khi click
                        setSelectedPosition(currentPosition)
                        // Thông báo ra ngoài để dừng scroll với vị trí thực
                        val realIndex = getRealPosition(currentPosition) + 1
                        onFlagSelected?.invoke("stop_scroll_${realIndex}:${currentPosition}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}