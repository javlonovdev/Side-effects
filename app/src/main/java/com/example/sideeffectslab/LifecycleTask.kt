package com.example.sideeffectslab

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val TAG = "LabLifecycle"

private class ObserverCounts {
    var added = 0
    var removed = 0
}

@Composable
fun LifecycleTask() {
    var visible by remember { mutableStateOf(false) }
    val counts = remember { ObserverCounts() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(checked = visible, onCheckedChange = { visible = it })
            Text("Show lifecycle observer")
        }
        Text("Flip the switch five times, then return to the task menu to dispose the last observer.")
        if (visible) {
            LifecycleLogger(LocalLifecycleOwner.current, counts)
            Text("Observer attached")
        }
    }
}

@Composable
private fun LifecycleLogger(lifecycleOwner: LifecycleOwner, counts: ObserverCounts) {
    DisposableEffect(lifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_STOP) {
                Log.d(TAG, event.name)
            }
        }
        Log.d(TAG, "addObserver count=${++counts.added}")
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            Log.d(TAG, "removeObserver count=${++counts.removed}")
        }
    }
}
