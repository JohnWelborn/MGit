# Android Release Signing Setup

Allows future APK installs to upgrade without uninstalling (preserving user data).
Without this, each CI build uses a fresh debug keystore and Android blocks upgrades.

---

## What you do once (locally)

### 1. Generate a keystore

```bash
keytool -genkey -v -keystore mgit-release.jks \
  -alias mgit \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Answer the prompts. Save the passwords somewhere safe — if you lose them you cannot sign future updates and users will have to uninstall.

### 2. Encode the keystore for GitHub

```bash
base64 -w 0 mgit-release.jks
```

Copy the output.

### 3. Add secrets to the GitHub repo

Go to **Settings → Secrets and variables → Actions → New repository secret** and add:

| Secret name | Value |
|-------------|-------|
| `KEYSTORE_BASE64` | base64 output from step 2 |
| `KEY_ALIAS` | `mgit` (or whatever alias you used) |
| `KEY_PASSWORD` | the password you chose |

Keep `mgit-release.jks` somewhere safe offline — it is the only copy of your signing identity.

---

## What Claude does (workflow changes)

Prompt Claude with something like:

> "Set up Android release signing in the GitHub Actions release workflow using secrets
> KEYSTORE_BASE64, KEY_ALIAS, and KEY_PASSWORD. Decode the keystore in CI, pass it to
> Gradle via -Pspecial -Palias -Ppassword -Pkeystore, switch assembleDebug to
> assembleRelease, and update the artifact path to the release output directory."

Claude will:
1. Add a step to decode `KEYSTORE_BASE64` → a `.jks` file in `$RUNNER_TEMP`
2. Pass `-Pspecial`, `-Palias`, `-Ppassword`, `-Pkeystore` to `./gradlew assembleRelease`
3. Update the release artifact path from `apk/debug/` to `apk/release/`
4. Verify `app/build.gradle` has the matching `signingConfigs` block (uses `project.hasProperty('special')`)

---

## How it works

`app/build.gradle` contains:

```groovy
signingConfigs {
    if (project.hasProperty('special')) {
        release {
            keyAlias alias
            keyPassword password
            storeFile file(keystore)
            storePassword password
        }
    } else {
        release { /* empty fallback */ }
    }
}
buildTypes {
    release {
        signingConfig signingConfigs.release
    }
}
```

When `-Pspecial` is passed, Gradle picks up the real key. The same APK signature is used every build, so Android treats each new version as an upgrade rather than a different app.
