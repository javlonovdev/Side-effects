package com.example.sideeffectslab

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val TAG = "LabSnackbar"

@Composable
fun SnackbarTask() {
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val taps = remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(host) },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Tap twice quickly to queue a second message. Leave while a message is visible, then return.")
            Button(onClick = {
                val tap = ++taps.intValue
                // onClick is a regular callback, so it cannot call the composable LaunchedEffect.
                scope.launch {
                    Log.d(TAG, "requested message $tap")
                    try {
                        // showSnackbar suspends. A second caller waits until the first message ends.
                        val result = host.showSnackbar("Message $tap", duration = SnackbarDuration.Short)
                        Log.d(TAG, "message $tap finished: $result")
                    } catch (cancelled: CancellationException) {
                        Log.d(TAG, "message $tap cancelled")
                        throw cancelled
                    }
                }
            }) { Text("Show snackbar") }
        }
    }
}
