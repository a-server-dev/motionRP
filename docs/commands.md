# コマンドと権限（v0.95.0）

ルートは `/motionrp`、短縮形は `/mrp` です。Minecraft標準の `/give` は変更しません。

| コマンド | 動作 | 権限・利用者 |
|---|---|---|
| `/motionrp`、`/motionrp gui` | 登録GUI | `motionrp.use`、プレイヤーのみ、初期値は全員 |
| `/motionrp view [on\|off]` | 演出表示切替・保存（省略で反転） | `motionrp.use`、プレイヤーのみ |
| `/motionrp accept [招待ID]` | 届いている二人用招待を承諾 | `motionrp.use`、プレイヤーのみ |
| `/motionrp laststop` | 最後の停止理由 | `motionrp.use`、プレイヤーのみ |
| `/motionrp reload` | 自分のリソースパックを再適用 | `motionrp.reload`、プレイヤーのみ、初期値OP |
| `/motionrp give controller [プレイヤー]` | 空の専用セットを配布 | `motionrp.give.controller`、初期値は全員 |
| `/motionrp give motion [プレイヤー]` | 入手可能な全種入り専用セットを配布 | `motionrp.give.motion`、OP／EmeraldREX91／コンソール・RCON限定 |
| `/motionrp status` | プラグイン・モデル・パック状態確認 | `motionrp.status`、初期値OP |
| `/motionrp configreload` | 設定再読込、再生中のモーションを停止 | `motionrp.configreload`、初期値OP |
| `/motionrp pack [status]` | パック状態確認 | `motionrp.pack`、初期値OP |
| `/motionrp pack publish` | 設定されたパック公開処理を予約 | `motionrp.pack`、初期値OP |

配布先省略時は自分です。コンソール・RCONでは配布先指定が必要です。他人への配布には、追加で `motionrp.give.others` とOP／EmeraldREX91／コンソール・RCONの条件が必要です。一般プレイヤーは自分用の空セットのみ取得できます。

`motionrp.admin`（初期値OP）は `motionrp.status`・`motionrp.configreload`・`motionrp.pack` の親権限です。配布の制限は解除しません。

`motionrp.use` はアイテム操作による登録・再生にも適用します。個別モーション配布（ID・個数指定）、`give all`、`fusion`・`broly`・`balse`・`emperor_penguin_2` の直接実行、および `/goalperformance`・`/hrperformance` は廃止しました。二人用はアイテムから招待します。既存アイテム・登録内容は保持します。

他プラグインのリソースパック表示への影響を避けるため、`reload` は一般プレイヤーから実行できず、Tab候補にも表示しません。必要な担当者だけに `motionrp.reload` を許可できます。`motionrp.admin` や `motionrp.use` からは付与しません。

## LuckPerms

Bukkit標準の権限判定を使用するため、LuckPermsを追加するだけで設定できます。MotionRPからLuckPermsへの必須依存はありません。

```text
/lp group default permission set motionrp.give.controller true
/lp user <名前> permission set motionrp.use false
/lp group staff permission set motionrp.status true
/lp group admin permission set motionrp.admin true
/lp user EmeraldREX91 permission set motionrp.give.motion false
```

OP／EmeraldREX91の配布資格は固定です。他の非OPに `motionrp.give.motion true` や `motionrp.* true` を付けても全種セットは配布できません。配布資格を持つ人には、明示的な権限拒否を設定できます。EmeraldREX91は未設定なら全種セット・他人への配布を利用できますが、管理権限は自動付与しません。

アップグレード後はJARを差し替えて完全再起動してください。コマンド登録の変更は `/motionrp configreload` だけでは反映されません。
