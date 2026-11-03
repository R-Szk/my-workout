# My Workout — Androidアプリ実装計画書

> **実装担当者向け**：この計画は各タスクを小さく区切り、テストを先に書いてから実装する。タスクごとに動作確認とレビューを行い、完了したらコミットする。実装時は `superpowers:executing-plans` または `superpowers:subagent-driven-development` の手順を利用する。

**目的：** FIT365相模原での筋トレを簡単に記録し、重量・回数・身体写真の変化を実感できる、自分専用のAndroidアプリを作る。

**構成：** Jetpack Composeによる単一Activityのアプリ。画面ごとにViewModelを設け、Roomに記録を保存する。写真はアプリ専用領域へ保存。バックアップは記録データと写真をまとめ、端末のファイル選択画面から書き出し・復元する。通常利用にログインも通信も不要。

**技術：** Kotlin、Jetpack Compose（Material 3）、Navigation Compose、Room、DataStore、Coroutines/Flow、Android Storage Access Framework、JUnit、AndroidのUIテスト。

**元の設計書：** `my-workout-design.md`（2026年11月3日承認済み）

## 全体に共通する条件

- Android専用・完全に自分用。アカウント、サーバー、クラウド同期は作らない。
- A/Bメニューを初期登録し、当日の順番変更・スキップ・種目追加を可能にする。
- 筋トレ中は1種目ずつ表示し、マシン写真と鍛える筋肉の図を常に見せる。
- 重量の単位はkg。重量と回数の両方を比較し、数値の向上を筋肥大の証拠と断定しない。
- セットの完了時点で保存し、アプリ中断後も再開できる。
- 身体写真は非公開のアプリ専用領域に保存し、バックアップに含める。
- 休憩タイマー、通知、SNS、AI体型診断は初期版の対象外。
- マシン写真は利用条件と機種の対応を確認するまで無断転用しない。自身で撮影した画像の登録を可能にする。
- 開発開始時に互換性のある安定版ツールチェーンを選定・固定し、READMEに記載する。

## 先に注意すべき5つの不具合

| リスク | 期待する動作 | 対応タスク |
|---|---|---|
| セット保存直後にアプリが終了 | 記録の消失・二重登録がない | 2 |
| 前回値を表示しただけで画面を閉じる | 今回の実績として保存しない | 2 |
| 種目の順番変更・スキップ | 過去のトレーニング履歴を書き換えない | 3 |
| マシン画像や筋肉図が欠ける | 名称・プレースホルダーで記録を続行できる | 4 |
| バックアップ破損・パスワード誤り | 既存データを壊さず復元を中止する | 7 |

## コードの配置方針

Androidパッケージを `com.myworkout` とする。`data/` にRoom・DAO・リポジトリ、`domain/` にトレーニングの状態変更、`feature/` に画面・ViewModel、`ui/components/` に画像・筋肉図、`backup/` に書き出し・復元処理を配置する。単体テストは `app/src/test/`、Android実機・エミュレーターのテストは `app/src/androidTest/` に配置する。

---

## 第1段階：Androidプロジェクトと初期マシン・メニュー

**作成ファイル：** `settings.gradle.kts`、`build.gradle.kts`、`gradle/libs.versions.toml`、`app/build.gradle.kts`、`AndroidManifest.xml`、`MainActivity.kt`、`data/AppDatabase.kt`、`data/Entities.kt`、`data/WorkoutDao.kt`、`data/SeedCatalog.kt`、`data/WorkoutRepository.kt`、`SeedCatalogTest.kt`、`README.md`。

**主要インターフェース：**

- `suspend fun WorkoutRepository.seedIfEmpty()`：初期データの重複登録を防ぐ。
- `fun WorkoutRepository.observeRoutine(id: Long): Flow<RoutineWithExercises?>`
- `fun WorkoutRepository.observeMachines(): Flow<List<Machine>>`

**初期データ：** A・B各5種目。種目の順番、目標回数とセット数は設計書の内容を使用する。

- [ ] まず `SeedCatalogTest` を書き、A/Bの各5種目、順番、目標値、2回初期化しても重複しないことを検証する。
- [ ] `./gradlew testDebugUnitTest --tests '*SeedCatalogTest'` を実行し、実装前に失敗することを確認する。
- [ ] Androidプロジェクト、Roomのテーブル、初期データ登録、最低限の起動画面を実装する。
- [ ] 同じ単体テストと `./gradlew assembleDebug` を実行し、成功を確認する。
- [ ] `feat: scaffold offline workout catalog` のコミットを作成する。

**完了条件：** アプリが起動し、A/Bメニューを端末内から読み込める。

## 第2段階：セット記録と中断・再開

**作成ファイル：** `data/WorkoutSessionDao.kt`、`domain/SessionController.kt`、`feature/workout/WorkoutViewModel.kt`、`feature/workout/WorkoutScreen.kt`、`SessionControllerTest.kt`、`WorkoutPersistenceTest.kt`。Roomの既存ファイルとMainActivityも更新する。

**主要インターフェース：**

- `start(routineId: Long): Long`：トレーニング開始。
- `completeSet(sessionExerciseId: Long, weightKg: Double, reps: Int): Long`：完了セットを永続化。
- `undoSet(setId: Long)`：記録の取り消し。
- `resumeActive(): ActiveWorkout?`：進行中のトレーニングを取得。
- `finish(sessionId: Long)`：トレーニング終了。

- [ ] 開始・保存・取り消し・終了・再起動後の再開・重複タップ防止を検証する単体テストを書く。「前回値の提示だけでは保存されない」ことも検証する。
- [ ] `./gradlew testDebugUnitTest --tests '*SessionControllerTest'` で実装前の失敗を確認する。
- [ ] Roomのトランザクションと1種目記録画面を実装する。重量は0以上、回数は1以上の整数で入力を検証する。
- [ ] 単体テスト、エミュレーターでの `./gradlew connectedDebugAndroidTest`、ビルドを実行する。
- [ ] `feat: persist workout sets and resume sessions` をコミットする。

**完了条件：** チェストプレスを記録後、アプリを終了・再起動しても続きから再開できる。

## 第3段階：当日の種目変更

**作成ファイル：** `domain/SessionExerciseEditor.kt`、`feature/workout/ExercisePicker.kt`、`feature/workout/ExerciseQueueSheet.kt`、`SessionExerciseEditorTest.kt`。記録画面とDAOも更新。

**主要インターフェース：**

- `reorder(sessionId: Long, orderedSessionExerciseIds: List<Long>)`
- `skip(sessionExerciseId: Long)`
- `add(sessionId: Long, machineId: Long): Long`

- [ ] 順番変更、スキップ、種目追加、過去履歴を維持する動作のテストを書く。
- [ ] `./gradlew testDebugUnitTest --tests '*SessionExerciseEditorTest'` で失敗を確認する。
- [ ] 一覧を一時表示する操作メニューと、データベースの順番・状態変更を実装する。
- [ ] 単体テストとUI操作テストを実行し、成功を確認する。
- [ ] `feat: customize exercises during a workout` をコミットする。

**完了条件：** 混雑時に種目順を変えたり、疲労時にスキップしたりできる。過去の記録は変わらない。

## 第4段階：実機写真と鍛える筋肉の可視化

**作成ファイル：** `ui/components/MachinePhoto.kt`、`ui/components/MuscleMap.kt`、`data/MuscleCatalog.kt`、筋肉図ベクターアセット、`MuscleCatalogTest.kt`、`MachineVisualTest.kt`。

**主要インターフェース：**

- `@Composable fun MachinePhoto(imagePath: String?, machineName: String, modifier: Modifier = Modifier)`
- `@Composable fun MuscleMap(machineId: Long, side: BodySide, modifier: Modifier = Modifier)`
- `fun MuscleCatalog.forMachine(machineId: Long): List<MuscleRole>`

- [ ] チェストプレスのメイン筋肉＝大胸筋、補助＝肩前部・上腕三頭筋、ラットプルダウンの背面表示、画像欠落時の代替表示をテストする。
- [ ] まずテスト失敗を確認する。
- [ ] 解剖学的に検証した前面・背面の筋肉図を作り、メインは赤・補助はオレンジで描画する。写真と筋肉図を記録画面に常時並べ、手動で正面・背面を切り替えられるようにする。
- [ ] 小画面での可読性、画像なしの場合、切替操作のテストを実行する。
- [ ] `feat: show machine photos and target muscles` をコミットする。

**完了条件：** マシン名を知らなくても写真で識別でき、使う筋肉を常に確認できる。

## 第5段階：成長グラフと完了画面

**作成ファイル：** `domain/ProgressCalculator.kt`、`feature/history/HistoryScreen.kt`、`feature/history/HistoryViewModel.kt`、`feature/summary/WorkoutSummaryScreen.kt`、`ProgressCalculatorTest.kt`。

**主要インターフェース：**

- `ProgressCalculator.dailyBest(sets: List<SetRecord>): List<PerformancePoint>`
- `ProgressCalculator.compareSameWeight(current: PerformancePoint, previous: PerformancePoint): RepetitionDelta?`

- [ ] 日付ごとの最大成功重量、同じ重量での回数増加、異なる重量の比較不可、スキップ種目の除外をテストする。
- [ ] 実装前のテスト失敗を確認する。
- [ ] 履歴画面、重量と回数の切替グラフ、完了サマリー、今日鍛えた筋肉、月間実施回数を実装する。
- [ ] 単体テストとサンプル記録の画面確認を行う。
- [ ] `feat: visualize workout history and progress` をコミットする。

**完了条件：** 重量だけでなく、同じ重量で回数が増えた成長も確認できる。

## 第6段階：身体写真とサイズの比較

**作成ファイル：** `data/ProgressPhotoRepository.kt`、`feature/body/BodyProgressScreen.kt`、`feature/body/PhotoCompareScreen.kt`、`feature/body/BodyProgressViewModel.kt`、`ProgressPhotoTest.kt`。

**主要インターフェース：**

- `savePhoto(uri: Uri, angle: PhotoAngle, takenAt: Instant): Long`
- `photosAt(date: LocalDate): List<ProgressPhoto>`
- `deletePhoto(id: Long)`

- [ ] 正面・側面・背面、任意の体重・胸囲・上腕囲、2時点比較、写真ファイル欠落、公開ギャラリーに意図せず複製しないことをテストする。
- [ ] 実装前のテスト失敗を確認する。
- [ ] Androidの写真選択機能、アプリ専用領域へのコピー、測定値の入力、写真の左右比較を実装する。
- [ ] エミュレーターでの写真保存・比較・削除テストを行う。
- [ ] `feat: compare private physique photos` をコミットする。

**完了条件：** 4週間前と現在の正面写真などを並べて比較できる。

## 第7段階：暗号化バックアップ・復元・最終確認

**作成ファイル：** `backup/BackupCodec.kt`、`backup/BackupService.kt`、`feature/settings/SettingsScreen.kt`、`BackupCodecTest.kt`、`BackupRestoreTest.kt`。

**主要インターフェース：**

- `BackupService.export(destination: Uri, passphrase: CharArray): ExportResult`
- `BackupService.preview(source: Uri, passphrase: CharArray): BackupPreview`
- `BackupService.restore(source: Uri, passphrase: CharArray, mode: RestoreMode): RestoreResult`

- [ ] 記録と写真の往復復元、誤パスワード、破損ファイル、形式の不一致、写真不足、復元失敗時に既存データが残ることをテストする。
- [ ] 実装前のテスト失敗を確認する。
- [ ] 暗号化方式と鍵導出方式を実装前に文書化し、バージョン付きバックアップ形式、ファイル選択画面、検証後の安全な復元を実装する。パスワードを忘れると復元できないことを明示する。
- [ ] 全単体テスト、Android UIテスト、リリースビルド、機内モードでの記録、強制終了からの再開、バックアップ転送、小画面での操作を確認する。
- [ ] `feat: safely back up and restore personal workout data` をコミットする。

**完了条件：** 別のAndroid端末環境へ記録と身体写真を復元でき、失敗時に既存データを壊さない。

---

## 開発時の運用ルール

1. まだ実装は開始していない。開始時に既存のAndroidプロジェクトがあるか確認し、ある場合は既存構成と未コミットの変更を尊重する。
2. 各段階を「失敗するテスト作成 → 最小限の実装 → テスト成功 → レビュー → コミット」で進める。
3. Roomのテーブル構造を後から変更する場合は、移行テストも追加する。
4. APKのビルドが通っただけで完成とは言わない。端末またはエミュレーターで記録・写真・復元を検証する。
5. 第2段階まで完成した時点で実際に記録を試し、操作の改善点を確認してから残りを進める。
