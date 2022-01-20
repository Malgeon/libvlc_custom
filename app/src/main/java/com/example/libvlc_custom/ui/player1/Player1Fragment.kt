package com.example.libvlc_custom.ui.player1

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.libvlc_custom.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Player1Fragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_player1, container, false)
    }
}