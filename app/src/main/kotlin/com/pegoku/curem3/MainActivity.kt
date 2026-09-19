package com.pegoku.curem3

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && savedInstanceState == null &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshIfStale()
    }
}
