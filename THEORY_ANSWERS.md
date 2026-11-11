# Seminar 10 Side effects theory answers

These answers cover all 19 questions in Part A. The model traces follow the rules stated in the handout and were checked with a small independent simulation. The complete lecture slides and runnable lecture model programs were not supplied.

## Question 1 Side effects and recomposition

A side effect changes something outside the composable's own UI description, such as sending a request, writing a log, or modifying an external object.

A composable can run again whenever observed state or relevant inputs change. Animations can cause updates every frame. Compose can skip functions, cancel an unfinished composition attempt when newer state arrives, and try again. Calling `loadProfile()` or logging directly in the body can therefore repeat work or perform it for an attempt whose UI is discarded.

An effect is a composable API that emits no UI and arranges for work to happen in coordination with a successful composition. Profile loading and business state belong in the ViewModel and repository. The UI displays state and sends events to the ViewModel, preserving the flow from events to state to UI. A composition effect can notify the ViewModel that a particular screen or user became active.

[Thinking in Compose](https://developer.android.com/develop/ui/compose/mental-model)

## Question 2 Choosing an API

| Need | API |
| --- | --- |
| Call a suspend function while the call site is in the Composition | `LaunchedEffect` |
| Launch a coroutine from an event callback | `rememberCoroutineScope` |
| Use the latest value without restarting an effect | `rememberUpdatedState` |
| Register a listener and remove it later | `DisposableEffect` |
| Publish Compose state to an external object | `SideEffect` |
| Adapt an external source into Compose state | `produceState` |
| Update a consumer less often than its inputs change | `derivedStateOf` |
| Convert reads of Compose state into a Flow | `snapshotFlow` |

For the sixth row, `produceState` is the intended answer among the eight APIs. Existing adapters include `collectAsStateWithLifecycle()` for Flow on Android, `observeAsState()` for LiveData, and `subscribeAsState()` for RxJava.

[State adapters](https://developer.android.com/develop/ui/compose/state)

## Question 3 Pulse animation

1. The effect launches its coroutine when its call enters the Composition successfully.
2. Changing the key from `3000L` to `1000L` cancels the previous coroutine and launches a new one. The new loop starts with a 1000 ms delay. The remembered `Animatable` survives this restart.
3. Leaving the Composition cancels the coroutine. `isActive` checks whether its coroutine context is still active. The suspending `delay` and animation calls also cooperate with cancellation, so a cancelled loop stops.

The lecture model's `effect(100) cancelled` message represents cancellation of the old job when its key changes or its call site leaves.

[Composition lifecycle](https://developer.android.com/develop/ui/compose/lifecycle)

## Question 4 Constant keys

A constant key ties work to one entry of that call site. It needs careful consideration because changing inputs will not restart the job. This is appropriate for a splash timer that should keep its original deadline while using the latest callback.

Sending a greeting depends on the current user, so the user ID should be the key.

```kotlin
LaunchedEffect(userId) {
    api.sendHello(userId)
}
```

The job restarts for a different user ID. Returning to the screen after it has left the Composition creates a new entry and can send another greeting.

## Question 5 Snackbar from a click

`onClick` is a regular callback. `LaunchedEffect` requires a composable context. `rememberCoroutineScope()` returns a `CoroutineScope` associated with its call site, and leaving that site cancels the scope's jobs.

```kotlin
@Composable
fun SnackbarScreen() {
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(host) }) { padding ->
        Button(
            modifier = Modifier.padding(padding),
            onClick = {
                scope.launch {
                    host.showSnackbar("Hi")
                }
            },
        ) {
            Text("Show snackbar")
        }
    }
}
```

`showSnackbar` suspends, so it needs a coroutine or another suspending context. Use `LaunchedEffect` for work triggered by composition entry or a key change. Use the remembered scope for work triggered by a click.

[Composition coroutine scopes](https://developer.android.com/develop/ui/compose/side-effects#remembercoroutinescope)

## Question 6 Keeping a callback current

The running coroutine can retain the lambda from the composition that created it. Later recompositions can supply new lambdas while that coroutine continues waiting.

`rememberUpdatedState(onTimeout)` keeps a stable state holder whose value follows the latest callback. Reading it when the delay finishes invokes the current callback. Making `onTimeout` a key would cancel and restart the wait whenever that lambda changes.

The model's `captured -> onTimeout v1` and `updatedState -> onTimeout v3` results show that the captured version remains old while the state holder exposes the newest version.

[Keeping values current inside effects](https://developer.android.com/develop/ui/compose/side-effects#rememberupdatedstate)

## Question 7 Listener cleanup

The supplied snippet fails to compile because the `DisposableEffect` block must end with `onDispose`. If a listener is registered through other code without cleanup, an owner that outlives the screen can retain its observer and captured references. Repeated registration can also produce duplicate callbacks.

```kotlin
DisposableEffect(lifecycleOwner) {
    val lifecycle = lifecycleOwner.lifecycle
    val observer = LifecycleEventObserver { _, event ->
        Log.d("LifecycleLogger", event.name)
    }

    lifecycle.addObserver(observer)

    onDispose {
        lifecycle.removeObserver(observer)
    }
}
```

An empty cleanup block leaves an acquired resource unmanaged. When the operation needs no resource cleanup, consider `SideEffect` for publishing a value or `LaunchedEffect` for coroutine work.

[Disposable effects](https://developer.android.com/develop/ui/compose/side-effects#disposableeffect)

## Question 8 Disposable model output

```text
A.addObserver(observer)
A.removeObserver(observer)
B.addObserver(observer)
B.removeObserver(observer)
```

The second `screen("A")` retains the same key, so it neither registers nor removes an observer. Switching to B first disposes A's registration, then registers B's observer. `comp.leave()` removes B's observer. An independent simulation of the supplied rules produced these four lines.

## Question 9 Publishing analytics state

`SideEffect` runs after a successful composition that executes its call site. Its block has no keys.

| Attempt | Is analytics called? | Reason |
| --- | --- | --- |
| Direct call with `commits=false` | Yes | The call already happened while evaluating the body |
| `SideEffect` with `commits=false` | No | The abandoned attempt's scheduled effect is discarded |
| `SideEffect` with `commits=true` | Yes | The effect runs after the successful commit |

```kotlin
SideEffect {
    analytics.setUserType(userType)
}
```

## Question 10 Loading an image with produceState

`produceState` returns a Compose `State` and starts a producer coroutine that can assign its value. Entry starts the producer, changed keys cancel and replace it, and leaving cancels it.

Equal assignments are conflated. For example, assigning the same `Success` value twice does not cause another state-change invalidation. A model can show two assignment attempts while the observed UI-update count increases only once for that result.

A URL change does not automatically restore the original initial value. Set `value = Loading` at the beginning of the producer when each request should show Loading.

The name `loadNetworkImage` uses lower camel case because it returns a value. Compose functions returning a non-`Unit` value follow ordinary Kotlin function naming.

[Compose API naming guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md#naming-composable-functions-that-return-values)

## Question 11 A simplified produceState implementation

```kotlin
@Composable
fun <T> myProduceState(
    initialValue: T,
    key: Any?,
    producer: suspend MutableState<T>.() -> Unit,
): State<T> {
    val state = remember { mutableStateOf(initialValue) }

    LaunchedEffect(key) {
        producer(state)
    }

    return state
}
```

`remember` preserves the state holder, and `LaunchedEffect` owns the producer job. A changed key cancels the old job and starts a new producer using that same holder. The previous value remains until the new producer assigns a value. This implements the requested simplified behavior. The full `produceState` API also supports features such as `awaitDispose`.

## Question 12 Scroll button recompositions

A direct read of the index makes the surrounding composition depend on each index change. A remembered `derivedStateOf` exposes the threshold result, allowing its consumer to skip updates while that Boolean stays the same. `remember` preserves the derived state object across recompositions.

For the supplied sequence, the consecutive distinct values are as follows.

```text
Index input       0, 0, 1, 2, 3, 4, 5, 3, 1, 0
Direct values     0,    1, 2, 3, 4, 5, 3, 1, 0
Threshold values  false, true, false
```

The handout's model counts the initial execution. The direct version has 1 initial execution plus 8 changes, giving 9. The derived version has 1 initial execution plus 2 Boolean changes, giving 3. Counting only executions after the initial composition gives 8 and 2. Actual Compose execution counts can also depend on batching and other invalidations.

[Derived state and performance](https://developer.android.com/develop/ui/compose/performance/bestpractices#use-derivedstateof-to-limit-recompositions)

## Question 13 Combining names

```kotlin
val fullName = "$firstName $lastName"
```

The full name normally changes whenever either input changes, so wrapping this simple expression in `derivedStateOf` adds tracking overhead without suppressing useful UI updates. The criterion is whether inputs change more often than the result needs to change. Creating a value from other values alone does not justify the API.

## Question 14 Voice waveform

Use `derivedStateOf` when observable amplitude data changes frequently but the displayed result changes less often, such as when many amplitude changes produce the same quantized bar height.

| Situation | Reason it may be unnecessary |
| --- | --- |
| Amplitudes are precomputed | The display data is already available. Rendering or caching a calculation from fixed inputs is sufficient |
| The waveform is static | There is no rapid stream of state changes to reduce |
| An effect or animation already updates exactly the display value at the required rate | The UI needs each update, so another state derivation offers no reduction |

Using `LaunchedEffect` or an animation API alone does not decide this. If its output still changes more often than a particular consumer needs, deriving a less frequently changing result can remain useful.

## Question 15 Scroll analytics Flow

`snapshotFlow` observes snapshot state read in its block. Collection starts the observation and emits the initial result. Later changes cause it to reevaluate, and it emits unequal results. It is cold because observation begins when a collector collects it.

`map` converts each index to `index > 0`. `distinctUntilChanged` removes repeated Boolean values. `filter` keeps only true values. The suspending `collect` runs inside the coroutine supplied by `LaunchedEffect`, whose cancellation also ends collection.

```text
Input              0, 0, 1, 2, 5, 3, 0, 0, 1, 4
snapshotFlow       0,    1, 2, 5, 3, 0,    1, 4
map                F,    T, T, T, T, F,    T, T
distinctUntilChanged
                   F, T, F, T
filter(true)       T, T
```

Exactly 2 events are sent, one on each transition from item 0 to a positive index. The calculation assumes collection observes each listed transition, as the lecture model does.

[Compose state as a Flow](https://developer.android.com/develop/ui/compose/side-effects#snapshotflow), [Kotlin Flow operators and collection](https://kotlinlang.org/docs/coroutines-flow.html)

## Question 16 Effect keys

Assume the resource depends on `owner` and `tick` only causes recomposition.

| Model keys | Initial starts plus restarts | Restarts after the initial start | Explanation |
| --- | --- | --- | --- |
| `[]` | 1 start | 0 | Keeps using A after the owner becomes B |
| `[owner]` | 2 starts | 1, at tick 3 | Replaces the job when its owner changes |
| `[owner, tick]` | 4 starts | 3, at ticks 2, 3, and 4 | Replaces the job on every tick |

The first variant has a stale owner. The second matches the dependency. The third causes unnecessary restarts under this assumption. If the work also genuinely depends on the tick, then including it is appropriate.

These counts were checked with an independent simulation of the four ticks. Empty keys are notation from the lecture model. Real `LaunchedEffect` requires a key, so a constant-key example uses `LaunchedEffect(Unit)`.

## Question 17 Lifecycle owner and callbacks

`lifecycleOwner` must be the `DisposableEffect` key because changing it requires removing the observer from the previous owner and registering with the new one.

The callbacks should be accessed through `rememberUpdatedState`. Their stable state holders supply the latest callbacks without requiring observer replacement. Therefore, `currentOnStart` and `currentOnStop` should not trigger restarts.

If the owner changes without being a key, the observer stays attached to the old lifecycle and misses events from the new owner.

The rule is to key an effect on values whose changes should cancel and recreate its work. Use `rememberUpdatedState` for changing values that ongoing work should read without restarting.

## Question 18 Four bugs in ChatScreen

1. `markOpened(userId)` runs directly in the composable body and can repeat on recomposition. Move the notification into an effect keyed by `userId`, while keeping business work in the ViewModel.
2. The timeout coroutine captures `onClose`, so it can call an old callback. Read the callback through `rememberUpdatedState`.
3. The lifecycle observer registration lacks `onDispose`. Remove the same observer from the same owner during cleanup.
4. `LaunchedEffect` is called inside `onClick`, which is a regular callback. Launch through the remembered coroutine scope.

This correction keeps the handout's existing `viewModel` and suspending `send()` function. The close timer runs once per screen entry and calls the latest callback.

```kotlin
@Composable
fun ChatScreen(userId: String, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    val latestOnClose by rememberUpdatedState(onClose)

    LaunchedEffect(userId) {
        viewModel.markOpened(userId)
    }

    LaunchedEffect(Unit) {
        delay(5_000)
        latestOnClose()
    }

    DisposableEffect(owner) {
        val lifecycle = owner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            Log.d("ChatScreen", event.name)
        }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    Button(onClick = {
        scope.launch { send() }
    }) {
        Text("Send")
    }
}
```

The snippets use standard Compose runtime, Material 3, layout, lifecycle, and coroutine imports. `LocalLifecycleOwner` comes from `androidx.lifecycle.compose`.

## Question 19 Summary and example uses

The example uses below can be adapted to a personal project.

| API | When it runs or updates | Cleanup | Restart or update trigger | Example |
| --- | --- | --- | --- | --- |
| `LaunchedEffect` | Launches on entry | Cancels its coroutine on exit | Changed keys | Run a splash timer |
| `rememberCoroutineScope` | Returns a remembered scope. Callbacks launch jobs | Cancels its jobs on exit | Jobs start through `launch` | Show a snackbar after a click |
| `rememberUpdatedState` | Refreshes the remembered holder's value during recomposition | No explicit cleanup | A new supplied value | Use the latest timeout callback |
| `DisposableEffect` | Registers work on entry | Runs `onDispose` on key change or exit | Changed keys | Register a lifecycle observer |
| `SideEffect` | Runs after a successful composition executing its call site | No cleanup callback | Another successful execution of that call site | Publish an analytics user type |
| `produceState` | Starts a producer on entry and exposes its state | Cancels the producer. Subscriptions can use `awaitDispose` | Changed keys | Load an image result for a URL |
| `derivedStateOf` | Calculates and tracks a derived state value | Managed state dependencies | Dependency changes can invalidate the calculation | Show a scroll button after item 0 |
| `snapshotFlow` | Observes state while collected and emits changed results | Stops observation when collection ends | Observed state changes. A new collection starts observation again | Track crossings of a scroll threshold |

`rememberUpdatedState` and `derivedStateOf` do not start coroutine jobs. `snapshotFlow` has no effect keys of its own. Its collection lifetime depends on its collector.

[Compose runtime API reference](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary)
