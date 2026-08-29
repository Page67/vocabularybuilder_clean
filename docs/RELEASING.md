# Release guide · 发布指南

## 中文

APK 通过 GitHub Releases 分发；普通 push/PR 生成的 debug APK 仅是 14 天有效的 Actions
构建产物。正式 Release 必须使用稳定的独立签名，以便用户后续覆盖升级。
公开版安装包 ID 固定为 `com.page67.vocabularybuilder.clean`，不得改回私用版 ID。

在仓库 **Settings → Secrets and variables → Actions** 中配置：

- `ANDROID_KEYSTORE_BASE64`：keystore 文件的 Base64 内容。
- `ANDROID_SIGNING_STORE_PASSWORD`：keystore 密码。
- `ANDROID_SIGNING_KEY_ALIAS`：密钥别名。
- `ANDROID_SIGNING_KEY_PASSWORD`：密钥密码。

密钥原文件及密码必须在仓库外安全备份。不要提交 keystore、密码或 Base64 内容。

确认 `main` 的 Android CI 通过后，创建并推送与应用版本一致的注释 tag：

```powershell
git tag -a v1.0.0 -m "Vocabulary Builder Clean 1.0.0"
git push origin v1.0.0
```

tag 会触发 Release workflow：测试、Lint、构建正式签名 APK、验证签名、生成 SHA-256，
然后创建 GitHub Release 并附加两个文件。若任一签名 Secret 缺失，流程会失败关闭，
不会退回 debug 签名。

## English

APKs are distributed through GitHub Releases. Debug APKs produced for ordinary pushes and pull
requests are temporary Actions artifacts retained for 14 days. A Release requires a stable,
dedicated signing key so later versions can upgrade an existing installation.
The public application ID is fixed as `com.page67.vocabularybuilder.clean` and must not be changed
back to the private build's ID.

Configure the four repository secrets listed above under
**Settings → Secrets and variables → Actions**. Back up the original keystore and passwords
outside the repository. Never commit the keystore, passwords, or Base64 value.

After Android CI passes on `main`, create and push an annotated tag matching the application
version. The Release workflow tests and lints the project, builds and verifies the signed APK,
writes its SHA-256 checksum, and attaches both files to a GitHub Release. Missing signing secrets
cause a closed failure; the workflow never falls back to debug signing.
