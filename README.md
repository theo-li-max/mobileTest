# Accenture Booking Demo

Booking segment data viewer built with **Kotlin** and **MVVM** architecture.

## Tech Stack

| Component | Library |
|-----------|---------|
| Language | Kotlin 1.9.20 |
| UI | ViewBinding + Material Components |
| Architecture | MVVM (Model-View-ViewModel) |
| Async | Kotlin Coroutines |
| Serialization | Gson |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 |

## Architecture

```
View (MainActivity) ──observes──> ViewModel ──delegates──> DataManager ──reads──> Service/Cache
```

- **Model** — `BookingResponse`, `Segment`, `Location` data classes + `DataState` sealed class
- **ViewModel** — `BookingViewModel` exposes LiveData to the View
- **Data** — `BookingDataManager` singleton with cache-first strategy and pagination
- **View** — `MainActivity` with RecyclerView + SwipeRefreshLayout

## Features

- Pull-to-refresh reloads booking data
- Scroll-to-bottom triggers pagination (up to 3 pages)
- Cache-first strategy: show cached data immediately, then refresh in background
- Mock data generation for pagination demo

## Build

```bash
./gradlew assembleDebug
```

Open in Android Studio, sync Gradle, and run on an API 26+ device/emulator.

## Coding Conventions

See [CLAUDE.md](./CLAUDE.md) for the full style guide, inspired by [kodecocodes/swift-style-guide](https://github.com/kodecocodes/swift-style-guide).
