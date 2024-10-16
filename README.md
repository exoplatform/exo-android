# eXo Mobile Android application

Crowdin i18n jobs statues: 

[![Sync Crowdin with eXo Mobile Project](https://github.com/exoplatform/exo-android/actions/workflows/crowdin_sync.yml/badge.svg)](https://github.com/exoplatform/exo-android/actions/workflows/crowdin_sync.yml)
[![Merge Crowdin translation to eXo Mobile Project](https://github.com/exoplatform/exo-android/actions/workflows/crowdin_merge.yml/badge.svg)](https://github.com/exoplatform/exo-android/actions/workflows/crowdin_merge.yml)

Minimum compatibility :

- Android SDK : 14.0 (Pie)
- Fastlane : 2.135.2

## how-to test

TODO: add doc for local testing

## how-to build

TODO: add doc for build

```bash
gradle clean assembleDebug
```

## how-to publish beta on private store

To build and publish a beta version on Appaloosa private store :

prerequisites:

- `APPALOOSA_EXO_STORE_ID` environment variable with the target store id
- `APPALOOSA_EXO_API_TOKEN` environment variable with a valid Appaloosa api token (needed for binary upload)

Increment the label used to build the badge in `./app/build.gradle`:

```diff
-        versionCode 33
+        versionCode 34
-        versionName "5.1.x-SNAPSHOT"
+        versionName "5.1.0-RC13"
```

(optional) Update the label used to build the badge in `./fastlane/Fastfile`:

```diff
add_badge(
-     shield: "5.1-M04-blue", 
+    shield: "5.1-RC13-blue", 
    dark: true,
```

Launch the following command :

```bash
fastlane beta
```

## how-to build a feature branch

TODO: add doc for feature branch configuration

## how-to release

TODO: add a doc to build and publish an official public version
