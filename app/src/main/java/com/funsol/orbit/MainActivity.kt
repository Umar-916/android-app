package com.funsol.orbit

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.funsol.orbit.core.datastore.AppSettings
import com.funsol.orbit.core.datastore.SettingsStore
import com.funsol.orbit.core.designsystem.OrbitTheme
import com.funsol.orbit.core.navigation.OrbitApp
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
    private val settingsStore: SettingsStore by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsStore.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            OrbitTheme(settings.themeMode) { OrbitApp() }
        }
    }
}
