# Jelly-Bean-apk_Digital-signage

**Android 4.2.2に革命だ!!**

Jelly Bean Signage は、Android 4.2.2（API Level 17）の古い端末を、自分専用の壁掛け情報ディスプレイとして活用するプロジェクトです。

時計やカレンダー、画像・動画・音楽などを表示し、勉強や日常生活、趣味に使えるディスプレイを目指しています。

## 対応環境

- 最低対応OS: Android 4.2.2 / API 17
- 言語: Java
- UI: Android標準View
- ビルド: GitHub Actions
- 通信: 同一Wi-Fi内のローカル通信（Manager連携は段階的に実装）

## 主な機能

- 時計・日付
- 月間カレンダーとホーム画面
- 画像スライドショー
- 動画再生
- 音楽再生
- 端末内への設定保存
- Jelly Bean ManagerとのWi-Fi連携（開発予定）

## 使い方

1. GitHub Actionsからビルド済みAPKを取得します。
2. Android 4.2.2端末へAPKをインストールします。
3. 画像や音楽など、端末内のメディアを利用して表示できます。

対応する動画・音声形式は端末のハードウェアとAndroidのメディアデコーダーに依存します。

## ドキュメント

- [仕様書](docs/SPECIFICATION.md)
- [Wi-Fi通信仕様](docs/NETWORK_PROTOCOL.md)

## 開発方針

既存機能を活かしながら、Android API 17で動作する範囲で整理・拡張します。Manager連携は、接続状態、ペアリング、設定同期、メディア転送の順で実装します。通信機能が未実装の段階では、Wi-Fi連携が利用可能であるとは扱いません。

## Security

ローカルHTTP APIは信頼できるWi-Fi内でのみ利用する設計です。未知のネットワークでペアリングしないでください。
