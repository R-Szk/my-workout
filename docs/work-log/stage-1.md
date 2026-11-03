# 第1段階 作業記録

対象: docs/implementation-plan.md の第1段階のみ。
設計: docs/design.md（計画内の旧名 my-workout-design.md はこのファイルを指すと判断）。

- 既存構成: Activityなしの単一appモジュール、AGP 9.1.1、SDK 36.1。
- 未コミットの初期プロジェクトでHEADが存在しないため、作業ツリーを維持して実装。ユーザーがステージ済みの.idea/docsは変更しない。
- 既存Core 1.19.0でAPI要件エラーを確認。SDKを維持するためCore 1.17.0に固定。
- KSP旧版はAGP 9内蔵KotlinのsourceSetsと不整合。AGP 9対応のKSP 2.3.9を使用。
- Roomのテーブルは第1段階のMachine/Routine/RoutineExerciseに限定。後続の記録用テーブルは作成しない。
- 初期カタログを単一トランザクションで登録し、既存のユーザー編集を上書きしない。
- RED: KSP取得中にテスト側のKSPを一時的に外し、testDebugUnitTestでAppDatabase/WorkoutRepository未実装によるコンパイル失敗を確認。その後KSP設定を復元。

## 検証結果

- 単体テストの初回実行では、テストSDK 35 < minSdk 36によるパッケージ解析エラーを検出。@Configを36に修正。
- `./gradlew testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest`: BUILD SUCCESSFUL。単体テスト3件成功（SeedCatalogTest 2件、既存テスト1件）。
- `./gradlew connectedDebugAndroidTest`: BUILD SUCCESSFUL。API 36.1エミュレーター、機内モードで2件成功（A/Bの読み込みと切替、既存コンテキストテスト）。
- 機内モードでプロセスを終了・再起動し、Aの表示、Bの5種目への切替を手動確認。
- エミュレーターの容量不足はインストール中だけ低容量しきい値を1%にすることで回避。設定は元の未設定値に復元。既存アプリの保存データは保持。
- 通信設定も実行前の状態に復元。
- 別エージェントの静的レビュー: 第1段階内の具体的な正確性問題なし。
- Lintはエラーなし。警告は固定依存関係の更新案内、既存SDK設定、未使用の初期リソース、自動バックアップ規則等。第2段階以降は未着手。
