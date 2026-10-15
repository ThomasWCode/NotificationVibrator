# Notification Vibrator

An Android app (built for the Galaxy S24, works on Android 13+) that vibrates with a **different pattern for each app** that sends you a notification. For example, WhatsApp can be one long buzz, Gmail and Outlook two buzzes, and GitHub one short buzz.

## Features

- **101 built-in patterns** in 7 groups: single, double, triple and more, rhythms (heartbeat, knock-knock, SOS…), shapes that fade or swell in strength, soft versions, and Morse A–Z / 0–9.
- **No vibrate** option that you can assign to any app.
- **Default pattern** for every app you haven't assigned. It can also be No vibrate.
- **Per-app strength** slider (30–100%).
- **Record your own rhythm** by holding a pad on screen (up to 5 s).
- **Morse generator**: type `W` for WhatsApp or `GH` for GitHub, choose a speed and save it.
- **Respects Silent mode and Do Not Disturb.** In Silent mode nothing vibrates. With DND on, only apps and people that DND allows through will buzz.
- **One buzz per burst**: a cooldown (1.5 s by default, adjustable) means five quick WhatsApp messages buzz once.
- **Activity tab** lists the last 200 decisions, e.g. *played Long* or *skipped: Phone in Silent mode*, so you can see why something did or didn't buzz.
- No internet permission: the app never sends anything anywhere itself. If Android/Google backup is switched on, the app's data (assignments, custom patterns, settings and the activity log) is included in your Google backup, so it restores on a new phone.

## Install on your phone

1. On the phone, open **https://github.com/ThomasWCode/NotificationVibrator/releases/tag/latest**. Each push to the repository rebuilds this release.
2. Tap **NotificationVibrator.apk** to download it, then open it. If asked, allow your browser to **Install unknown apps**.
3. If Play Protect warns that the app is unrecognised, choose **Install anyway**. The warning appears because the app isn't from the Play Store.
4. Updates install over the old version and keep your settings, because every build is signed with the same key (`app/debug.keystore`).

## Set up (follow the Setup tab)

### 1. Notification access

Tap **Turn on access** and switch on *Notification Vibrator patterns*.

If the switch is greyed out or says **Restricted setting**: Android 13+ blocks this for sideloaded apps.

1. Open **App info** (there's a button for it in the app).
2. Tap **⋮** (top right), then **Allow restricted settings**.
3. Try again.

### 2. Keep it running

Samsung stops background apps to save battery.

1. Tap **Allow unrestricted** in the app.
2. Then go to Settings → Battery → Background usage limits → **Never sleeping apps** (called *Never auto sleeping apps* on some versions) → **+** → Notification Vibrator.

### 3. Silence the other apps' own vibration

Android doesn't let one app block another app's vibration, so you turn off the system's notification vibration and this app takes over.

> **Do not use Silent mode for this.** In Silent mode this app deliberately stays silent too. Keep the phone in **Vibrate** (or Sound) mode.

**Global mute (recommended, one setting):**

1. Settings → Sounds and vibration → Vibration intensity → drag **Notifications** to 0.
2. In the app, run **Test 1**. A test notification appears, and the phone should not buzz.
3. Run **Test 2**. You should feel two buzzes.

This works because the app plays its patterns as *alarm-type* vibrations, which have their own intensity setting.

It hasn't been confirmed that every One UI version lets the slider reach 0, or that alarm-type vibrations survive it — that's what the two tests check. If Test 2 doesn't buzz, use Per-app mute instead and put the Notifications slider back up.

**Per-app mute (fallback):**

1. For each app: Settings → Notifications → App notifications → *app* → Notification categories → each category → turn vibration off. The Setup tab has an **Open** button for every app it has seen.
2. If a category has no vibration switch, set it to **Silent**, then turn on **Vibrate for Silent categories** in the app. Silent categories also lose their pop-up banners.

### 4. Assign patterns

1. On the **Apps** tab, tap the **Default pattern** card to choose what unassigned apps do.
2. Tap any app to choose its pattern. Tapping a pattern lets you feel it.
3. Press **Save**. **Reset to default** removes the app's own assignment.
4. Apps that have recently sent notifications are listed near the top.

## What doesn't vibrate (by design)

- Anything while the phone is in **Silent mode**, and anything **Do Not Disturb** blocks. With DND on, apps and people that DND allows through still buzz. One UI reports the phone as Silent whenever DND is on, so while DND is on the app follows DND's rules rather than the sound mode.
- **Incoming calls.** Your ringtone vibration handles those.
- **Ongoing notifications** (music, navigation, downloads) and **updates** to a notification already showing, when the app marks them as "alert once".
- **Silent notification categories.** These are categories the app or you set to Silent; Per-app mute has an option to include them.
- A second notification from the same app within the cooldown time.

## Limitations

- A Galaxy Watch keeps its own vibration settings.
- The Notifications intensity slider in One UI doesn't affect this app in Global mute mode. Use each app's strength slider instead.
- Patterns that differ only in strength (the Soft and Shape groups) are harder to tell apart than patterns with different rhythms.

## Build from source

**Requirements:**

- JDK 17 or newer, e.g. `sudo apt install openjdk-17-jdk`, or the JDK bundled with Android Studio.
- Android SDK with platform 35, e.g. Android Studio → SDK Manager, or:
  ```sh
  sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
  ```
- Create `local.properties` containing `sdk.dir=/path/to/android-sdk`, or set `ANDROID_HOME`.

Gradle downloads every library itself; there's nothing else to install.

```sh
./gradlew assembleDebug          # APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit tests + Robolectric UI smoke tests
./gradlew lintDebug              # Android lint
adb install -r app/build/outputs/apk/debug/app-debug.apk   # install over USB (USB debugging on)
```

**Quick device test:** assign a pattern to the app called **Shell**, then post a notification from a computer:

```sh
adb shell cmd notification post -t "Test" test_tag "Hello"
```

The phone should play Shell's pattern, and the Activity tab should show *Played*.

## Project layout

```
app/src/main/java/io/github/thomaswcode/notificationvibrator/
  vibration/  pattern model, built-in library, Morse encoder, tap recorder, player
  service/    notification listener and AlertFilter (play/skip rules)
  data/       Room database, DataStore settings, repositories
  ui/         Compose screens: apps, patterns (+ recorder, Morse), activity, setup
```
