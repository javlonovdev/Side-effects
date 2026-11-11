package com.example.sideeffectslab

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val TAG = "LabSplash"
private const val WAIT_MS = 2_000L

@Composable
fun SplashTask() {
    var useCallbackKey by remember { mutableStateOf(false) }
    var run by remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(checked = useCallbackKey, onCheckedChange = { useCallbackKey = it })
            Text("Use callback as the left timer key")
        }
        Button(onClick = { run++ }) { Text("Run again") }
        key(run, useCallbackKey) { SplashComparison(useCallbackKey) }
    }
}

@Composable
private fun SplashComparison(useCallbackKey: Boolean) {
    var version by remember { mutableIntStateOf(0) }
    var left by remember { mutableStateOf("Waiting…") }
    var right by remember { mutableStateOf("Waiting…") }

    LaunchedEffect(Unit) {
        repeat(6) {
            delay(500)
            version++
        }
    }

    // Capture an immutable snapshot so each tick supplies a callback with a different version.
    val callbackVersion = version
    val onCapturedTimeout: () -> Unit = {
        Log.d(TAG, "captured callback v$callbackVersion (keyed=$useCallbackKey)")
        left = "Called v$callbackVersion"
    }
    val onUpdatedTimeout: () -> Unit = {
        Log.d(TAG, "latest callback v$callbackVersion")
        right = "Called v$callbackVersion"
    }

    Text("Parent version v$callbackVersion")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(if (useCallbackKey) "Callback as key" else "Captured callback")
            CapturedTimer(onCapturedTimeout, useCallbackKey, callbackVersion)
            Text(left, Modifier.padding(top = 4.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Latest callback")
            LandingScreen(onUpdatedTimeout)
            Text(right, Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun CapturedTimer(onTimeout: () -> Unit, useCallbackKey: Boolean, version: Int) {
    if (useCallbackKey) {
        // A new lambda changes the key, cancelling the old delay and starting two seconds again.
        LaunchedEffect(onTimeout) {
            Log.d(TAG, "keyed timer started for v$version")
            delay(WAIT_MS)
            onTimeout()
        }
    } else {
        LaunchedEffect(Unit) {
            delay(WAIT_MS)
            onTimeout()
        }
    }
}

@Composable
fun LandingScreen(onTimeout: () -> Unit) {
    val latestOnTimeout by rememberUpdatedState(onTimeout)
    LaunchedEffect(Unit) {
        delay(WAIT_MS)
        latestOnTimeout()
    }
}
