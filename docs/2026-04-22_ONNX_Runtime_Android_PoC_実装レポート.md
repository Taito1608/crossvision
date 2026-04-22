# ONNX Runtime Android PoC 実装レポート

**日付**: 2026-04-22  
**プロジェクト**: crossvision (Android, Kotlin)  
**ブランチ**: `soya-ocr`  
**目的**: PaddleOCR の ONNX モデルを Android アプリで動作させる PoC（朝の報告期限まで）

---

## 🎯 成果摘要

✅ **ONNX Runtime 1.16.3 を Android プロジェクトに統合**  
✅ **ダミーモデル（IR version 9, opset 9）を使用して推論テスト成功**  
✅ `OnnxTestActivity` にて Load Model / Run Inference の両操作が正常動作  
✅ 推論結果 `[49827570]` を確認（想定通りのダミー出力）

---

## 📋 実装内容

### 1. 環境セットアップ

- **依存関係**: `implementation "ai.onnxruntime:onnxruntime-android:1.16.3"`
- **ダミーモデル**: Python 3 (`onnx` ライブラリ) で生成
  - 入力: `input` (shape [1,3,224,224])
  - 出力: `output` (shape [1,1000])
  - 値: 定数 0.5 を代入（1000要素）
- **配置**: `app/src/main/assets/dummy_model.onnx`

### 2. 主要クラス

#### `OnnxOcrEngine.kt`

- `loadModel()`: assets からモデル読み込み、`OrtSession` 作成
- `extractProductCode(Bitmap)`: 224x224 の白色Bitmapから入力テンソル生成し推論
- `bitmapToInputTensor()`: FloatArray(3*224*224) を 0.5f で初期化
- ログによる詳細なデバッグ出力

#### `OnnxTestActivity.kt`

- Load Model / Run Inference の2ボタン
- UI スレッド分離（`Thread { ... }.start()`）
- 結果を `TextView` に表示

### 3. Git 履歴（soya-ocr ブランチ）

| コミット | 内容 |
|---------|------|
| `d73a2c4` | INPUT_NAME/OUTPUT_NAME を `input`/`output` に変更 |
| `07de3e3` | デバッグログ追加、`.get()` 余計な呼び出し削除 |
| `9c0e8de` | `result.keys` → ログ削除 |
| `fc9d583` | `result` を `OrtSession.Result` にキャスト |
| `d1eccc2` | `getValue()` の代わりに `floatBuffer` 使用を試みる（失敗） |
| `e670c90` | `outputTensor` を `OnnxTensor` にキャスト（依然 ClassCastException） |
| `a92e8a7` | **API仕様に従い `Optional<OnnxValue>` を安全に処理**（成功） |

---

## 🔍 トラブルシューティング

### 問題1: モデル IR バージョン不兼容

- **現象**: `Unsupported model IR version: 13, max supported IR version: 9`
- **原因**: 生成したダミーモデルの IR version が 13 だった
- **解決**: IR version 9 で再生成 `dummy_model.onnx`

### 問題2: 入力/出力名不一致

- **現象**: `result.get(OUTPUT_NAME)` で例外
- **原因**: コード内の `"data_0"`/`"softmaxout_1"` とモデルの actual 名が不一致
- **解決**: `INPUT_NAME="input"`, `OUTPUT_NAME="output"` に修正

### 問題3: `ClassCastException: java.util.Optional cannot be cast to ai.onnxruntime.OnnxTensor`

- **現象**: `val outputTensor = result.get(OUTPUT_NAME) as OnnxTensor` で失敗
- **根本原因**: `OrtSession.Result.get(String)` の戻り値は `Optional<OnnxValue>` であるため、直接 `OnnxTensor` へキャストできない
- **誤解**: `result.get(OUTPUT_NAME)` が `OnnxTensor` を返すと思っていた
- **調査**: Research Agent を起動し ONNX Runtime Java API を調査
- **公式仕様**: `public Optional<OnnxValue> get(String key)`
- **解決パターン**:
  ```kotlin
  val outputTensor = result.get(OUTPUT_NAME)?.get() as? OnnxTensor
  ```
  - `result.get(key)` → `Optional<OnnxValue>`
  - `?.get()` → `OnnxValue?`
  - `as? OnnxTensor` → 安全なキャスト

### 問題4: Kotlin の `getValue()` メソッド曖昧性

- **現象**: `outputTensor.getValue()` の呼び出しでコンパイルエラー
- **原因**: Kotlin の `getValue` 演算子（`Map.getValue` など）と競合
- **解決**: `floatBuffer` プロパティ経由でデータを取得
  ```kotlin
  val floatBuffer = outputTensor.floatBuffer
  val outputArray = FloatArray(floatBuffer.remaining())
  floatBuffer.get(outputArray)
  ```

---

## 🧪 テスト結果

### 成功ログ（抜粋）

```
2026-04-22 03:29:18.523 D OnnxOcrEngine: Load Model result: true
2026-04-22 03:29:20.837 D OnnxTest: Inference result: [49827570]
```

### 完全な推論フロー

```
D/OnnxTest: Run Inference button clicked
D/OnnxOcrEngine: extractProductCode() called
D/OnnxOcrEngine: Creating input tensor
D/OnnxOcrEngine: Input tensor created
D/OnnxOcrEngine: Running session.run() with input: input
D/OnnxOcrEngine: Getting output tensor: output
D/OnnxOcrEngine: Output tensor obtained
D/OnnxOcrEngine: Output array size: 1000, first 5: [0.5, 0.5, 0.5, 0.5, 0.5]
D/OnnxTest: Inference result: [49827570]
```

---

## 📌 現状

- **ONNX Runtime  basic 統合完了**
- **PoC テスト成功**: ダミーモデルで正常な推論を確認
- **次のステップ**: 本番の PaddleOCR 英語モデル（ONNX 変換済み）を探して適用

---

## 🚀 今後の計画（明朝以降）

### 短期（今日中）
1. **既存 ONNX PaddleOCR 英語モデルの探索**
   - 公開されている ONNX 形式の PaddleOCR 英語モデルを探す
   - あるいは PaddleOCR 自体の ONNX 変換手順を調査

2. `OnnxOcrEngine` の拡張
   - ダミー出力 `["49827570"]` を実際の OCR 結果に置き換え
   - 後処理：argmax + 文字列パース（製品コード形式 `[A-Z0-9]{3,}-?[0-9]*`）

### 中期（チーム開発）
3. **CameraX 統合**
   - `CameraActivity` から `OnnxOcrEngine` を呼び出し
   - プレビュー画像をキャプチャし推論

4. **パフォーマンス最適化**
   - 推論時間の計測（目標 2-5秒/枚）
   - メモリ使用量の監視（4GB RAM 制約）

5. **実機テスト（SH-53D）**
   - エミュレータ → 実機
   - 速度・精度の検証

---

## 📚 学んだこと

1. **ONNX Runtime Java API は `Optional<OnnxValue>` を返す**
   - 直接キャストは禁物
   - 常に `?.get() as? OnnxTensor` で安全に取得

2. **Kotlin の `getValue()` は予約語的な演算子**
   - `OnnxTensor.getValue()` と Kotlin の `getValue` 拡張関数が競合
   - `floatBuffer` プロパティを使うことで回避可能

3. **API リファレンスの重要性**
   - 推測で実装すると `ClassCastException` など予期せぬ例外が出る
   - 公式ドキュメント or ソースコードでシグネチャを確認すべし

4. **デバッグログの重要性**
   - `output tensor obtained` といったログを挿入したことで、どの段階で失敗するか特定が容易になった

---

## 🎯 明朝へのTODO（國政さん向け）

- [ ] **PaddleOCR 英語モデル（ONNX）探し**
  - 公開リポジトリ（Hugging Face, GitHub）で検索
  - あるいは `paddle2onnx` での変換手順を調査
- [ ] `OnnxOcrEngine` に `OUTPUT_NAME` をモデルに合わせて調整（モデルによる）
- [ ] 出力 `1000` 次元のargmaxを取って文字列化するロジック実装
- [ ] チームミーティングで進捗報告と次の分担決め

---

**作成者**: CEO Agent (OpenClaw)  
**最終更新**: 2026-04-22 03:35 JST
