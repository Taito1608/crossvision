# PaddleOCR ONNX Android 環境別導入ガイド

**最終更新:** 2026-04-24
**適用ブランチ:** soea-ocr
**モデルバージョン:** PP-OCRv3 English Recognition (en_PP-OCRv3_rec_infer.onnx)

---

## 1. 概要

ONNX Runtime Androidを使用して、PaddleOCRの英語文字認識モデルをアプリに組み込む手順と注意事項をまとめた文書。「どの環境でも動く」ことを目的としている。

## 2. 前提条件

| 項目 | 値 |
|------|-----|
| Android Studio | 2023.3 以降（推奨） |
| minSdk | 26 (Android 8.0) |
| targetSdk | 34 |
| Kotlin | 2.0 / JDK 21 |
| Gradle | 8.9+ |
| Gradle Plugin | 8.7.0+ |

### 依存ライブラリ

```kotlin
// app/build.gradle.kts (アプリの依存関係ファイル)
implementation("com.microsoft.onnxruntime:onnxruntime-android:1.16.3")
```

**バージョン固定の理由:**
- `1.16.3`はPP-OCRv3 ONNX（opset=11）で動作確認済み
- バージョンを変更する場合は必ず `session.run()` で動作検証を行うこと
- `1.18.x`以降は一部のモデルでshapeエラーが発生する例がある

## 3. ファイル配置

プロジェクトの `assets` ディレクトリに配置する。ファイルパスはプロジェクトルートからの相対パス：

```
app/src/main/assets/onnx/
├── ppocrv3_rec.onnx       # PP-OCRv3 英語認識モデル (8.6 MB)
├── ppocrv3_dict.txt       # 辞書ファイル (96文字: 数字+英字+記号+スペース)
├── pnnx_english.onnx      # 旧モデル（フォールバック用、7.8 MB）
├── dict.txt               # 旧モデル用辞書 (436文字)
└── images/                # テスト画像
    └── test_label.jpg
```

### 重要: assets vs ファイルシステム

Androidの`assets`はファイルシステムディレクトリではない。
- `assets/`はAPKにバンドルされ、`AssetManager`経由でストリームとしてアクセスする
- `File("path/to/model.onnx")`形式のアクセスは**動作しない**
- 必ず`context.assets.open("onnx/ppocrv3_rec.onnx")`を使用すること
- `Environment.getExternalStorageDirectory()`など外部ストレージは本番では使用禁止

```kotlin
// ✅ 正しい: AssetManager経由
val inputStream = context.assets.open("onnx/ppocrv3_rec.onnx")

// ❌ 間違い: ファイルシステムとしてアクセス
val file = File(context.assetsDir, "onnx/ppocrv3_rec.onnx") // assetsDirはない！
```

## 4. モデル仕様

### PP-OCRv3 (高速・高精度)

| 項目 | 値 |
|------|-----|
| Input Name | `x` |
| Input Shape | `[-1, 3, 48, -1]` (NCHW, 動的幅対応) |
| Input Type | FLOAT |
| Output Name | `softmax_2.tmp_0` |
| Output Shape | `[-1, -1, 97]` (batch, timeSteps, numClasses) |
| numClasses | 97 |
| blank index | 0 |
| 辞書サイズ | 96文字 |

### PP-OCRv3 辞書構成 (96文字)

```
0-9 (10文字), ; < = > ? @ (記号6文字),
A-Z (26文字), ! " # $ % & ' ( ) * + , - . / (16文字),
: 等特殊記号, + スペース文字 (38文字)
```

### pnnx_english (旧・フォールバック用)

| 項目 | 値 |
|------|-----|
| Output Name | `fetch_name_0` |
| numClasses | 438 |
| 辞書サイズ | 436文字 |

**切り替え方法:**

```kotlin
// PP-OCRv3（デフォルト）
engine.setModelPreset(OnnxOcrEngine.ModelPreset.PP_OCR_V3_RAPIDOCR)

// 旧pnnxモデル（フォールバック時）
engine.setModelPreset(OnnxOcrEngine.ModelPreset.PNNX)
```

## 5. 前処理パイプライン（環境不変）

```
画像 → リサイズ(高さ48, 幅は比率で320以下) → RGB抽出 →
正規化: (pixel - 127.5) / 127.5 → -1.0 パディング → NCHWテンソル
```

### 5.1 重要: パディング値は `-1.0`

`FloatArray(totalSize) { -1.0f }` で初期化すること。

原因: Androidの`FloatArray`はデフォルト値が`0.0f`。`0.0`パディングはモデルにとって「灰色」のピクセルとして認識され、CTCのblankトークンへバイアスがかかり全空白出力になる。正規化後の黒(`(0-127.5)/127.5 = -1.0`)でパディングする必要がある。

```kotlin
// ❌ 間違い: デフォルト0で全てblank
val floatArray = FloatArray(totalSize)

// ✅ 正しい: -1.0で初期化
val floatArray = FloatArray(totalSize) { -1.0f }
```

### 5.2 重要: チャンネル順は `RGB`

PaddleOCRネイティブはBGRだが、PP-OCRv3のONNX変換版はRGBを期待する。

```kotlin
val r = ((pixel shr 16) and 0xFF).toFloat()
val g = ((pixel shr 8) and 0xFF).toFloat()
val b = (pixel and 0xFF).toFloat()

floatArray[base] = (r - 127.5f) / 127.5f          // R plane
floatArray[planeSize + base] = (g - 127.5f) / 127.5f  // G plane
floatArray[2 * planeSize + base] = (b - 127.5f) / 127.5f // B plane
```

### 5.3 重要: アスペクト比維持リサイズ

高さ48に固定。幅は `srcW / srcH * 48`（最大320）で計算。残りは -1.0 パディング。

```kotlin
val ratio = srcW.toFloat() / srcH.toFloat()
var resizedW = (ratio * 48).toInt()
if (resizedW > 320) resizedW = 320
val resized = Bitmap.createScaledBitmap(bitmap, resizedW, 48, true)
```

## 6. 環境間で問題になりやすい点と対策

### 6.1 ONNX Runtime バージョン違い

**問題:** デスクトップPCでは動いたがノートPCでは動かなかった事例

**原因候補:**
1. `app/build.gradle.kts`のonnxruntime-androidバージョンが環境間で異なっていた
2. `local.properties`のSDKパスが異なる
3. Gradleキャッシュが壊れていた

**対策:**
- バージョンは `app/build.gradle.kts` で**明示的に1行で指定**し、`versions.properties`や`libs.versions.toml`に依存しない
- `./gradlew clean` でキャッシュをリセットしてからビルド
- `Build → Clean Project` → `Build → Rebuild Project`でクリーンビルド

### 6.2 assetsファイルのサイズ制限

Androidのaapt2は`assets/`内のファイルの圧縮制御が難しいことがある。
`.onnx`ファイルは通常圧縮されないが、`.onnx.cars`のような拡張子は圧縮される可能性がある。

**対策:**
- 拡張子は`.onnx`のままにする
- もし読み込みエラーが出た場合、`app/build.gradle.kts`にnoCompress設定を追加:

```kotlin
android {
    aaptOptions {
        noCompress += listOf("onnx")
    }
}
```

### 6.3 エミュレーター vs 実機

- x86エミュレーターとARM実機でONNX Runtimeのビルドが異なる場合がある
- `onnxruntime-android`はAAR内部にarmeabi-v7a, arm64-v8a, x86_64の.soを含む
- エミュレーターでテストする場合は x86_64 ABI のエミュレーターを使用

### 6.4 Git LFS / 大きいファイル

`.onnx`ファイル(7-9MB)は通常のGitで追跡可能。ただし:
- `git lfs`使用時はclone後に`git lfs pull`が必要
- 本プロジェクトではLFS不使用。通常のGit追跡で管理

## 7. CTC デコード仕様

### 7.1 アルゴリズム

```
1. 出力shape: [batch, timeSteps, numClasses]
2. 各timestepでargmaxを取り、class indexを取得
3. blank(index=0)をスキップ
4. 連続する同じclassを除外 (例: "MM" → "M")
5. class index から辞書へ: dictIndex = classIndex - 1
```

### 7.2 辞書とクラスの対応

| class index | 意味 | 辞書インデックス |
|-------------|------|-----------------|
| 0 | blank (CTCの空白トークン) | - |
| 1 | dict[0] (文字'0') | 0 |
| 2 | dict[1] (文字'1') | 1 |
| ... | ... | ... |
| 96 | dict[95] (スペース) | 95 |

## 8. 製品コード抽出

推論結果から正規表現で製品コードを抽出:

```kotlin
// 現在の正規表現: 大文字英数字3文字以上 + 任意のハイフンと数字
val PRODUCT_CODE_PATTERN = Pattern.compile("[A-Z0-9]{3,}-?[0-9]*")
```

**注意点:**
- 辞書に小文字が含まれる場合、decoded text は小文字混じりになる
- 製品コードにマッチさせるには **`.uppercase()`** をかけてから正規表現を適用
- 現在の実装では `extractProductCodes()` 内で既に `text.uppercase()` を実行

## 9. EXIF Orientation 対応

`BitmapFactory.decodeStream()` は**EXIF orientationを適用しない**。
カメラ画像など方向情報が含まれる画像では、明示的な回転処理が必要。

```kotlin
// ExifInterfaceによる回転処理（必要に応じて実装）
val exif = ExifInterface(inputStream)
val orientation = exif.getAttributeInt(
    ExifInterface.TAG_ORIENTATION,
    ExifInterface.ORIENTATION_NORMAL
)
// orientationに応じてMatrixで回転
```

## 10. トラブルシューティング

| 症状 | 原因 | 対策 |
|------|------|------|
| 全空白出力 | パディングが0.0 | `-1.0f`で初期化 |
| 全空白出力 | チャンネル順がBGR | RGB順に変更 |
| `UnsatisfiedLinkError` | ONNX Runtime .soがロード不可 | ABIが一致しているか確認 |
| `Model file NOT found` | assetsパス間違い | `context.assets.open()` で確認 |
| `Output tensor not OnnxTensor` | 出力名が間違っている | `session.outputNames` を確認 |
| `ClassCastException` | 型キャストのミス | `onnxruntime-java`のバージョン確認 |

## 11. モデルファイルの入手法

### PP-OCRv3 公式モデルのONNX変換済みRapidOCR版

- **ファイル**: `en_PP-OCRv3_rec_infer.onnx` (8.6 MB)
- **ソース**: [SWHL/RapidOCR HuggingFace](https://huggingface.co/SWHL/RapidOCR)
- **ディレクトリ**: `PP-OCRv3/en_PP-OCRv3_rec_infer.onnx`
- **辞書**: en_dict.txt (95文字) + スペース = 96文字

```bash
# HuggingFaceからダウンロード
huggingface-cli download SWHL/RapidOCR PP-OCRv3/en_PP-OCRv3_rec_infer.onnx --local-dir ./downloads
```

## 12. コード構造

```
ocr/
├── OnnxOcrEngine.kt   # メンエンジン（モデル切替、前処理、CTCデコード、コード抽出）
test/
├── OnnxTestActivity.kt  # テストActivity（model preset, 画像ファイル指定）
assets/onnx/
├── ppocrv3_rec.onnx   # PP-OCRv3 英語認識モデル
├── ppocrv3_dict.txt   # PP-OCRv3 用辞書
├── pnnx_english.onnx  # 旧モデル
└── dict.txt           # 旧モデル用辞書
```

---

*本ドキュメントは2026-04-24時点の実装に基づく。環境変更に伴う更新が必要な場合はPRを作成のこと。*
