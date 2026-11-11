# 起動アイコンの重複と旧カール試作画面の起動を修正

2026-11-10

原因はdebug用ManifestのCurlPrototypeActivityにもMAIN/LAUNCHERが登録されていたこと。同じcom.myworkoutパッケージ内に通常画面と試作画面の2つのランチャーがあり、実機でも2つの登録を確認した。

- debug用試作Activityからintent-filterを削除し、exported=falseに変更。明示的なAndroidテストからの起動は維持。
- LauncherTestを追加。修正前に2つの起動エントリーで失敗することを確認し、修正後はMainActivityのみになることとgetLaunchIntentForPackageの起動先を検証。
- エミュレーターでLauncherTest、CurlAnimationTest、CurlRigRenderingTestの計4件成功。assembleDebug成功。
- Galaxy SM-F966Zへadb install -rで更新。アンインストール・データ消去は行っていない。実機のランチャー登録がMainActivityの1件だけであることを確認し、同Activityの正常起動（Status: ok）を確認。
- DB・記録機能・画像には変更なし。
