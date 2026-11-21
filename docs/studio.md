# MotionRP Studio

公開先: https://a-server-dev.github.io/motionRP/

## 制作・保存

編集・プレビュー・ZIP書き出しはログイン不要で、ファイルをブラウザ内で扱います。作業はIndexedDBへ自動保存します。端末のデータを削除する前にZIPを書き出してください。

- PNGテクスチャを埋め込んだbbmodel（cube / mesh）を読み込みます。
- モデル配置、開始地点固定、プレイヤー追従、直線移動、アニメーション、粒子、サウンド、一時的なプレイヤー移動を設定できます。
- 20tick=1秒、位置はブロック単位の `[右, 上, 前]` です。終了tickは区間に含めず、その直前に終了位置へ到達します。
- Webの標準プレビューはスティーブです。プレイヤー本体モデルにはBetterModel標準の `player_root` リグを使います。書き出しでは既存のプレイヤー用リグ／スキン対応を保ちます。
- 手・剣先など特定のボーンへの追従、任意Javaコード、コマンド実行、ダメージやブロック破壊には対応していません。
- プレビューは簡易表示です。粒子の形、音、補間の細部や実プレイヤーのスキンはMinecraftで確認してください。

読み込んだ表示用モデルは、演出のタイムラインにも自動配置します。既存の作業で未配置のモデルは、左のモデルを選び「演出に配置・編集」を押してください。bbmodelにアニメーションがあれば先頭のクリップを初期選択します。

生成の標準対象は「スティーブの動き」です。固定のプレイヤーリグへ `target:"player"` のアニメーションJSONを適用し、本体アニメーションとして自動設定します。「小物・別モデルとその動き」に切り替えると、従来の簡易生成を使えます。

別モデル用の簡易生成JSONは `parts`（形・色）、`bones`（関節・親子関係）、`animations`（キーフレーム）に対応します。ChatGPTへの依頼文に動きを含め、返答を取り込むとアニメーション入りbbmodelへ変換します。先頭クリップを演出に自動設定し、必要なら演出の長さを延長します。「動くサンプルを入力」で手を振るロボットを試せます。静止モデル用の従来JSONもそのまま利用できます。

ChatGPT補助は「依頼文をコピー→別タブのChatGPTに貼り付け→返答のJSONをStudioへ貼り付け」の方式です。サイトからOpenAI APIを呼びません。ChatGPT側のログイン・回数・機能制限には従います。無料APIとして自動利用する仕組みではありません。

## PRの流れ

1. ZIPを書き出して展開します。
2. GitHubにログインして、自分の作業用Forkを作ります。
3. Forkの `projects/` に、ZIP内の作品IDのフォルダをそのままアップロードし、mainへコミットします。
4. サイトのPR案内へForkのユーザー名を入力し、比較画面を開きます。
5. 差分を確認してPRを作成します。初回の外部PRではActions実行の承認が必要な場合があります。
6. `Studio Build` の成果物を確認し、管理者が取り込みを判断します。自動マージはしません。

最終的な保存先はこのリポジトリの `projects/<id>/` です。Forkは提案用の作業コピーです。サイトにはリポジトリの書き込みトークンを置きません。公開・再配布できるモデルとテクスチャのみ提案してください。

取り込まれた有効な作品はPagesの作品ライブラリにも表示されます。改良するときは読み込んで編集し、再度PRで提案できます。

## JAR・リソパ

`Studio Build` はデータを検証し、Java 25でJARを作り、隔離したPaper 26.3 + BetterModel 3.6.0を起動して実際のリソパを生成します。公開リポジトリのGitHub Actionsを使い、秘密のAPIキーは不要です。利用はGitHub Actionsの制限・ポリシーに従います。Pages自身ではJavaを実行しません。

成果物には `MotionRP.jar`、`MotionRP-resource-pack.zip`、ハッシュ、導入説明、生成ログを含みます。既存機能も含む同じMotionRPプラグインへ作品を追加する方式です。PaperにBetterModelを別途導入し、生成したJARを置いて再起動してください。生成されたリソパをクライアントへ適用します。サーバー独自のBetterModelパック設定がある場合は、導入先で生成されたパックでも確認してください。

```
/motionrp give motion studio_<作品ID>
```

配布したモーションアイテムを通常のセットへ登録できます。既存の観覧ON/OFF、通知、リソパの個別再適用、二人用などの実装済み機能は維持します。Studioで新たに作る演出は現在一人用です。

## 形式と制限

サンプルは `projects/example/`（`enabled:false`）です。Webから書き出した作品は `enabled:true` でビルド対象になります。モーションIDとモデル名は作品IDで名前空間を分けます。モデルの更新時は、以前インストールしたファイルのハッシュが一致する場合だけ上書きし、サーバー上の独自編集は保持します。

- 作品ID・モデルID・イベントID: 英小文字から始まる32文字以内の英数字と `_`
- 長さ: 2〜12,000tick、モデル16個、イベント128個まで
- bbmodel: 1ファイル30MiB、作品合計80MiB、要素20,000・ボーン512・面200,000・階層128まで
- テクスチャ: 埋込PNG、1〜16枚、1枚8MiB・4096pxまで。外部URLやサーバー内のファイルを読みません。
- 配置モデルイベント24個、音48イベント、粒子発生量48個/tickまで
- 移動: ±64ブロック、5tick以上、最大1.5ブロック/tick。移動区間を重ねず、開始位置を前の終了位置に合わせます。終了・中断時には元の位置・飛行状態へ復帰します。
- コマンドや効果用アニメーター、数式入りキーフレームは受け付けません。アニメーションは位置・回転・スケールの数値キーに対応します。

## 開発者向け

```
npm --prefix editor ci
npm --prefix editor run dev
npm --prefix editor test
python3 tools/test_studio_projects.py
python3 tools/compile_studio_projects.py --check
./gradlew build distributionZip sourceBundle
python3 tools/build_studio_pack.py --jar build/libs/motionRP-0.42.0.jar
```

Java 25・Python 3・Node.js 24を使用します。`build_studio_pack.py` は公式配布のPaper / BetterModelを固定SHA-256で検証してダウンロードし、`build/studio-paper/` のループバック専用サーバーで生成します。実運用のサーバーやワールドは使用しません。

UIのブラウザテストは `cd editor && npx playwright test` です。Chromiumが `/usr/bin/chromium` にない環境では `playwright.config.mjs` の実行パスを設定してください。

## Pagesの初回公開設定

リポジトリ所有者が [Settings → Pages](https://github.com/a-server-dev/motionRP/settings/pages) で Source を **GitHub Actions** に設定します。接続アプリにはPagesを初回有効化する権限がないため、この設定だけは所有者の操作が必要です。その後、Actionsの **Studio Pages** を実行すると公開されます。公開設定が未完了でも、ビルド成功時にはサイト一式を `MotionRP-Studio-website` アーティファクトから取得できます。

## ChatGPT向けアニメーションJSON

完全な例は [`editor/public/animated-example.json`](../editor/public/animated-example.json) です。画面の依頼文にも形式を含めています。

- `bones`: `id`・`parent`・`origin`（関節位置）・任意の初期`rotation`。`root`は省略時に原点へ自動追加します。部品の`bone`で所属先を指定します。
- 部品の`from/to/origin`とボーンの`origin`はモデル全体の絶対座標です。16=1ブロック、Yが上、前が-Zです。親からの相対座標ではありません。
- `animations`: `name`・`length`（秒）・`loop`（真偽値）・`tracks`。各トラックは`bone`と`keyframes`を持ちます。
- キーは`time`（秒）と`rotation`（度）・`position`（初期姿勢からの相対移動）・`scale`（倍率）のいずれか。`interpolation`は`linear`または`step`です。
- 最大200部品・64ボーン（自動rootを除く）・32クリップ・合計4096キーチャンネルです。ボーンの循環・参照漏れ、範囲外時刻、数式、外部ファイルは受け付けません。

生成された動きの品質は依頼内容に左右されます。標準のスティーブ形式は固定リグを使ってプレイヤー本体を動かします。別モデル形式は、演出用の別モデルを生成します。

### スティーブの本体アニメーション（標準）

[手を振る例](../editor/public/steve-wave.json) のように `{"target":"player","animations":[...]}` を返します。`parts`や`bones`で形を作る必要はありません。使用できる関節は `root`・`hip`・`waist`・`chest`・`head`・`right_arm`・`right_forearm`・`left_arm`・`left_forearm`・`right_leg`・`right_foreleg`・`left_leg`・`left_foreleg` です。左右は本人基準、右が+X、前が-Zです。右腕の正のZ回転で横に腕が上がります。単位・キーフレーム形式は別モデルと同じです。

最初のクリップが自動で本体に設定されます。複数クリップは「プロジェクト → 本体アニメーション」から切り替えます。モデル配置イベントを追加する必要はありません。Web上ではテンプレートのデバッグ色をスティーブのスキンへ置き換えて表示し、書き出すプレイヤー用リグのボーン名・UV・スキン用テクスチャは保持します。

プレビュースキン `editor/public/steve.png` はMinecraft標準Steveテクスチャです（Mojang、Minecraft Java 1.21.8、[配布データの参照元](https://github.com/InventivetalentDev/minecraft-assets/blob/1.21.8/assets/minecraft/textures/entity/player/wide/steve.png)）。リグは本リポジトリの既存プレイヤーモデルから静止形状を使用しています。
