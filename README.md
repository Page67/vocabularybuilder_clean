# Vocabulary Builder Clean

An offline-first Android vocabulary-learning shell built with Kotlin and Jetpack Compose.

This public repository deliberately contains no textbook text, translation, scan, source map,
extraction output, review record, or production APK. The bundled one-unit dataset is newly written
demonstration content and exists only to make the application runnable after cloning.

## What is included

- A reusable Android learning interface with a bookshelf, bilingual reveal controls, quizzes,
  local practice, sentence drafting, Android system TTS, and device-local progress.
- A small original JSON dataset under `content/units/`.
- The Gradle Wrapper and pinned Android dependencies required to build the project.

## Build

Requirements: JDK 17 and an Android SDK with the versions declared in `gradle/libs.versions.toml`.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
```

On macOS or Linux, use `./gradlew` instead of `.\gradlew.bat`.

## Bring your own content

`content/catalog.json` lists packaged units. Each `payload_path` points to a JSON file under
`content/units/`. Keep stable IDs when updating content so local progress remains associated with
the same unit and word.

Only add material that you created, that is in the public domain, or that you have permission to
copy and distribute. Do not commit source books or intermediate extraction/review files.

## Privacy

The app requests no Internet permission, has no account system, analytics, advertising, or cloud
sync, and disables Android backup. Text-to-speech uses the Android system service.

## License

The source code and original demonstration data in this repository are available under the MIT
License. Third-party Android and Gradle dependencies retain their own licenses.
