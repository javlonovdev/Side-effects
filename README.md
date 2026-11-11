# SideEffectsLab

Six hands-on Jetpack Compose demos from Seminar 10. Each task opens from the app's task menu. Use **Back to tasks** to leave a demo and trigger its cleanup.

## Run the project

Open this folder in Android Studio and let Gradle sync. Use JDK 17 or 21, Android SDK Platform 35, and an emulator or device on API 23 or later. Run the `app` configuration. From the terminal, the equivalent build command is `.\gradlew.bat :app:assembleDebug` on Windows or `./gradlew :app:assembleDebug` on macOS and Linux.

In Android Studio Logcat, select the running app and filter by the tag listed below. From a terminal, use:

```text
adb logcat -v brief -s LabSplash:D LabSnackbar:D LabLifecycle:D LabAnalytics:D LabScroll:D LabProduce:D
```

## Task 1 Splash screen

**Effect choice.** `LaunchedEffect(Unit)` runs a single two-second timer per entry. The left timer captures the original callback, which closes over version 0. The right timer calls through `rememberUpdatedState` so it uses the latest callback when the timer fires.

**Check in `LabSplash`.** Open the screen and wait two seconds. The left result should be v0. The right result should be the latest composed version at the deadline, usually v3 or v4 because a 500 ms tick and the timer can coincide. Enable **Use callback as the left timer key**. Each new lambda cancels the pending delay, so the left timer finishes about two seconds after the final v6 tick. The version counter stops at six to make the eventual completion observable.

## Task 2 Snackbar button

**Effect choice.** `rememberCoroutineScope` supplies a coroutine scope for the regular `onClick` callback to call the suspending `showSnackbar` function. The scope cancels pending work on leaving the screen.

**Check in `LabSnackbar`.** Tap twice quickly. The second request waits and appears after the first message finishes. Tap again, return to the menu while the message is visible, and reopen the screen. The message is cancelled on exit and the new screen has an empty host.

## Task 3 Lifecycle logger

**Effect choice.** `DisposableEffect(lifecycleOwner)` registers a single observer for the current owner. Its `onDispose` block removes that same observer when the logger leaves or the owner changes.

**Check in `LabLifecycle`.** Toggle the switch five times starting from off, then return to the menu. There should be three adds and three removals. At toggle five, the third observer is still attached and is removed on exit. To observe both `ON_STOP` and `ON_START`, background and foreground the app while the observer is visible. Temporarily remove the `onDispose { ... }` block from `LifecycleTask.kt` and rebuild. The body must return a `DisposableEffectResult`, which `onDispose` supplies. Restore the block after the check.

## Task 4 SideEffect and analytics

**Effect choice.** `SideEffect` publishes the composed user type to the external `Analytics` object after a successful commit. It runs each time this composable executes and commits, even when the user type is identical. In the lecture's attempt model, the scheduled publish is discarded with an abandoned attempt and runs after a committed attempt.

**Check in `LabAnalytics`.** Tap **Recompose with the same user type** twice, then **Change user type**. The count should increase on every tap. The type stays at guest for the first two taps and becomes member on the third.

## Task 5 Scroll and analytics

**Effect choice.** Two controls observe the same 100-item list. The direct control reads the index, while the `derivedStateOf` control reads a Boolean that changes only at the threshold. Both use `SideEffect` to count committed executions. The on-screen counts exclude the initial commit. `snapshotFlow` observes the index from a `LaunchedEffect` coroutine. The pipeline maps the index to a Boolean, removes repeated Boolean values, skips the initial baseline, and passes only transitions to true.

**Check in `LabScroll`.** Scroll slowly across several item boundaries, return to item 0, and scroll down again. While scrolling between nonzero indexes, only the direct count should rise. The two departures from item 0 should produce exactly two events.

## Task 6 Bonus image state

**Effect choice.** `produceState` runs the fake repository request and exposes its result as Compose state. The URL key restarts the producer and the producer explicitly resets its value to Loading. The fake repository returns after 500 ms. Equal result assignments are conflated by the returned state.

**Check in `LabProduce`.** Open the screen, wait for Success, and tap **Switch URL** to see Loading followed by Error. The producer logs an identical assignment 500 ms after each result, with no corresponding increase in the image UI commit count. Switching URLs or leaving the screen logs the producer's `finally` line.

## Build verification

The debug APK assembled successfully using Android SDK 35. The verification build used the SDK's AAPT2 executable because the local Gradle cache lacked the packaged version.

The temporary Task 3 build without `onDispose` failed with the compiler diagnostic below. The final project includes the cleanup.

```text
Argument type mismatch: actual type is 'Unit', but 'DisposableEffectResult' was expected.
```

[Android Developers documentation on Compose side effects](https://developer.android.com/develop/ui/compose/side-effects)

