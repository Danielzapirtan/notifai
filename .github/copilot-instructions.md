# Copilot instructions

## Build, test, and lint

This is a Kotlin Android application using Gradle and Android Gradle Plugin 8.5.2.

- Build the debug APK: `./gradlew assembleDebug`
- Install on a connected device: `./gradlew installDebug`
- Run all checks: `./gradlew check`
- Run one unit test class: `./gradlew testDebugUnitTest --tests "com.notifai.ExampleTest"`

The repository currently has no test classes or custom lint configuration. Android Studio can import the project and manage Gradle, Android SDK, and device deployment; the local environment used to create this scaffold did not include Gradle or an Android SDK.

## Architecture

- `MainActivity` provides the small MVP UI for listing, creating, editing, enabling/disabling, and deleting activities. The UI is built programmatically to keep the initial app dependency-free.
- `Schedule` is the persisted domain model. `ScheduleStore` serializes the schedule list as JSON in app-private `SharedPreferences`.
- `AlarmScheduler` calculates and registers the next matching day/time with `AlarmManager`. Each alarm is one-shot; `AlarmReceiver` emits the notification and schedules the next occurrence.
- `BootReceiver` restores enabled alarms after boot, time changes, or timezone changes.
- Notifications use an Android notification channel and open `MainActivity` when tapped. Android 13+ notification permission and Android 12+ exact-alarm access are requested by the activity.

## Conventions

## Repository-specific conventions

- Keep project guidance in `.github/copilot-instructions.md` and update it whenever the project structure or developer commands change.
- Keep the MVP dependency-free apart from the Android and Kotlin Gradle plugins; use platform APIs unless a dependency solves a clearly demonstrated need.
- Store schedules through `ScheduleStore` rather than introducing a second persistence path.
- Treat alarms as one-shot and always schedule the next occurrence after delivery or when a schedule changes.
- Make platform assumptions explicit: this app uses Android notification APIs and `AlarmManager`, not Termux or an external notification service.
- Do not add generated files, local environment files, credentials, or device-specific state to version control.
