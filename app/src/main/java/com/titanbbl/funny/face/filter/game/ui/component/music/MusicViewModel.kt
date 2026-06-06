package com.titanbbl.funny.face.filter.game.ui.component.music

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.titanbbl.funny.face.filter.game.model.api.Song
import com.titanbbl.funny.face.filter.game.data.repository.MusicRepository
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {
    
    private val musicRepository = MusicRepository.getInstance()
    
    private val _musicList = MutableLiveData<List<Song>>()
    val musicList: LiveData<List<Song>> = _musicList
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _selectedSong = MutableLiveData<Song?>()
    val selectedSong: LiveData<Song?> = _selectedSong
    
    init {
        loadMusic()
    }
    
    fun loadMusic() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val songs = musicRepository.getMusic()
                _musicList.value = songs
                
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load music"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectSong(song: Song) {
        _selectedSong.value = song
    }
    
    fun clearSelection() {
        _selectedSong.value = null
    }
    
    fun getSelectedSong(): Song? {
        return _selectedSong.value
    }
}
