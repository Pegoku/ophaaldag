package com.pegoku.curem3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.pegoku.curem3.ui.AppViewModel
import com.pegoku.curem3.ui.CureApp

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels { AppViewModel.Factory(CureApplication.from(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { CureApp(viewModel) }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshIfStale()
    }
}
