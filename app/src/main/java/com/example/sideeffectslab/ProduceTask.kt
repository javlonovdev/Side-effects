package com.example.sideeffectslab

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay

private const val TAG = "LabProduce"

sealed interface Result {
    data object Loading : Result
    data class Success(val url: String) : Result
    data class Error(val message: String) : Result
}

object FakeImageRepository {
    suspend fun load(url: String): Result {
        delay(500)
        return if (url.endsWith(".png")) Result.Success(url) else Result.Error("Expected a .png URL")
    }
}

@Composable
fun loadNetworkImage(url: String): State<Result> =
    produceState<Result>(initialValue = Result.Loading, key1 = url) {
        try {
            value = Result.Loading
            Log.d(TAG, "loading $url")
            val result = FakeImageRepository.load(url)
            value = result
            Log.d(TAG, "assigned $result")

            delay(500)
            value = result
            Log.d(TAG, "assigned identical result again: $result")
            // Keep this demo producer alive after loading to observe cancellation on exit.
            awaitCancellation()
        } finally {
            Log.d(TAG, "finally: producer ended for $url")
        }
    }

@Composable
fun ProduceTask() {
    var png by remember { mutableStateOf(true) }
    val url = if (png) "https://example.test/photo.png" else "https://example.test/photo.jpg"
    val image = loadNetworkImage(url)
    val commits = remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(url)
        Button(onClick = { png = !png }) { Text("Switch URL") }
        ImageResult(image, commits)
        ImageCommitCount(commits)
    }
}

@Composable
private fun ImageResult(image: State<Result>, commits: MutableIntState) {
    val result = image.value
    SideEffect { Log.d(TAG, "image UI commit=${++commits.intValue} result=$result") }

    when (result) {
        Result.Loading -> {
            CircularProgressIndicator()
            Text("Loading")
        }
        is Result.Success -> Text("Success • ${result.url.substringAfterLast('/')}")
        is Result.Error -> Text("Error • ${result.message}")
    }
}

@Composable
private fun ImageCommitCount(commits: MutableIntState) {
    Text("Image UI commits ${commits.intValue}")
}

