# My Workout

自分専用のオフライン筋トレ記録アプリ。実装計画の第2段階（セット記録・中断再開）まで対応しています。

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
- 写真は未提供のため参照先はnull。筋肉図、写真、バックアップ等は今後の段階で対応。
- 自動バックアップは無効。明示的な手動バックアップは第7段階で実装予定。

## テスト方針

`SeedCatalogTest` はRobolectric上の実Roomで、設計書どおりの順序・目標値、重複初期化防止、ファイルDBの再オープンと編集保持を検証。

## 第1段階の検証結果

2026-11-03: 単体テスト3件、機内モードのAPI 36.1エミュレーターでAndroidテスト2件成功。
`assembleDebug` / `assembleDebugAndroidTest` / `lintDebug` 成功。
Lintには依存関係の更新通知・既存リソース等の警告が残ります（エラーなし）。
詳細は `docs/work-log/stage-1.md`。

## 第2段階の内容

- A/Bの一覧から開始し、1種目ずつ重量（kg）と回数を入力してセットを即時保存。
- 保存中は操作を抑止し、同じ保存要求IDの再処理でも重複登録しない。同じ数値の次のセットは別途登録可能。
- 重量は0以上の有限数、回数は1以上の整数。訂正はセットの取り消し後に再入力。
- 前／次の種目へ移動、中断してホームに戻る、再開、確認後に終了。
- 再起動時は未完了セッションの再開を優先し、記録済みセットと表示位置を復元。
- 前回同種目の終了済みセッションの直近セットを提案。完了操作までは今回の実績として保存しない。
- 開始時のメニュー名・種目名・目標を記録にコピーし、テンプレートの後日編集から分離。
- Room DB v2へ自動移行。v1の既存カタログや編集内容を保持し、破壊的な再作成は使用しない。

第3段階以降（順番変更・スキップ・追加、写真・筋肉図、履歴・グラフ・詳細サマリーなど）は未着手。

`SessionControllerTest` は実Roomで開始・保存・取り消し・中断再開・終了、競合要求・入力検証・前回値を検証。
`WorkoutPersistenceTest` はAndroid上のファイルDBで再オープンとv1→v2移行を、
`WorkoutFlowTest` は入力検証から保存・中断・再生成・再開・取り消し・終了の画面操作を検証。

## 第2段階の検証結果

2026-11-03: 全単体テスト11件、機内モードのAPI 36.1エミュレーターでAndroidテスト5件成功。
`assembleDebug` / `assembleDebugAndroidTest` / `lintDebug` 成功（Lintエラー0件・警告27件）。
機内モードでチェストプレスを保存し、次の種目へ移動後にプロセスを強制終了。
再起動後に種目位置と保存済みセットが戻ることを確認。検証用セットは取り消し、アプリはインストール済みで保持。
詳細は `docs/work-log/stage-2.md`。
