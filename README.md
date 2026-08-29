# Vocabulary Builder Clean · 纯净词汇学习工具

[中文](#中文) · [English](#english)

<p align="center">
  <img src="docs/images/overview.png" width="30%" alt="应用概览 / App overview">
  <img src="docs/images/course.png" width="30%" alt="双语课程 / Bilingual course">
  <img src="docs/images/quiz.png" width="30%" alt="固定测验 / Fixed quiz">
</p>

## 中文

这是一个使用 Kotlin 和 Jetpack Compose 构建的离线优先 Android 词汇学习框架。

本公开仓库不包含任何教材正文、翻译、扫描件、来源映射、提取结果、审核记录、生产
APK 或其他中间文件。仓库自带的单单元数据全部为原创演示内容，只用于确保克隆后
项目能够直接运行。完整示范单元包含 5 个课程阶段、15 个双语词条、5 个阶段测验和
1 个综合复习测验，可体验应用的主要学习流程。

### 功能

- 书架、主题课程、按需显示中文、固定测验和本地动态练习。
- 三句写作练习、Android 系统文字转语音和设备本地学习进度。
- 数据驱动的目录与单元 JSON，方便替换成你有权使用的内容。
- 无账号、无分析、无广告、无云同步，且不申请网络权限。

### 构建

需要 JDK 17，以及与 `gradle/libs.versions.toml` 中声明版本相符的 Android SDK。

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
```

macOS 或 Linux 请使用 `./gradlew`。

### 使用自己的内容

`content/catalog.json` 是内容目录，每个 `payload_path` 指向 `content/units/` 下的一个
单元 JSON。更新内容时请保持稳定 ID，这样本地学习进度仍能对应原来的单元和词条。

只应添加由你原创、属于公有领域，或你已取得复制和分发许可的材料。请勿提交来源
书籍或提取、转换、审核过程中产生的中间文件。

### 许可证

本仓库源码及原创演示数据采用 [MIT License](LICENSE)。第三方 Android 与 Gradle 依赖
仍适用各自的许可证。

## English

An offline-first Android vocabulary-learning shell built with Kotlin and Jetpack Compose.

This public repository contains no textbook text, translation, scan, source map, extraction
output, review record, production APK, or other intermediate artifact. Its bundled single-unit
dataset is newly written demonstration content included only to keep the project runnable after
cloning. The complete demo unit includes five lesson stages, 15 bilingual entries, five stage
quizzes, and one cumulative review quiz, covering the application's main learning flow.

### Features

- Bookshelf navigation, topic-based lessons, on-demand Chinese reveal, fixed quizzes, and local
  dynamic practice.
- A three-sentence writing workshop, Android system text-to-speech, and device-local progress.
- A data-driven catalog and unit JSON format that can be replaced with content you may use.
- No accounts, analytics, advertising, cloud sync, or Internet permission.

### Build

The project requires JDK 17 and an Android SDK matching the versions declared in
`gradle/libs.versions.toml`.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
```

Use `./gradlew` on macOS or Linux.

### Bring your own content

`content/catalog.json` is the packaged content catalog. Each `payload_path` points to a unit JSON
file under `content/units/`. Preserve stable IDs when updating content so local progress remains
associated with the same units and words.

Only add material that you created, that is in the public domain, or that you have permission to
copy and distribute. Do not commit source books or intermediate extraction, conversion, or review
artifacts.

### License

The source code and original demonstration data are available under the [MIT License](LICENSE).
Third-party Android and Gradle dependencies retain their own licenses.
