# motionRP

Minecraft **Paper 26.3 / Java 25 / BetterModel 3.6.0** 用のプレイヤーモーションプラグインです。Velocity配下ではPaper側に導入します。Velocity用の追加プラグインは必要ありません。

リポジトリ: https://github.com/a-server-dev/motionRP

## 自動ビルド・ソース

[GitHub ActionsのBuild](https://github.com/a-server-dev/motionRP/actions/workflows/build.yml) はpush・Pull Request・手動実行で動きます。Java 25でビルドと単体テストを実行し、成功した実行の **Artifacts → MotionRP-コミットID** から以下を取得できます（保存期間30日）。

- プラグインJAR。
- 配布ZIP（JAR・編集可能bbmodel・設定例・README・ライセンス）。
- ソースZIP（Java・テスト・モデル・生成スクリプト・Gradle Wrapper・結合テスト・Actions定義）。

テスト結果は別の `test-reports-コミットID` に保存します。BetterModelが生成する実際のリソースパックは、導入先サーバーでモデルを読み込んだ後の `plugins/BetterModel/build.zip` を配布します。

```sh
git clone https://github.com/a-server-dev/motionRP.git
cd motionRP
./gradlew --no-daemon clean build distributionZip sourceBundle
```

Windowsでは `gradlew.bat` を使用できます。ビルドにクラウド環境専用ファイルや認証トークンは必要ありません。ソースは `src/`、編集用モデルは `src/main/resources/bettermodel/`、生成・検証準備は `tools/`、結合テストは `integration/`、自動ビルド定義は `.github/workflows/build.yml` にあります。

## 螺旋丸（影分身）

### 0.2.3：低い構えと実際の突進

本体は足を外側へ開き、膝を曲げて腰を下げ、上体を前へ傾けて球を溜めます。分身にも膝の曲げを追加し、球の高さを本体の構えに合わせました。分身が消えた後は身体をひねって引き、前後に脚を開いた姿勢で螺旋丸を突き出します。

突き出す0.5秒間に、実際のプレイヤーが開始時の水平方向へ最大4ブロック進みます。進路を細かく調べ、壁・足場の途切れ・未読込チャンク・ワールド境界の手前で止まります。段差を自動で登る処理はありません。ダメージやブロック破壊は発生しません。途中キャンセルでは移動も終了し、移動前の位置へ戻すことはしません。

旧JARを外して `motionRP-0.2.3.jar` に差し替え、Paperを再起動してください。未編集の0.2.0〜0.2.2標準プレイヤーモデルはバックアップ後に自動更新します。独自編集したモデルは保持し、更新候補を `plugins/MotionRP/model-updates/` に出力します。必要に応じて編集内容を統合してください。

### 0.2.2：分身・球の表示対策と診断

分身・球は、同一ワールド・48ブロック以内・プレイヤーの非表示設定を満たす相手へ送ります。この近距離演出にはBetterModelのプレイヤー視線による汎用カリングを適用せず、三人称カメラの向きや `min-sight` 設定に左右されず描画情報を送るようにしました。表示の登録に失敗した相手には、短い間隔で再試行します。BetterModel全体の設定は変更しません。

「利用可能」はモデルの読み込み確認であり、クライアントでの描画成功を意味しません。粒子・音だけ出る場合の診断には、`/motionrp status` の版番号・effect・視界設定を確認してください。`debug-input: true` にして `/motionrp reload` 後に1回再生すると、コンソールへ `Rasengan visibility` が出ます。`spawned` は表示登録、`hidden` は非表示状態、`view` は描画送信の対象、`models` は可視モデル部品数です。診断後は `debug-input: false` に戻してください。

今回の対策は自分・他プレイヤー双方への実アイテムメタデータ送信を検証していますが、報告された環境での原因はまだ確定していません。

### 0.2.1：腕の向きの修正

本体と分身の腕が背中側へ上がっていた回転方向を修正しました。モデルが向く前方（Blockbenchの-Z側）へ両手を伸ばす姿勢に変更しています。

旧JARを外し、`motionRP-0.2.1.jar` に差し替えてPaperを再起動してください。未編集の0.2.0標準モデルはSHA256で識別し、`plugins/MotionRP/model-backups/` に保存してから修正版に自動更新します。独自編集されたモデルは上書きせず、修正版候補を `plugins/MotionRP/model-updates/` に出力します。その場合は編集内容を確認して反映し、BetterModelを再読み込みしてください。

紫黒の市松模様はリソースパック未適用時の表示です。螺旋丸を追加した後の `plugins/BetterModel/build.zip` を適用してください。今回の腕の修正はサーバー側アニメーションの変更です。

0.2.0に、分身が球を練る演出を含む約4.8秒のモーションを追加しました。

| 時間の目安 | 演出 |
| --- | --- |
| 0〜0.4秒 | 本体が印を結ぶ |
| 0.4〜1秒 | 煙とともに分身1体が現れ、合計2人になる |
| 1〜3秒 | 分身が両手を動かし、本体の手元の青い球が成長。3軸のリング・粒子が回転 |
| 3〜3.6秒 | 分身が煙で消え、本体が螺旋丸を引いて構える |
| 3.6〜4.1秒 | 低い姿勢から螺旋丸を突き出し、実際のプレイヤーが最大4ブロック突進する |
| 4.1〜4.8秒 | 発光・粒子と音が収束し、元の姿へ戻る |

分身はプレイヤーのスキンプロフィールを使う表示モデルで、操作可能なプレイヤーや戦闘NPCではありません。ダメージ・ノックバック・ブロック破壊は発生しません。途中キャンセルでは本体・分身・球をすべて消し、後続の粒子・音も停止します。近くの他プレイヤーにも見えます。

```text
/motionrp give controller
/motionrp give motion rasengan 1
```

セットアイテムを右クリックして、所持品の「螺旋丸（影分身）」をクリックで登録し、GUIを閉じて左クリックすると再生します。前転も登録済みなら、シフト左クリックで選択します。

初回起動時に同梱bbmodelをBetterModelの `players/` と `models/` へ追加し、BetterModelを再読み込みしてパックを再生成します。既知の未編集旧版モデルはバックアップ後に更新し、独自編集モデルは保持します。`/motionrp status` が `rasengan: 利用可能` になるまで数秒待ってください。既存のMotionRP設定には、この新規定義だけを追加し、既存項目は保持します。

**更新された `BetterModel/build.zip` をクライアントに再配布してください。以前のパックでは追加したモデルは表示できません。**

編集できる同梱モデルは以下です。

- `src/main/resources/bettermodel/players/motionrp_rasengan.bbmodel`：本体と分身の動き。
- `src/main/resources/bettermodel/models/motionrp_rasengan_orb.bbmodel`：青い球・発光・回転リング。

Blockbenchで開いて編集できます。プレイヤーリグはBetterModel 3.6.0のMITライセンス素材を基にしています。追加のアニメーションと球・パレットはMotionRP用に作成しています。ライセンス表記は同梱しています。生成スクリプトは `tools/generate_rasengan.py`（元のsteve.bbmodelのSHA256を照合）です。

## 操作

| 専用セットアイテムの操作 | 動作 |
| --- | --- |
| 右クリック | 9枠の登録GUIを開く |
| 左クリック | 選択中のモーションを1回再生。再生中に押すと終了 |
| シフト＋左クリック | 次の登録済み枠に切り替える。空き枠を飛ばし、最後の次は先頭 |

GUIを開いた状態で、**下の所持品のモーションアイテムをクリックすると、最初の空き枠へ1個だけ預けます**。上の登録済み枠をクリックすると、その1個を所持品へ返します。GUIを閉じても登録内容は残ります。左・右・シフトクリックはいずれも1個ずつです。ドラッグによる登録や枠の入れ替えには対応していません。

同じ定義から配布したモーションアイテムは最大64個までスタックできます。所持品が満杯で返せない場合は登録したままにし、地面には捨てません。別のアイテムやGUIの表示コピーは登録・取り出しできません。

移動（ジャンプを含む）・有効な被ダメージ・外部からのテレポート・死亡・ログアウト・持ち替え・プラグイン停止で再生を終了します。螺旋丸の突進が行う移動だけは終了の対象から外します。向きだけの変更では終了しません。乗車・滑空・就寝中は再生できません。

## 導入

1. Java 25で `./gradlew build` を実行します。
2. `build/libs/motionRP-0.2.3.jar` とBetterModel 3.6.0のPaper版をPaperの `plugins/` に置き、再起動します。旧MotionRP JARは同時に置かないでください。
3. `/motionrp status` で `roll: 利用可能` と `rasengan: 利用可能` を確認します。前転はBetterModel標準の `players/steve.bbmodel` を使い、螺旋丸用モデルは自動配置されます。
4. 管理者が `/motionrp give controller` と `/motionrp give motion roll 64` を実行します。
5. 専用セットアイテムを右クリックし、所持品のモーションアイテムをクリックして登録します。

このクラウド環境では、先に `source /workspace/tooling/activate.sh` を実行してください。Gradle Wrapperには配布ZIPのSHA256検証を設定しています。

### リソースパック

BetterModelが生成する `plugins/BetterModel/build.zip` をプレイヤーに適用する必要があります。モデルを追加したらBetterModelを再読み込みし、生成し直したパックを配布してください。

配布例は、生成ZIPをHTTPSで配信し、Paperの `server.properties` の `resource-pack` にそのURL、`resource-pack-sha1` にZIPのSHA1を設定する方法です。全員への適用を必須にする場合は `require-resource-pack=true` を設定します。既存パックがある場合はBetterModel側のパック結合設定を使ってください。

MotionRPはBetterModel APIで周囲のプレイヤーにもモデルを表示しますが、リソースパックのホスティングやURLの自動設定は行いません。パックが未適用なら正しい見た目にはなりません。

## コマンドと権限

| コマンド | 権限 |
| --- | --- |
| `/motionrp` または `/motionrp gui` | `motionrp.use`（標準で全員） |
| `/motionrp give controller [プレイヤー]` | `motionrp.admin`（標準でOP） |
| `/motionrp give motion <ID> [個数1〜64] [プレイヤー]` | `motionrp.admin` |
| `/motionrp status` | `motionrp.admin` |
| `/motionrp reload` | `motionrp.admin` |

別名は `/mrp` です。コンソールから配布するときはオンラインのプレイヤー名を指定してください。所持品に収まらない配布は中止します。

## モーションの追加

1. BetterModel用のプレイヤーモデル（`.bbmodel`）にアニメーションを作り、`plugins/BetterModel/players/` に配置します。通常のエンティティモデルではなく、プレイヤー用の骨構造を使用します。
2. BetterModelを再読み込みし、リソースパックを再生成・配布します。
3. `plugins/MotionRP/config.yml` の `motions` に定義を追加します。

```yaml
motions:
  wave:
    name: 手を振る
    model: your_player_model
    animation: wave
    max-ticks: 100
    # item-model: yourpack:wave_token
```

4. `/motionrp reload` → `/motionrp status` で確認し、`/motionrp give motion wave` で配布します。

`item-model` は任意で、アイテムの見た目を外部パックのモデルに変更できます。モデル自体やアイテム用テクスチャは別途パックに用意してください。`max-ticks` は再生が終了しなかった場合の強制終了上限（1〜12000 tick）です。通常はアニメーションの終了時に元のプレイヤー表示へ戻ります。

定義を削除しても、預けたアイテムは消さずGUIから返却できます。その定義の再生と新規登録は停止します。既存の別プラグインによるモデル表示がある場合は、勝手に閉じず、再生を拒否します。

通常の定義では `effect: none`（省略可）、螺旋丸のシーンには `effect: rasengan` を使用します。螺旋丸のプレイヤーモデルには `rasengan_helper`、球モデルには `spin` が必要です。螺旋丸の `max-ticks` は96以上にしてください。同梱モデルをサーバー上で編集した場合は上書きしないので、更新版を反映したいときは既存ファイルをバックアップして差し替え、BetterModelを再読み込みしてください。

## 保存

登録したアイテムの実データと選択番号を、プレイヤーのPersistentDataContainer（`motionrp:loadout_v1`）に保存します。所有者はセットアイテムではなくプレイヤーです。セットアイテムを交換しても登録は共有されます。

登録・返却・選択変更時に `Player.saveData()` を呼び、通常の所持品と預かりデータを同じプレイヤーデータに記録します。Paper 26.3で確認した保存先は `world/players/data/<UUID>.dat` です。別ファイルへの二重書き込みはありません。保存形式はバージョン・サイズ上限・チェックサムを持ち、読めないデータを空として上書きしません。バックアップはプレイヤーデータ全体を取得してください。プロセス強制終了やディスク障害に対する追加のトランザクション保証はありません。

登録は死亡後も保持します。アンインストール前にはGUIから預けたアイテムを回収してください。Velocity配下の複数Paper間でのデータ同期は含みません。

## 検証

```sh
./gradlew build
```

保存形式、破損・未知バージョンの拒否、9枠の選択・空き枠スキップ、満杯時の上書き拒否、2000回の登録・返却・保存復元での個数保存を検証します。

実サーバーの結合テストは `integration/` にあります。Node.js 22以上と、このクラウド環境に準備したPaper/BetterModelのキャッシュを使います。

```sh
source /workspace/tooling/activate.sh
./gradlew build
cd integration
npm ci --cache /workspace/tooling/npm-cache --ignore-scripts --no-audit --no-fund
npm test
```

`MOTIONRP_PAPER_FIXTURE` でキャッシュ元（標準 `/workspace/runtime/paper`）を指定できます。`build/integration-server/` を毎回作り直し、ループバック専用ポート25576でのみ動かします。通常サーバーの設定・プレイヤーデータは変更しません。

クラウド環境以外では、Java 25・Python 3.12以上・curl・Node.js 22以上を準備して、リポジトリ内のスクリプトで検証用キャッシュを作成できます。公式配布物のチェックサムを照合し、Mavenを厳格なチェックサム検証付きで実行します。

```sh
python3 tools/prepare_fixture.py build/paper-fixture
cd integration
npm ci --ignore-scripts --no-audit --no-fund
MOTIONRP_PAPER_FIXTURE="$(cd ../build/paper-fixture && pwd)" npm test
```

結合テストはゲームサーバーを起動するため、標準のActions Buildでは単体テストを実行します。

自動クライアントは26.1のため、テスト専用に公式ViaVersion/ViaBackwards 5.12.2をチェックサム検証して導入し、Paper 26.3へ接続します。この隔離サーバーだけはオフライン認証です。ネイティブ26.3クライアント、Mojang認証、Velocityのログイン処理、実画面でのパック適用・描画は別途確認が必要です。

結果は `build/integration-result.json`、ログは `build/integration-server.log`、単体テスト結果は `build/reports/tests/test/index.html` に出力します。

初版0.1.0では単体テスト11件・結合確認11項目が成功しました。結合確認には右クリックでGUIを開く操作、64個スタック、登録・返却、番号キー・ダブルクリック・ドロップの拒否、プレイヤー別保存、シフト左での選択、他クライアントへのモデル送信と終了時の削除、再起動後の復元、所持品満杯時の保持を含みます。通常のVelocity＋Paper構成でも、Via系プラグインを入れずに本体の有効化と `roll: 利用可能` を確認しました。

0.2.0では単体テスト17件・結合確認14項目が成功しました。追加モデルの骨・階層・キーフレームとパレットPNGの検証、演出タイムライン、旧版設定からの追加定義の移行、分身・球・本体のモデル送信、粒子と音、終了時と再クリックキャンセル時の全表示の削除を確認しています。実クライアントでの見た目、手と球の細かな位置合わせ、パック適用後の描画はまだ目視確認していません。

0.2.1では単体テスト22件・結合確認16項目が成功しました。腕の向きは、肩・肘・胸の回転を階層に沿って合成し、本体・分身の手首がモデルの前方に出ることを検証しています。旧標準モデルのバックアップと置換、独自編集の保持、再ログインやGUIの開き直しなしでの3回の完走と途中キャンセルも確認しました。

0.2.2では単体テスト22件・結合確認17項目が成功しました。結合テストのBetterModelに `min-sight: 0` を設定して観測者を反対向きにし、チャージ中にも本体側・観測者側へ分身と球の空でない描画アイテムが届くことを確認しています。テスト用ワールドはスライム等が検証を妨げないようPeacefulに設定し、演出中の最低体力も確認します。通常サーバーの難易度は変更しません。

0.2.3では単体テスト24件・結合確認21項目が成功しました。腰が下がり、膝が前へ曲がり、足先が腰より外側かつ地面付近に来ることを階層の回転・位置から検証しています。Paper 26.3では、溜め中の静止、突進時だけの実プレイヤー4ブロック移動、移動後の停止、壁・崖での停止、外部テレポートおよび突進中クリックでの中断を確認しました。実クライアントでの最終的な見た目は別途目視確認が必要です。
