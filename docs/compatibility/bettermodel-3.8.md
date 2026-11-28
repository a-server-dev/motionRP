# BetterModel 3.8 対応

対象: MotionRP 0.84.0 / Paper 26.3 build 253 / BetterModel Paper `3.8.0-SNAPSHOT-553`。
調査日: 2026-11-28（日本時間）。Modrinthで配布中の最新3.8系はこの開発ビルド。正式公開APIの最新版は3.7.1。

## 再調査と修正

0.83.0と3.8を組み合わせた際のフュージョン開始エラーを再現した。

```
NoClassDefFoundError: kr/toxicity/model/api/tracker/Tracker$CloseReason
  at dev.aserver.motionrp.MotionPlayback.play(MotionPlayback.java:130)
```

3.8では終了理由が `TrackerCloseReason` へ移動している。コールバックの実体は引き続き `BiConsumer` であるため、終了理由を `Object` として受け取り、JAR内に特定バージョンのenum参照を残さない呼び出しへ変更した。非公開フィールドの反射操作や通信ハンドラーの再登録は行わない。コンパイル用依存は3.6.0を維持する。

3.8では難読化されたアイテム定義の名前から元のモデル名を判別できなくなった。`ModelImportedEvent` でMotionRPが所有する発光モデルのrendererを特定し、実際のアイテムモデル参照を取得する。BetterModelの生成ZIPに `assets/motionrp/energy-targets.json` を加え、その対応表に記録された対象だけを補正する。難読化の有効・無効や、出力ファイル名の命名規則には依存しない。

虚化マスクの片面化・通常エネルギーの明るさ補正は専用のコピーを作る方式を維持し、共有する三角形プリミティブや他のアイテム定義は変更しない。対応表のない旧パックは従来の名前による補正を継続する。

## 検証

- 新旧APIでのJavaコンパイル。
- 難読化ありの3.8生成ZIPで44対象（エネルギー36、マスク8）の参照を取得し、すべての補正先を確認。
- 発光コピー・マスクの外向き片面・共有プリミティブの保持・別プラグイン相当のアイテムの保持・overlay・再実行時の不変性を単体検証。
- 3.8.0上でフュージョン、デスソード、エターナルブリザード、デススピアー、ファイアトルネード、皇帝ペンギン1号、ギガントピストル、炎帝、バルスの実行・中断・表示OFF・後片付けを既存の検証で確認。蹴りとボールの接触・着地などの数値検証も通過した。
- 3.6.0へ戻した環境でも、0ダメージ・微小移動・アニメーションの早期終了では演出を維持し、実ダメージ・外部移動・モデルの外部終了・明示的な中止では正しく終了する回帰検証に通過。単体アニメーションの自然終了と空中移動、二人用の停止理由も確認した。3.6.0のパックでも44対象すべての補正とマスクの片面を確認。
- Minecraft画面を使った全31種の目視比較までは行っていない。

3.8はアニメーションの内部更新をMinecraftの50ms tickへ合わせている。演出全体を一律2倍速・半分速にする変更は入れない。既存のポーズと接触・着地検証で互換性を確認する。

## 更新

MotionRPのJARとBetterModelのPaper版JARを差し替え、サーバーを完全再起動する。起動後にBetterModelのZIP再生成と配布準備の完了を待ち、各プレイヤーが `/motionrp reload` で更新リソパを適用する。登録内容、54枠、選択ページ、お気に入りの保存形式は変更しない。

3.8の別のsnapshotや今後の正式版を、このビルドの検証結果だけで動作保証するものではない。

## 配布元

- [検証したPaper版](https://modrinth.com/plugin/bettermodel/version/Kimt65Sx)
- [対応するソースコミット](https://github.com/toxicity188/BetterModel/commit/56489a0731079ec6e7c622fc88fa5bf372a19e92)
- Paper JARのSHA-512: `deefb9f5879334386b329488fc0fd010672f908c1ec8cf1b4da1de8aa078a5ff8bd795d8346d070673de60a864cbeabc7874c146f6833a7e2ffbfbd873e9356d`
