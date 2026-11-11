# Paps Update SDK

A tiny library any app distributed through Paps App Store can embed to
force updates: on launch, it asks the Paps server whether a newer version
exists, and if so shows a blocking "Update required" dialog that sends the
person to install it.

## What it does

- Reads your app's own package name and version code.
- Asks `https://paps.probloxworld.com/api/version-check/<package>?versionCode=<N>`.
- If the server says a newer `versionCode` is published, shows a dialog:
  - **By default**, it can't be dismissed - the only button is "Update,"
    which opens the update page and then closes your activity.
  - Pass `allowDismiss = true` if you'd rather nag than block (adds a
    "Later" button, dialog is cancelable).
- If Paps can't be reached at all (offline, server down), it does
  **nothing** - it never blocks someone from using an app they already
  have just because a network check failed.

## Integrating it

This SDK isn't published to Maven Central or JitPack (there's nowhere set
up to host it yet) - for now, add it as a local module:

1. Copy the `paps-update-sdk` folder into your project, next to your `app`
   module.
2. In your project's `settings.gradle.kts`, add:
   ```kotlin
   include(":paps-update-sdk")
   ```
3. In your app module's `build.gradle.kts`:
   ```kotlin
   dependencies {
       implementation(project(":paps-update-sdk"))
   }
   ```
4. In your main activity:
   ```kotlin
   import com.paps.updatesdk.PapsUpdateChecker

   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       PapsUpdateChecker.checkOnLaunch(this)
   }
   ```

That's it - no API key, no registration. It just needs your app's package
name to already be published on Paps App Store with a `versionCode` higher
than what someone currently has installed for the dialog to ever appear.

## Options

```kotlin
PapsUpdateChecker.checkOnLaunch(
    activity = this,
    serverBaseUrl = "https://paps.probloxworld.com/", // only change if self-hosting elsewhere
    allowDismiss = false // true = "Later" button instead of forcing the update
)
```

## Why this can't fully "force" anything

This is a client-side check your own app calls voluntarily - it's a good
UX pattern for steering people to update, not a security boundary. Someone
could still use a build of your app that never calls it, or block network
access to bypass it. If you need to hard-enforce a minimum version (e.g.
because old clients break against a new backend), that check has to live
server-side, in whatever backend your app itself talks to - this SDK can't
do that part for you
ransdsdjfnsfhdonfdsofsdofuosdfuosdhfsdufhousdfhudshousdhusdfhousdfh
