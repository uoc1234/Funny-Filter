package com.titanbbl.funny.face.filter.game.ui.component.music

import android.app.Activity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.titanbbl.funny.face.filter.game.R
import com.titanbbl.funny.face.filter.game.databinding.ActivityMusicSelectBinding
import com.titanbbl.funny.face.filter.game.app.MusicManagerApp
import com.titanbbl.funny.face.filter.game.model.api.Song
import com.titanbbl.funny.face.filter.game.ui.bases.BaseActivity
import android.util.Log
import com.titanbbl.funny.face.filter.game.ui.component.dialog.DialogLoading

class SelectMusicActivity : BaseActivity<ActivityMusicSelectBinding>() {

    private val viewModel: MusicViewModel by viewModels()
    private lateinit var musicAdapter: MusicAdapter
    private var selectedSong: Song? = null

    private val dialogLoading by lazy { DialogLoading(this) }

    override fun getLayoutActivity(): Int {
        return R.layout.activity_music_select
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // MusicManagerApp đã được khởi tạo trong GlobalApp
        // Không cần gọi init() lại
        
        setupViews()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        // Load bài hát đã chọn trước đó (nếu có)
        loadPreviouslySelectedSong()
    }

    private fun setupViews() {
        // Set up the title
    }

    private fun setupRecyclerView() {
        musicAdapter = MusicAdapter(this, mutableListOf()) { song, position ->
            // Handle item selection
            selectedSong = song

            
            Log.d("SelectMusicActivity", "Selected song: ${song.title}")
        }

        mBinding.rvMusic.apply {
            layoutManager = LinearLayoutManager(this@SelectMusicActivity)
            adapter = musicAdapter
        }
        
        // Set RecyclerView reference for adapter to update UI during download
        musicAdapter.setRecyclerView(mBinding.rvMusic)
    }

    private fun setupClickListeners() {
        // Back button
        mBinding.btnBack.setOnClickListener {
            onBackPressed()
        }

        // Done button
        mBinding.btnDone.setOnClickListener {
            selectedSong?.let { song ->
                // Lưu bài hát được chọn vào MusicManager
                MusicManagerApp.saveSelectedSong(song)
                
                Log.d("SelectMusicActivity", "Done button clicked, saved song: ${song.title}")
                
                // Set result để activity gọi biết việc chọn nhạc đã hoàn thành
                setResult(Activity.RESULT_OK)
            }
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.musicList.observe(this) { songs ->
            musicAdapter.updateMusicList(songs)
            
            // Kiểm tra xem có bài hát nào đã được chọn trước đó không
            val previouslySelectedSong = MusicManagerApp.getSelectedSong()
            if (previouslySelectedSong != null) {
                val index = songs.indexOfFirst { it.id == previouslySelectedSong.id }
                if (index != -1) {
                    musicAdapter.setSelectedPosition(index)
                    selectedSong = previouslySelectedSong
                    Log.d("SelectMusicActivity", "Restored previously selected song: ${previouslySelectedSong.title}")
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            // Handle loading state if needed
            if (isLoading) {
                dialogLoading.show()
            } else {
                dialogLoading.dismiss()
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                // Handle error state if needed
            }
        }
    }

    /**
     * Load bài hát đã chọn trước đó
     */
    private fun loadPreviouslySelectedSong() {
        val savedSong = MusicManagerApp.getSelectedSong()
        if (savedSong != null) {
            selectedSong = savedSong
            Log.d("SelectMusicActivity", "Loaded previously selected song: ${savedSong.title}")
        }
    }

    /**
     * Kiểm tra xem có bài hát nào đã được chọn chưa
     */
    fun hasSelectedSong(): Boolean {
        return selectedSong != null || MusicManagerApp.getSelectedSong() != null
    }

    /**
     * Lấy bài hát đã chọn
     */
    fun getSelectedSong(): Song? {
        return selectedSong ?: MusicManagerApp.getSelectedSong()
    }

    /**
     * Xóa bài hát đã chọn
     */
    fun clearSelectedSong() {
        selectedSong = null
        MusicManagerApp.clearSelectedSong()
        Log.d("SelectMusicActivity", "Cleared selected song")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up media player resources
        if (::musicAdapter.isInitialized) {
            musicAdapter.releaseMediaPlayer()
        }

        if (dialogLoading.isShowing){
            dialogLoading.dismiss()
        }
        // Không release MusicManager ở đây vì nó là singleton
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Clean up before going back
        if (::musicAdapter.isInitialized) {
            musicAdapter.releaseMediaPlayer()
        }
    }
}