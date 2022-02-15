package com.example.libvlc_custom.ui.player1

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {

    private val _isFullScreen = MutableLiveData<Boolean>()
    val isFullScreen: LiveData<Boolean>
        get() = _isFullScreen

    fun setFullscreenState(setValue: Boolean) {
        if (_isFullScreen.value != setValue) {
            _isFullScreen.value = setValue
        }
    }
}