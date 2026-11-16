package dev.tunderlucky

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import dev.tunderlucky.ui.AssistantViewModel
import dev.tunderlucky.ui.Root
import dev.tunderlucky.ui.TunderTheme

class MainActivity : ComponentActivity() {
    private val vm: AssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { TunderTheme { Root(vm) } }
    }

    override fun onResume() { super.onResume(); vm.refresh() }
}
