# Sleep Roulette

Personal Android MVP for irregular / shift sleep schedules. **You are the only user.** Local-only data. No accounts, no cloud, no iOS/web.

Target device: **Oppo Reno 11F (CPH2603), Android 16 / ColorOS**.

**Product docs:** [docs/PRODUCT.md](docs/PRODUCT.md) — origin story, V1 goals, principles, idea backlog, and roadmap. (The old ChatGPT dump `TXT.txt` is archive only.)

## Product promise

Help you get to bed more consistently by reacting to *life events*:

- Arrived home (geofence)
- Sleep countdown until goal bedtime / sunrise
- Phone still in use after goal → nudge (notification only — does not lock the phone)

## Architecture (learn this)

```
ui/          Compose screens + ViewModels (state holders)
domain/      Pure Kotlin: models, policies, sunrise math, repository interfaces
data/        Room + DataStore implementations
location/    Geofencing (Play Services)
usage/       UsageStats sampling + WorkManager nudges
notify/      Notification channels + countdown foreground service
di/          Hilt modules
```

**Rules of thumb**

1. **Domain has zero Android imports** — so you can unit test sunrise + bedtime policy with plain JUnit.
2. **UI talks to repositories / controllers via ViewModels** — not to Room DAOs directly.
3. **Events are append-only** (`life_events`) — treat them like an observability log.
4. **ColorOS kills background work** — countdown uses a foreground service; Setup pushes battery exemption + Autostart.

```mermaid
flowchart TB
  UI[Compose_UI] --> VM[ViewModels]
  VM --> Repo[Repository_interfaces]
  Repo --> Room[(Room_SQLite)]
  Repo --> DS[(DataStore)]
  Geo[GeofenceReceiver] --> Events[LifeEventRepository]
  Geo --> Countdown[SleepCountdownController]
  Countdown --> FGS[ForegroundService]
  Countdown --> Work[UsageNudgeWorker]
  Work --> Usage[UsageStatsSampler]
  Work --> Notif[NotificationHelper]
```

## Stack

| Piece | Choice |
|-------|--------|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose + Material 3 (BOM 2026.06) |
| Build | AGP 8.13 + Gradle 8.13 |
| Navigation | Type-safe `@Serializable` routes |
| DI | Hilt 2.57 |
| DB | Room 2.8 (+ Room Gradle plugin schemas) |
| Prefs | DataStore |
| Async | Coroutines + Flow + `@ApplicationScope` |
| Location | Play Services Geofencing |
| Jobs | WorkManager + `@HiltWorker` |

> **Note:** AGP 9 + Hilt 2.60 exist, but KSP is not ready for AGP 9’s built-in Kotlin yet.
> We stay on the latest AGP 8.x line until that lands — still current, and what most production apps use today.

## Modern practices used (and why)

- **Type-safe Navigation** — no string route typos; `composable<AppDestination.Today>`
- **`LifecycleEventEffect`** — replaces `DisposableEffect` + `LifecycleEventObserver` boilerplate
- **`collectAsStateWithLifecycle`** — UI collects only while started
- **Room `fallbackToDestructiveMigration(dropAllTables = true)`** — Room 2.7+ API
- **Room Gradle plugin `schemaDirectory`** — replaces legacy KSP `room.schemaLocation`
- **`kotlin { compilerOptions { jvmTarget } }`** — replaces deprecated `android.kotlinOptions`
- **UsageStats `ACTIVITY_RESUMED/PAUSED` only** — `MOVE_TO_*` events are deprecated
- **`ServiceCompat.stopForeground`** — support-library-safe FGS teardown
- **Injected `@ApplicationScope`** — no orphan `CoroutineScope(SupervisorJob())` in receivers
- **Theme.Material3.DayNight.NoActionBar** — correct base for Compose edge-to-edge

## Build & run

```bash
cd sleep-roulette
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open **Setup** first:

1. Fine location → Background location  
2. Usage access  
3. Ignore battery optimizations  
4. ColorOS Autostart (App settings)  
5. **Use current location as Home** (or **Clear Home** / overwrite when needed)  
6. Set bedtime goal  

From **Today**, tap **I'm home** to manually start the countdown while testing geofences.

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

Domain tests cover sunrise math and bedtime nudge policy — the parts that are easy to get wrong and easy to verify without a device.

## Repo

- Local folder: `sleep-roulette`
- Remote: https://github.com/Shahzaibalikhawaja/sleep-roulette
