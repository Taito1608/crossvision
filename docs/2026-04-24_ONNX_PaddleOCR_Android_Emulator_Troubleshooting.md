# ONNX PaddleOCR Androidエミュレーター上での動作確認トラブル

**作成日:** 2026-04-24
**更新日:** 2026-04-24（検証追記）

---

## 1. 環境情報

### OS / ツール
- **開発環境**: Android Studio（Gradle ビルド使用）
- **テストデバイス**: Android エミュレーター
- **ONNX Runtime**: `com.microsoft.onnxruntime:onnxruntime-android:1.16.3`
- **ブランチ**: soya-ocr

### 関連ファイルパス（プロジェクトルートからの相対パス）
- ONNXエンジン: `app/src/main/java/com/example/solution_development/ocr/OnnxOcrEngine.kt`
- テストActivity: `app/src/main/java/com/example/solution_development/test/OnnxTestActivity.kt`
- ONNXモデル: `app/src/main/assets/onnx/pnnx_english.onnx`
- 文字辞書: `app/src/main/assets/onnx/dict.txt`
- テスト画像: `app/src/main/assets/onnx/images/M4sb29-2.jpg`

### モデルファイル情報
- ファイルサイズ: 7,830,888 bytes（約7.8MB）
- 変換元: PaddleOCR Englishモデル → ONNX（変換ツール名不明）
- PyTorch ONNX export で作成されたもの（ファイル名 `pnnx_` から PNNX 変換の可能性）

### ONNXモデル仕様（メタ情報）
- **Input**: `x`, shape=[-1, 3, 48, -1], tensor=FLOAT, NCHW形式
- **Output**: `fetch_name_0`, shape=[-1, -1, 438], tensor=FLOAT
  - 実行時: shape=[1, 40, 438]（batch=1, timeSteps=40, numClasses=438）
- **char dict size**: 436文字
- **vocab diff**: numClasses=438, dictSize=436, diff=2（blank=0 + 追加=437 の可能性）

### dictの内容（app/src/main/assets/onnx/dict.txt）
- 0～9（数字10文字）
- A～Z（大文字26文字）
- 計436文字（改行区切り）

---

## 2. 何をしようとしていたか

1. `onnx/images/M4sb29-2.jpg`（実画像）をエミュレーター上で読み込む
2. ONNX Runtime経由で `pnnx_english.onnx` モデルに推論させる
3. OCR結果として画像内の文字列を取得する
4. 製品コード（例: `M4SB29-2` 形式のコード）を正規表現で抽出する

### 推論パイプライン（コード上の流れ）
1. 画像を `Assets` から読み込み、320×48にリサイズ
2. `bitmapToInputTensor()` で NCHWテンソルに変換
3. `session.run()` で推論実行
4. `decodeOutput()` で CTC greedy decode
5. `extractProductCodes()` で正規表現抽出

---

## 3. 何が起きたか

### 推論ログ（修正前）

```
Input stats: min=-1.8044444, max=2.64, avg=0.22681835135213582
First 20 input values (B): -0.0092, -0.2358, 0.0256, 1.3677, 1.4374, ...

Output stats: min=3.0529367E-8, max=0.90407306, avg=0.0022831050586655686
First 20 values: 0.904073, 0.001855, 0.011785, 0.010200, 0.003691, ...

Argmax summary: blank(blankIndex=0)=40/40, nonBlankClasses=0 unique classes
CTC decode: raw labels=0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ..., final=''
Decoded text: ''
```

### 1回目の修正後（正規化をPaddleOCR方式に変更）

```
Input stats: min=-1.0, max=1.0, avg=4.942812406524252E-4
First 20 input values (B): -0.1922, -0.2941, -0.1765, 0.4275, 0.4588, ...

Output stats: min=7.42415E-9, max=0.95414305, avg=0.002283105205806218
First 20 values: 0.948437, 0.000287, 0.001370, 0.001257, 0.003409, ...

Argmax summary: blank(blankIndex=0)=40/40, nonBlankClasses=0 unique classes
CTC decode: raw labels=0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ..., final=''
Decoded text: ''
```

### 2回目の修正後（チャンネル順序をBGR→RGBに変更）

```
Input stats: min=-1.0, max=1.0, avg=4.942812406524252E-4
First 20 input values (B): 0.1059, -0.0275, 0.0353, 0.5686, 0.6078, ...

Output stats: min=4.7305315E-9, max=0.9645706, avg=0.0022831047780326567
First 20 values: 0.956430, 0.000272, 0.001176, 0.001278, 0.002196, ...

Argmax summary: blank(blankIndex=0)=39/40, nonBlankClasses=1 unique classes
raw labels=[first10]=0, 0, 0, 0, 0, 0, 0, 0, 0, 0
CTC decode: ..., final='A'
Decoded text: 'A'
Extracted 0 product code(s): []
```

---

## 4. 確認済みのこと / 試したこと

### 確認済みの事実

| 項目 | 状態 | 内容 |
|------|------|------|
| ONNX Runtime依存 | ✅ | `onnxruntime-android:1.16.3` が解決済み |
| モデルファイル存在 | ✅ | `app/src/main/assets/onnx/pnnx_english.onnx` が存在 |
| モデル読み込み | ✅ | `Model loaded from assets, size: 7830888 bytes` |
| Session作成 | ✅ | `Session created successfully` |
| 入力名 | ✅ | `Input names: [x]` |
| 出力名 | ✅ | `Output names: [fetch_name_0]` |
| 入力shape | ✅ | `[1, 3, 48, 320]` (NCHW) |
| 出力shape | ✅ | `[1, 40, 438]` |
| テスト画像読み込み | ✅ | `bitmap size: 320x48` |
| 推論実行 | ✅ | `Session run completed` (例外なし) |
| Outputが全部0ではない | ✅ | `max=0.9645706` |

### 試したこと

| 対処 | 結果 |
|------|------|
| **正規化修正1**: ImageNet→PaddleOCR方式 `(pixel-127.5)/127.5` | Input分布改善（avg=0.0005）。出力は変わらず（blank=40/40） |
| **チャンネル順序修正**: BGR→RGB（PyTorch/PNNX由来モデルの場合） | blank=40/40 → blank=39/40, 出力='A'。ただし実質的な改善なし |
---

## 5. 追記: Perplexity相談後の検証（2026-04-24 01:00〜）

### 5-1. Python 検証結果

Python onnxruntime + 同じモデル + 同じ画像で検証:

| 前処理パターン | blank_count/40 | decoded | 備考 |
|---|---|---|---|
| BGR→RGB Stretch 320x48 | 39 | 'AS' | Androidと同じ |
| RGB (PIL) Stretch 320x48 | 39 | 'A' | |
| Grayscale→3ch Stretch | 40 | '' | |
| **アスペクト比維持+パディング** | **33** | **'cYmPob'** | **改善！** |
| **ROTATED_90+アスペクト比維持+パディング** | **24** | **'cYnaGetaYeYelowb'** | **さらに改善！** |
| BAND@0.3（画像の30%位置） | 39 | 'T' | |
| BAND@0.5（画像の50%位置） | 36 | 'LUME' | |
| All-zeros入力 | 36 | 'EPY' | ゼロ画像でも出力あり |
| Random noise | 40 | '' | |

**感度チェック**: |blank - noise| = 0.001135, |blank - img| = 0.001273 → モデルは入力に応答しているが、stretchでは情報が破壊されている。

### 5-2. Python検証の結論

- **モデルは動作している**。アスペクト比を維持すると文字が検出される
- テスト画像 `M4sb29-2.jpg` は **1536x2048 (portrait)** で 縦長 → 320x48 (landscape) にstretchすると情報が破壊される
- 「モデルが壊れている」仮説は棄却。**前処理の不一致が根本原因**
- 回転版で `cYnaGetaYeYelowb` → テキスト方向の問題も関係する可能性あり

### 5-3. Android側の実装変更（2026-04-24 01:30実装済み）

OnnxOcrEngine.kt の `bitmapToInputTensor()` を以下のように修正:

1. **正規化**: ImageNet → PaddleOCR方式 `(pixel-127.5)/127.5` ✓
2. **チャンネル順序**: BGR → RGB（PyTorch/PNNX由来モデル用）✓
3. **アスペクト比維持リサイズ**: 高さ48固定、幅は比率に応じて、320まで0パディング ✓
4. **デバッグログ追加**: `Resize: WxH → resizedWx48 (padded to 320x48)`

### 5-4. Android修正後の結果（2026-04-24 01:37）

```
extractProductCode() called, bitmap size: 320x48
Resize: 320x48 → 320x48 (padded to 320x48)    ← アスペクト比維持が効いていない！
Argmax summary: blank(blankIndex=0)=39/40, nonBlankClasses=1 unique classes
Decoded text: 'A'
```

### 5-5. 新規に発見した問題

**OnnxTestActivity.kt の `loadAssetBitmap()` 内で画像を予め 320x48 に stretch している**:

```kotlin
private fun loadAssetBitmap(assetPath: String): Bitmap? {
    assets.open(assetPath).use { inputStream ->
        val raw = BitmapFactory.decodeStream(inputStream) ?: return null
        Bitmap.createScaledBitmap(raw, 320, 48, true)  // ← ここで既にアスペクト比が破壊されている！
    }
}
```

OnnxOcrEngine の `bitmapToInputTensor()` は既に 320x48 に破壊されたビットマップを入力として受け取っているため、アスペクト比維持処理が無効化されている。

### 5-6. 現在の状況

- Python検証 → モデルは正常、アスペクト比維持で文字検出確認済み
- Android実装修正 → OnnxOcrEngine側は修正済み（アスペクト比維持）
- **残る問題** → OnnxTestActivity の呼び出し側で予め 320x48 に変形している
- 次の対応: OnnxTestActivity で画像を元のサイズのまま渡し、OnnxOcrEngine 側でアスペクト比維持+パディング処理を適用させれば改善する可能性が高い