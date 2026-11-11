package com.example.sideeffectslab

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TAG = "LabScroll"

@Composable
fun ScrollTask() {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val directCount = remember { mutableIntStateOf(0) }
    val derivedCount = remember { mutableIntStateOf(0) }
    val scrollToTop: () -> Unit = { scope.launch { listState.animateScrollToItem(0) } }

    LaunchedEffect(listState) {
        var events = 0
        snapshotFlow { listState.firstVisibleItemIndex }
            .map { it > 0 }
            .distinctUntilChanged()
            .drop(1) // The first emission is the baseline, even if a saved list starts below item 0.
            .filter { it }
            .collect { Log.d(TAG, "scrolledPastFirstItem event=${++events}") }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Scroll past item 0, continue down, then return to the top and repeat.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Direct read")
                CommitCount(directCount)
                ScrollTopControl(listState, false, directCount, scrollToTop)
            }
            Column(Modifier.weight(1f)) {
                Text("Derived state")
                CommitCount(derivedCount)
                ScrollTopControl(listState, true, derivedCount, scrollToTop)
            }
        }
        HorizontalDivider()
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            items(100) { index ->
                Text("Item $index", Modifier.fillMaxWidth().padding(vertical = 18.dp))
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ScrollTopControl(
    listState: LazyListState,
    derived: Boolean,
    commits: MutableIntState,
    onScrollToTop: () -> Unit,
) {
    val showButton = if (derived) {
        remember(listState) {
            derivedStateOf(structuralEqualityPolicy()) { listState.firstVisibleItemIndex > 0 }
        }.value
    } else {
        listState.firstVisibleItemIndex > 0
    }
    val label = if (derived) "derived" else "direct"
    SideEffect { Log.d(TAG, "$label commits=${++commits.intValue} showButton=$showButton") }

    if (showButton) {
        Button(onClick = onScrollToTop) { Text("Scroll to top") }
    }
}

@Composable
private fun CommitCount(commits: MutableIntState) {
    // This sibling reads the count so updating it does not recompose the measured control.
    Text("Recompositions ${(commits.intValue - 1).coerceAtLeast(0)}")
}
