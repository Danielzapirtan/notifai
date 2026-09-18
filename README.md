# Notifai

Notifai is a small native Android app for scheduling recurring activities and receiving phone notifications at selected times and days.

## MVP features

- Create, edit, enable/disable, and delete activities.
- Choose a 24-hour time and one or more weekdays.
- Persist schedules locally on the phone.
- Deliver notifications while the app is not open, including after reboot.
- Reschedule after time or timezone changes.

## Development

The project requires JDK 17, the Android SDK (API 35), and a Gradle 8.7+ installation (or Android Studio):

```sh
./gradlew assembleDebug
./gradlew installDebug
./gradlew check
```

On the first run, Android may ask for notification permission (Android 13+) and exact-alarm access (Android 12+). Exact alarms are used when permitted; otherwise the app falls back to an inexact idle-capable alarm.

## Usage

Open the app, tap **Add activity**, enter a title and message, choose a time and weekdays, then save. Tap an existing activity to edit it. The switch controls whether future notifications are active.
