package com.example.sideeffectslab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { LabApp() } }
    }
}

private class Lab(val title: String, val screen: @Composable () -> Unit)

private val labs = listOf<Lab>(
    Lab("1. Splash screen") { SplashTask() },
    Lab("2. Snackbar button") { SnackbarTask() },
    Lab("3. Lifecycle logger") { LifecycleTask() },
)

@Composable
private fun LabApp() {
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    BackHandler(enabled = selected >= 0) { selected = -1 }

    Surface(Modifier.fillMaxSize()) {
        Scaffold { insets ->
            Column(
                modifier = Modifier.fillMaxSize().padding(insets).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (selected == -1) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("SideEffectsLab", style = MaterialTheme.typography.headlineMedium)
                        Text("Jetpack Compose effects practice")
                        labs.forEachIndexed { index, lab ->
                            Button(
                                onClick = { selected = index },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(lab.title)
                            }
                        }
                    }
                } else {
                    TextButton(onClick = { selected = -1 }) { Text("Back to tasks") }
                    Text(labs[selected].title, style = MaterialTheme.typography.headlineSmall)
                    Box(Modifier.weight(1f)) { labs[selected].screen() }
                }
            }
        }
    }
}

