package com.example.sideeffectslab

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

object Analytics {
    var userType: String = "guest"
}

@Composable
fun AnalyticsTask() {
    var userType by remember { mutableStateOf("guest") }
    var refreshes by remember { mutableIntStateOf(0) }
    val runs = remember { mutableIntStateOf(0) }
    val currentType = userType

    // The attempt model drops SideEffect for discarded attempts and runs it after a commit.
    SideEffect {
        Analytics.userType = currentType
        Log.d("LabAnalytics", "SideEffect run=${++runs.intValue} userType=${Analytics.userType}")
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("User type $currentType")
        Text("Manual refreshes $refreshes")
        Button(onClick = { userType = if (userType == "guest") "member" else "guest" }) {
            Text("Change user type")
        }
        Button(onClick = { refreshes++ }) { Text("Recompose with the same user type") }
    }
}
