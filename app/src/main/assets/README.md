# ONNX Model Deployment Guide

このフォルダには、PaddleOCRのONNXモデルファイルを配置します。

## 📦 必要なモデルファイル

### 1. PaddleOCR English Recognition Model (rec.onnx)

- **パス**: `app/src/main/assets/onnx/rec.onnx`
- **形式**: ONNX（PaddleOCR より出力済み）
- **入力名**: `x`
- **出力名**: `softmax_0.tmp_0`
- **入力形状**: `[1, 3, 48, 320]` (バッチ, RGB, 高さ48, 幅320)
- **出力形状**: `[1, seq_len, vocab_size]`
- **文字辞書**: `onnx/dict.txt` (同一フォルダ)

### 2. Character Dictionary (dict.txt)

- **パス**: `app/src/main/assets/onnx/dict.txt`
- **形式**: 1行1文字（インデックス順）
- **サイズ**: 約800文字（PaddleOCR英語モデル用）

## 🔧 モデル取得方法

### オプションA: 事前変換済みモデルをダウンロード（推奨）

#### Hugging Face（公式・推奨）
```bash
# PaddleOCR English PP-OCRv3 (ONNX)
wget https://huggingface.co/constanceastropaddle/paddleocr-english-onnx/resolve/main/rec.onnx
wget https://huggingface.co/constanceastropaddle/paddleocr-english-onnx/resolve/main/dict.txt

# 配置:
# app/src/main/assets/onnx/rec.onnx
# app/src/main/assets/onnx/dict.txt
```

#### GitHub Releases（代替）
```bash
# PaddleOCR 公式リポジトリのONNX変換済みモデル
wget https://github.com/PaddlePaddle/PaddleOCR/releases/download/v2.7.0/rec_inference_onnx.tar.gz

tar -xzf rec_inference_onnx.tar.gz
# 中身: rec.onnx, dict.txt

# 配置:
mkdir -p app/src/main/assets/onnx
mv rec.onnx app/src/main/assets/onnx/
mv dict.txt app/src/main/assets/onnx/
```

### オプションB: PaddleOCR から手動でONNX変換

PaddleOCR リポジトリをクローンして変換:

```bash
git clone https://github.com/PaddlePaddle/PaddleOCR.git
cd PaddleOCR

# 環境セットアップ（Python + PaddlePaddle + paddle2onnx）
pip install paddlepaddle paddle2onnx onnx

# 英語モデルをONNX変換
python tools/export_model.py \
    -c ppocr/rec/ch_ppocr_v3_teacher/config.yml \
    -o Global.checkpoints="path/to/ppocr_v3_teacher.pdparams" \
    -o Global.save_inference_dir="./inference/rec_onnx" \
    -o Global.pretrained_model="" \
    -o Global.use_onnx=True

# 出力:
# inference/rec_onnx/inference.onnx (rec.onnx)
# inference/rec_onnx/ppocr_keys_v3.txt (dict.txt)

# コピー
cp inference/rec_onnx/inference.onnx app/src/main/assets/onnx/rec.onnx
cp inference/rec_onnx/ppocr_keys_v3.txt app/src/main/assets/onnx/dict.txt
```

詳細: [PaddleOCR公式ドキュメント](https://github.com/PaddlePaddle/PaddleOCR/blob/main/doc/doc_en/export_en.md)

## 📖 使用方法

### 1. モデル配置の完了確認

`app/src/main/assets/onnx/` ディレクトリに以下のファイルがあること:
- ✅ `rec.onnx` (約7-10MB)
- ✅ `dict.txt` (約1-2KB)

### 2. アプリケーションのビルド

```bash
./gradlew assembleDebug
```

### 3. 実機テスト

1. アプリをインストール
2. カメラで製品コード（例: ABC-12345）を撮影
3. ログで確認:
   ```
   D/OnnxOcrEngine: Decoded text: 'ABC-12345'
   D/OnnxOcrEngine: Product code extracted: 'ABC-12345'
   ```

## 🐛 トラブルシューティング

### 問題: "Model file not found"
**原因**: `rec.onnx` が `assets/onnx/` に配置されていない
**解決**: 上記の取得方法でモデルをダウンロードし、正確なパスに配置

### 問題: "Invalid output shape"
**原因**: モデルとコードの期待する入出力形状が不一致
**解決**: ログ出力 `Input shape:` / `Output shape:` を確認し、`OnnxOcrEngine.kt` の`INPUT_NAME`, `OUTPUT_NAME`, `INPUT_WIDTH`, `INPUT_HEIGHT`を調整

### 問題: "Token ID out of range"
**原因**: `dict.txt` の内容がモデルの出力 Vocabulary と不一致
**解決**: モデルと同一ソースから取得した `dict.txt` を使用

## 🔍 現在の実装状態

- ✅ ONNX Runtime 依存関係追加 (`ai.onnxruntime:onnxruntime-android:1.16.3`)
- ✅ `OnnxOcrEngine.kt` 本番実装完了
- ✅ 文字列デコード (argmax + dict lookup)
- ✅ 製品コード抽出 (正規表現 `[A-Z0-9]{3,}-?[0-9]*`)
- ✅ 例外処理と詳細ログ
- ⏳ 本番モデル配置待ち（`rec.onnx` + `dict.txt`）

## 📚 参照リンク

- [ONNX Runtime Android 公式ドキュメント](https://onnxruntime.ai/docs/get-started/with-java/android.html)
- [PaddleOCR 公式リポジトリ](https://github.com/PaddlePaddle/PaddleOCR)
- [PaddleOCR ONNX エクスポート手順](https://github.com/PaddlePaddle/PaddleOCR/blob/main/doc/doc_en/export_en.md)
- [Hugging Face PaddleOCR Models](https://huggingface.co/models?search=paddleocr)

---

**最終更新**: 2026-04-23
**作成者**: CEO Agent (OpenClaw)
