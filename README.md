# CrossVision

Android アプリから製品コードを一括登録するプロジェクトです。

## セットアップ

1. JDK 11 を用意
2. Android SDK をインストール
3. 依存関係を解決

```bash
bash ./gradlew :app:dependencies
```

## APIトークン設定（gradle.properties）

`POST /products` の送信には API トークンが必要です。

`gradle.properties` に次を設定してください。

```properties
crossvisionApiToken=YOUR_API_TOKEN
```

この値は `app/build.gradle.kts` で `BuildConfig.CROSSVISION_API_TOKEN` に注入されます。

## ビルド

```bash
bash ./gradlew :app:compileDebugKotlin
```

## 実行フロー

1. ホーム画面で工事・工程を選択
2. カメラ画面で製品コードを読み取り
3. 確認画面で各コードの進捗を選択
4. 「確定」を押すと `POST /products` に一括送信

## API 送信仕様（アプリ側）

- エンドポイント: `https://crossvision-api.hirocr-api.workers.dev/products`
- メソッド: `POST`
- ヘッダー:
  - `Content-Type: application/json`
  - `Authorization: Bearer <CROSSVISION_API_TOKEN>`
- ボディ（配列）:

```json
[
  {
    "code": "A001",
    "status": "active",
    "process": "inspection",
    "construction": "line1"
  }
]
```

## 補足

- 同一リスト内に重複コードがある場合、アプリ側で送信前にエラー表示します。
- サーバーエラー時は「送信に失敗しました」が表示されます。
