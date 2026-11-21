# MotionRP Studio

公開先: https://a-server-dev.github.io/motionRP/

## 制作・保存

編集・プレビュー・ZIP書き出しはログイン不要で、ファイルをブラウザ内で扱います。作業はIndexedDBへ自動保存します。端末のデータを削除する前にZIPを書き出してください。

- PNGテクスチャを埋め込んだbbmodel（cube / mesh）を読み込みます。
- モデル配置、開始地点固定、プレイヤー追従、直線移動、アニメーション、粒子、サウンド、一時的なプレイヤー移動を設定できます。
- 20tick=1秒、位置はブロック単位の `[右, 上, 前]` です。終了tickは区間に含めず、その直前に終了位置へ到達します。
- プレイヤー本体モデルにはBetterModel標準の `player_root` リグを使います。本体未指定なら中立のプレイヤーモデルを生成します。
- 手・剣先など特定のボーンへの追従、任意Javaコード、コマンド実行、ダメージやブロック破壊には対応していません。
- プレビューは簡易表示です。粒子の形、音、補間の細部や実プレイヤーのスキンはMinecraftで確認してください。

読み込んだ表示用モデルは、演出のタイムラインにも自動配置します。既存の作業で未配置のモデルは、左のモデルを選び「演出に配置・編集」を押してください。bbmodelにアニメーションがあれば先頭のクリップを初期選択します。

簡易生成用の `parts` JSONは形と色のみで、ボーンアニメーションを作りません。モデル全体の移動はStudioの開始・終了位置で設定でき、ボーンアニメーションはBlockbench等で作成してbbmodelへ保存します。

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
