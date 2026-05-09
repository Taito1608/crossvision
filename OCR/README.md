# OCR 実験ディレクトリ

PP-OCRv3 ONNX モデルを使った製品コード認識の実験環境。

## ディレクトリ構成

```
OCR/
├── README.md           # このファイル
├── models/             # ONNXモデル・辞書
│   ├── det.onnx        # テキスト検出モデル (DB)
│   ├── en_PP-OCRv3_rec.onnx  # 認識モデル (PP-OCRv3 English, 8.5MB)
│   ├── en_PP-OCRv3_dict.txt  # 辞書 (95文字)
│   ├── ppocrv3_rec.onnx      # RapidOCR版認識モデル
│   ├── ppocrv3_dict.txt      # RapidOCR辞書
│   ├── pnnx_english.onnx     # PNNX変換モデル
│   └── dict.txt              # 旧辞書
├── images/             # 評価用画像14枚 + テスト画像
│   ├── IMG_0787.jpg           # 黄色, ノイズなし, 明暗
│   ├── M5sb24-10.jpg          # 水色, さび
│   ├── IMG_1537.jpg           # 水色, 垂直
│   ├── IMG_1548.jpg           # 水色, さび+ピントぼけ
│   ├── IMG_1571.jpg           # 水色
│   ├── IMG_0422.jpg           # 黄色+水色混合, 明暗
│   ├── IMG20250416153020.jpg  # 黄色, 斜め, 明暗
│   ├── multi_008_yellow_green.JPG  # 黄色+水色混合, 複数の向き
│   ├── N5G15-X1Y1.jpg         # 水色, さび
│   ├── multi_001_sunny_yellow.JPG  # 黄色, 複数の向き, 明暗
│   ├── 015_B15b30N-41__B15b30N-7A_45deg_NW_yellow.JPG  # 黄色, 45度
│   ├── IMG20250416160821.jpg  # 黄色(白背景), 少し斜め, 明暗混ざり
│   ├── IMG_1536.jpg           # 水色, 大きさ大小, 逆向き
│   └── IMG_0771.jpg           # 水色, 色々な大きさ
├── src/                # Androidソースコード
│   ├── OnnxOcrEngine.kt      # ONNX推論エンジン (det+rec)
│   ├── OnnxTestActivity.kt   # テスト用Activity
│   └── activity_onnx_test.xml # レイアウト
├── scripts/            # Python評価スクリプト
│   └── ocr_eval.py           # 精度評価メインスクリプト
└── results/            # 評価結果出力先
```

## 環境セットアップ

```bash
pip install numpy opencv-python onnxruntime
```

## 評価スクリプト実行

```bash
cd /mnt/c/Users/soyak/StudioProjects/crossvision/OCR
python3 scripts/ocr_eval.py
```

結果は `results/eval_results.json` に保存される。

## モデル情報

| モデル | ファイル | 備考 |
|--------|----------|------|
| 検出 (DB) | det.onnx | テキスト領域検出 |
| 認識 (PP-OCRv3 EN) | en_PP-OCRv3_rec.onnx | 公式English, 97クラス |
| 認識 (RapidOCR) | ppocrv3_rec.onnx | RapidOCR版 |

## 前回の評価結果 (2026-05-08)

| 指標 | 値 |
|------|-----|
| Precision | 0.0% |
| Recall | 0.0% |
| F1 Score | 0.0% |
| Partial Recall | 33.3% |
| 平均LCS類似度 | 49.2% |
| 平均処理時間 | 2.02s/image |

### 主なエラーパターン
- `1↔I`, `0↔O`, `N↔V` の形状類似文字混同
- ハイフン消失
- ピントぼけ画像でrecが完全失敗
- 黄色背景 > 水色背景で精度が良い

## 実験ログ

| 日付 | 内容 |
|------|------|
| 2026-05-08 | PP-OCRv3 English モデル評価 → 精度0% |
| 2026-05-09 | OCR実験ディレクトリ作成、ファイル整理 |
