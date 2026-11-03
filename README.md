# My Workout

自分専用のオフライン筋トレ記録アプリ。現在は実装計画の第1段階（初期カタログ）です。

## 開発環境

既存の単一 `app` モジュールを維持。パッケージは設計に合わせて `com.myworkout`。

- JDK 21 / Gradle 9.3.1 / Android Gradle Plugin 9.1.1
- Kotlin 2.2.10（AGP内蔵）、Compose Compiler 2.2.10、KSP 2.3.9
- compileSdk 36.1 / targetSdk 36 / minSdk 36（既存設定を維持）
- Compose BOM 2024.10.01 / Material 3 / Activity 1.10.1 / Lifecycle 2.9.1
- Room 2.8.4 / JUnit 4.13.2 / Robolectric 4.16.1

依存関係は `gradle/libs.versions.toml` に固定。Coreは既存の1.19.0がAPI 37を要求するため、API 36と互換の1.17.0を使用。

## 実行と検証

Android Studioで同期後、API 36以上の端末で `app` を実行。
初回ビルドにはSDK・依存関係のダウンロードが必要ですが、アプリの利用には通信不要です。

```sh
./gradlew testDebugUnitTest --tests '*SeedCatalogTest'
./gradlew testDebugUnitTest assembleDebug lintDebug
./gradlew connectedDebugAndroidTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## 第1段階の内容

- 起動時にRoomへA/B各5種目、共有するラテラルレイズを含む9マシンを登録。
- 初期登録はトランザクション内で空のカタログにのみ実行し、再起動時は既存の編集内容を保持。
- メニュー・順番・目標セット数・回数をDBのFlowからCompose画面に表示。
- 最初の2週間は各2セットから始める案内を表示。保存する標準目標は設計書の3/2セット。
- 写真は未提供のため参照先はnull。トレーニング記録、筋肉図、写真、バックアップ等は第2段階以降。
- 自動バックアップは無効。明示的な手動バックアップは第7段階で実装予定。

## テスト方針

`SeedCatalogTest` はRobolectric上の実Roomで、設計書どおりの順序・目標値、重複初期化防止、ファイルDBの再オープンと編集保持を検証。

## 第1段階の検証結果

2026-11-03: 単体テスト3件、機内モードのAPI 36.1エミュレーターでAndroidテスト2件成功。
`assembleDebug` / `assembleDebugAndroidTest` / `lintDebug` 成功。
Lintには依存関係の更新通知・既存リソース等の警告が残ります（エラーなし）。
詳細は `docs/work-log/stage-1.md`。
