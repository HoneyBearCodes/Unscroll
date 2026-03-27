package com.devsummit.scroll

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devsummit.scroll.service.OverlayService
import com.devsummit.scroll.ui.dashboard.DashboardScreen
import com.devsummit.scroll.ui.theme.UnscrollTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(intent)
        }
        
        val usageIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(usageIntent)

        setContent {
            UnscrollTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(
                        onTestOverlayClick = {
                            startService(Intent(this@MainActivity, OverlayService::class.java))
                        }
                    )
                }
            }
        }
    }
}
