package com.titanbbl.funny.face.filter.game.ui.component.loading

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ActivityLoadingGameBinding
import com.titanbbl.funny.face.filter.game.app.AppConstants
import com.titanbbl.funny.face.filter.game.model.api.PredictionResponseItem
import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import com.titanbbl.funny.face.filter.game.ui.component.filter_activity.prediction.PredictionActivity
import com.titanbbl.funny.face.filter.game.model.PhysicalFeatureItem
import com.titanbbl.funny.face.filter.game.model.PhysicalFeatureType
import com.titanbbl.funny.face.filter.game.utils.Routes
import com.titanbbl.funny.face.filter.game.utils.parcelable
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class LoadingGameActivity : BaseActivity<ActivityLoadingGameBinding>() {

    companion object {
        private const val TAG = "LoadingGameActivity"
    }

    override fun getLayoutActivity(): Int {
        return R.layout.activity_loading_game
    }

    override fun initViews() {
        super.initViews()
        animateProgressBar(mBinding.customProgressBar){
            val type = intent.getStringExtra(AppConstants.KEY_CATEGORY) ?: ""
            moveActivityLoading(type)
            finish()
        }
    }

    fun moveActivityLoading(type: String) {
        when (type) {
            "LIP_FALL_CHALLENGE_FACE_PUZZLE" -> {

            }

            "LIP_FALL_CHALLENGE_ZOOM_PUZZLE" -> {

            }

            "DIY_DRAW_CHALLENGE_USE_NOSE" -> {
                Routes.startDrawByNoseActivity(this)
            }

            "DIY_DRAW_CHALLENGE_USE_FINGER" -> {
                Routes.startDrawByHandDetectActivity(this)
            }

            "DIY_DRAW_CHALLENGE_DRAWING" -> {
                Routes.startDrawActivity(this)
            }

            "POKA_RUNNN" -> {
                // Giải nén file pokemom.zip trước khi chuyển màn hình
                extractPokemonZipAndNavigate()
            }

            "PHYSICAL_FEATURES_LIP" -> {
                Routes.startNationalActivity(
                    this, PhysicalFeatureItem(
                        id = 1,
                        type = PhysicalFeatureType.LIPS,
                        title = "LIPS",
                        imageRes = R.drawable.physical_lip, // Replace with actual lips image
                        overlayImageRes = null // Add lips outline drawable if available
                    )
                )
            }

            "PHYSICAL_FEATURES_NOSE" -> {
                Routes.startNationalActivity(
                    this, PhysicalFeatureItem(
                        id = 2,
                        type = PhysicalFeatureType.NOSE,
                        title = "NOSE",
                        imageRes = R.drawable.physical_nose, // Replace with actual nose image
                        overlayImageRes = null // Add nose outline drawable if available
                    )
                )
            }

            "FACE_DELAY_VERTICAL" -> {
                Routes.startTimeDelayActivityVertical(this)
            }

            "FACE_DELAY_GRID" -> {
                Routes.startTimeDelayActivityGrid(this)
            }

            "PHYSICAL_FEATURES_HAND" -> {
                Routes.startNationalActivity(
                    this, PhysicalFeatureItem(
                        id = 3,
                        type = PhysicalFeatureType.HAND,
                        title = "HAND",
                        imageRes = R.drawable.physical_hand, // Replace with actual hand image
                        overlayImageRes = null // Use hand icon as overlay
                    )
                )
            }

            "WHO_YOU_LOOK_LIKE" -> {
                Routes.startWhoYouLookLikeActivity(this)
            }

            else -> {
                val item =
                    intent.parcelable<PredictionResponseItem>(AppConstants.KEY_PREDICTION_ITEM)
                val intent = Intent(this, PredictionActivity::class.java)
                intent.putExtra(AppConstants.KEY_PREDICTION_ITEM, item)
                startActivity(intent)
            }
        }
    }

    /**
     * Giải nén file pokemom.zip và chuyển sang PokaRunActivity
     */
    private fun extractPokemonZipAndNavigate() {
        try {
            Log.d(TAG, "Bắt đầu giải nén file pokemom.zip")
            
            // Tạo thư mục anim trong internal storage
            val animDir = File(filesDir, "anim")
            if (!animDir.exists()) {
                val dirCreated = animDir.mkdirs()
                Log.d(TAG, "Tạo thư mục anim: $dirCreated tại ${animDir.absolutePath}")
            }

            // Đường dẫn file zip đích
            val zipFile = File(animDir, "pokemom.zip")

            // Kiểm tra xem đã có file zip chưa
            if (!zipFile.exists()) {
                Log.d(TAG, "File pokemom.zip chưa có trong internal storage - copy từ assets")
                copyAssetToFile("pokemom.zip", zipFile)
                Log.d(TAG, "Đã copy pokemom.zip đến ${zipFile.absolutePath}, kích thước: ${zipFile.length()} bytes")
            } else {
                Log.d(TAG, "File pokemom.zip đã tồn tại tại ${zipFile.absolutePath}, kích thước: ${zipFile.length()} bytes")
            }

            // Kiểm tra file zip có hợp lệ không
            if (!zipFile.exists()) {
                Log.e(TAG, "File pokemom.zip không tồn tại tại ${zipFile.absolutePath} - chuyển màn hình trực tiếp")
                Routes.startPokaRunActivity(this)
                return
            }
            
            if (zipFile.length() == 0L) {
                Log.e(TAG, "File pokemom.zip rỗng (0 bytes) - chuyển màn hình trực tiếp")
                Routes.startPokaRunActivity(this)
                return
            }
            
            Log.d(TAG, "File pokemom.zip hợp lệ với kích thước ${zipFile.length()} bytes")

            // Kiểm tra xem đã giải nén chưa bằng cách kiểm tra số lượng file trong thư mục
            val extractedFiles = animDir.listFiles()?.filter { !it.name.endsWith(".zip") } ?: emptyList()
            
            if (extractedFiles.isNotEmpty()) {
                Log.d(TAG, "File pokemom.zip đã được giải nén trước đó với ${extractedFiles.size} files:")
                extractedFiles.forEach { file ->
                    Log.d(TAG, "  - ${file.name} (${if (file.isDirectory) "thư mục" else "${file.length()} bytes"})")
                }
                Log.d(TAG, "Chuyển màn hình PokaRun")
                Routes.startPokaRunActivity(this)
                return
            }

            // Giải nén file zip
            Log.d(TAG, "Bắt đầu giải nén file pokemom.zip")
            val extractionSuccess = extractZipWithZip4j(zipFile, animDir)

            // Kiểm tra giải nén thành công bằng cách đếm file được giải nén
            val extractedFilesAfter = animDir.listFiles()?.filter { !it.name.endsWith(".zip") } ?: emptyList()
            
            if (extractionSuccess && extractedFilesAfter.isNotEmpty()) {
                Log.d(TAG, "Giải nén pokemom.zip thành công với ${extractedFilesAfter.size} files:")
                extractedFilesAfter.forEach { file ->
                    Log.d(TAG, "  - ${file.name} (${if (file.isDirectory) "thư mục" else "${file.length()} bytes"})")
                }
                Log.d(TAG, "Chuyển màn hình PokaRun")
                Routes.startPokaRunActivity(this)
            } else {
                Log.e(TAG, "Giải nén pokemom.zip thất bại:")
                Log.e(TAG, "  - Extraction success: $extractionSuccess")
                Log.e(TAG, "  - Files extracted: ${extractedFilesAfter.size}")
                if (extractedFilesAfter.isEmpty()) {
                    Log.e(TAG, "  - Không có file nào được giải nén")
                } else {
                    extractedFilesAfter.forEach { file ->
                        Log.e(TAG, "    - ${file.name} (${if (file.isDirectory) "thư mục" else "${file.length()} bytes"})")
                    }
                }
                Log.e(TAG, "Chuyển màn hình trực tiếp")
                Routes.startPokaRunActivity(this)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi giải nén pokemom.zip", e)
            // Nếu có lỗi, vẫn chuyển màn hình
            Routes.startPokaRunActivity(this)
        }
    }

    /**
     * Copy file pokemom.zip từ assets đến internal storage
     */
    private fun copyAssetToFile(assetName: String, outputFile: File) {
        try {
            Log.d(TAG, "Copying asset $assetName đến ${outputFile.absolutePath}")

            // Kiểm tra asset có tồn tại không
            val assetsList = assets.list("")
            if (assetsList != null) {
                val assetExists = assetsList.contains(assetName)
                Log.d(TAG, "Asset $assetName tồn tại trong assets: $assetExists")
                if (!assetExists) {
                    Log.e(TAG, "Asset $assetName không tìm thấy trong thư mục assets")
                    return
                }
            }

            // Tạo thư mục cha nếu chưa tồn tại
            outputFile.parentFile?.mkdirs()

            // Copy file
            assets.open(assetName).use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                    }

                    Log.d(TAG, "Đã copy thành công $assetName, tổng bytes: $totalBytes")
                }
            }

        } catch (e: IOException) {
            Log.e(TAG, "Lỗi khi copy asset $assetName", e)
        }
    }

    /**
     * Giải nén file pokemom.zip sử dụng thư viện zip4j
     * @return true nếu giải nén thành công, false nếu thất bại
     */
    private fun extractZipWithZip4j(zipFile: File, destDir: File): Boolean {
        return try {
            Log.d(TAG, "Đang giải nén ${zipFile.absolutePath} đến ${destDir.absolutePath}")

            val zip = ZipFile(zipFile)

            // Kiểm tra tính hợp lệ của file zip
            if (!zip.isValidZipFile) {
                Log.e(TAG, "File zip không hợp lệ: ${zipFile.absolutePath}")
                return false
            }

            // Tạo thư mục đích nếu chưa tồn tại
            if (!destDir.exists()) {
                val dirCreated = destDir.mkdirs()
                Log.d(TAG, "Tạo thư mục đích: $dirCreated")
            }

            // Lấy danh sách entries
            val entries = zip.fileHeaders
            Log.d(TAG, "pokemom.zip chứa ${entries.size} entries:")

            for (entry in entries) {
                Log.d(TAG, "- ${entry.fileName} (${entry.uncompressedSize} bytes)")
            }

            // Giải nén tất cả files
            zip.extractAll(destDir.absolutePath)

            Log.d(TAG, "Giải nén pokemom.zip hoàn tất thành công")
            true

        } catch (e: ZipException) {
            Log.e(TAG, "Lỗi khi giải nén pokemom.zip với zip4j", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi không mong muốn khi giải nén pokemom.zip", e)
            false
        }
    }

    override fun onClickViews() {
        super.onClickViews()
    }

    fun animateProgressBar(progressBar: ProgressBar, onEnd: (() -> Unit)? = null) {
        val animator = ValueAnimator.ofInt(0, 100)
        animator.duration = 5000L // 5 giây
        animator.interpolator = LinearInterpolator()

        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Int
            progressBar.progress = progress
        }

        // Lắng nghe sự kiện kết thúc
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                onEnd?.invoke() // gọi callback nếu có
            }
        })

        animator.start()
    }
}