# Session sharing between BYD apps

YouTube BYD and YT Music BYD are separate packages with separate GeckoView
profiles, so each one keeps its own cookie jar and Google sign-in. Signing in
twice is the default browser behaviour; this project restores the session from
the sibling app instead.

## How it works

```text
App A (signed in)                     App B (signed out)
  onStop                                onCreate
    extension export  ──►  bundle.json    loadUri (immediately)
                                          provider call  ──►  bundle.json
                                          extension import
                                          optional late reload
```

1. `browser-core` ships a built-in WebExtension in
   `src/main/assets/web_extensions/session-bridge/`. It is installed with
   `WebExtensionController.ensureBuiltIn` and is the only way to reach the
   cookie jar: GeckoView 160 no longer exposes cookies through
   `StorageController`.
2. When an app goes to the background it exports the jar through the
   extension. The bundle is written atomically to
   `files/session/session-bundle.json`.
3. `SessionProvider` serves that file to sibling apps over
   `content://<applicationId>.sessionsync`. The provider is protected by
   `<applicationId>.permission.SESSION_SYNC`, a `signature` permission, so only
   apps signed with the same certificate can read it. It never starts Gecko:
   a cross-app read is a plain file read.
4. When an app starts **signed out**, it asks every package in its
   `partnerPackages` for a bundle, picks the newest signed-in one and restores
   the cookies. The page is already loading in parallel; if the restore lands
   after the start page has begun, the app reloads once, and only while the
   start page is still showing and media is not playing. A cached last-known
   sign-in state skips the status query for signed-in profiles.

Rules that keep this safe:

- **Import only when signed out.** Importing over an account that is already
  signed in would silently switch accounts.
- **Export only when signed in.** A signed-out app never overwrites a good
  bundle.
- **Only cookies.** `localStorage` and `IndexedDB` are not copied; Google
  authentication is cookie-based, so sign-in works without them.

## Adding a third app

1. Add the new package to `partnerPackages` in the existing apps.
2. In the new app's manifest, declare a `<uses-permission>` for every sibling
   permission and a `<queries><package …/></queries>` entry for every sibling
   package:

   ```xml
   <uses-permission android:name="dev.theolm.bydapps.youtube.permission.SESSION_SYNC" />

   <queries>
       <package android:name="dev.theolm.bydapps.youtube" />
   </queries>
   ```

   The `<queries>` entry is required on API 30+ to reach a provider in another
   package.

The provider, permission and extension come from `browser-core`, so no other
code is needed.

## Testing without a Google account

The extension does not care whether the cookies are real. Write a synthetic
bundle into one app and check that the other restores it:

```bash
cat > /tmp/session-bundle.json <<'JSON'
{
  "version": 1,
  "createdAt": 1763400000000,
  "signedIn": true,
  "cookies": [
    {
      "name": "SID",
      "value": "synthetic-sid",
      "domain": ".google.com",
      "path": "/",
      "secure": true,
      "httpOnly": true,
      "sameSite": "no_restriction",
      "expirationDate": 1863400000
    },
    {
      "name": "HSID",
      "value": "synthetic-hsid",
      "domain": ".google.com",
      "path": "/",
      "secure": true,
      "httpOnly": true,
      "sameSite": "no_restriction",
      "expirationDate": 1863400000
    }
  ]
}
JSON

adb push /tmp/session-bundle.json /data/local/tmp/session-bundle.json
adb shell "run-as dev.theolm.bydapps.youtube sh -c \
  'mkdir -p files/session && cp /data/local/tmp/session-bundle.json files/session/session-bundle.json'"

adb shell am force-stop dev.theolm.bydapps.ytmusic
adb logcat -c
adb shell am start -n dev.theolm.bydapps.ytmusic/.MainActivity
adb logcat -d | grep SessionSync
```

Expected output:

```text
D SessionSync: Local session signedIn=false
D SessionSync: Restored 2 cookies (0 failed, null), hash=…
```

A synthetic account needs either `SID` + `HSID` or one of the modern
`__Secure-1PSID` / `__Secure-3PSID` cookies. A lone `SID` is a YouTube visitor
cookie, not proof of a Google account, so it is ignored.

Sending the app to the background then logs
`Export bundle signedIn=true, cookies=N` and `Exported N cookies`, and the
bundle appears in the other app's `files/session/`. The export is one cookie
jar scan; it replaced the old status + export pair.

The cookies can be inspected without root:

```bash
adb exec-out run-as dev.theolm.bydapps.ytmusic \
  cat files/mozilla/*/cookies.sqlite > /tmp/cookies.sqlite
sqlite3 /tmp/cookies.sqlite "select name, host, isSecure, isHttpOnly, sameSite from moz_cookies"
```

Note that Gecko keeps session cookies in memory only. A force-stop drops them;
Google's auth cookies (`SID`, `__Secure-*PSID`) are persistent, so a real
session survives.

## Verifying on the head unit

1. Install both APKs signed with the same key.
2. Sign in to Google in YouTube BYD.
3. Press Home (this triggers the export) and open YT Music BYD.
4. YT Music should load signed in; its log shows `Restored N cookies`.

Check the log with:

```bash
adb logcat -s SessionSync SessionBridge
```

## Limitations

- Google may invalidate a copied session, although the same device, IP and
  user agent make it look like the same client.
- Apps must be signed with the same certificate. A debug build next to a
  release build cannot read the other's bundle.
- The bundle holds bearer credentials. It stays in app-private storage and is
  never written to the SD card or to logs.
- A future "vault" APK could hold the bundle as well; the bundle format and
  provider contract are already designed for it.
