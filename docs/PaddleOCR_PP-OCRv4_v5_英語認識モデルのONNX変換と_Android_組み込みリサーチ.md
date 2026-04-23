# PaddleOCR PP-OCRv4/v5 英語認識モデルの ONNX 変換と Android 組み込みリサーチ

## 概要

本レポートでは、PaddleOCR の PP-OCRv4/v5 英語認識モデル（特に en_PP-OCRv4_mobile_rec / en_PP-OCRv5_mobile_rec）を ONNX に変換し、ONNX Runtime（ORT）経由で利用する際の既知事例・公式ドキュメントの範囲・注意点を整理する。特に、PP-OCRv4/v5 を Android の ORT 上で動かした成功事例の有無、paddle2onnx の推奨バージョン組み合わせ、PP-OCRv3 ONNX 版で出力が全 blank になる事象の原因候補、辞書とクラス数の差異、ONNX 形式での公式配布状況などを扱う。あわせて、既に動作している pnnx ベースの英語モデルとの違いを踏まえ、実務的なワークアラウンドも提案する。[^1][^2][^3][^4][^5][^6][^7]

## 1. PP-OCRv4/v5 英語 rec モデルの ONNX + ORT 成功事例

### 1.1 RapidOCR による v4/v5_mobile_rec の ONNX 化

RapidOCR プロジェクトは PaddleOCR の v3/v4/v5 系モデルを ONNX に変換し、ONNX Runtime で推論するクロスプラットフォーム OCR ライブラリである。RapidOCR チームのブログでは、PaddleX + paddle2onnx を用いて PP-OCRv5_mobile_rec を opset 14 で ONNX 変換し、RapidOCR から ORT backend で推論して PP-OCRv4_mobile_rec / PP-OCRv5_mobile_rec の精度比較を行っている。ここでは以下のような環境で v5_mobile_rec → ONNX 変換が成功し、ONNX Runtime で v4/v5 の両方が正常に動作している。[^7][^8]

- PaddlePaddle 3.0.0
- paddle2onnx 2.0.2.rc1
- paddlex 3.0.0
- onnx 1.16.0
- opset_version 14（paddlex --paddle2onnx の内部から呼び出し）[^7]

RapidOCR 側の推論結果では、PP-OCRv5_mobile_rec の Paddle 版と ONNX 版で精度がほぼ一致し、PP-OCRv4_mobile_rec / PP-OCRv4_server_rec も ONNX Runtime で良好に動作している。このことから、PP-OCRv4/v5_rec 系が ONNX + ORT で問題なく動作する実績は少なくともデスクトップ CPU 環境では確認されている。[^7]

### 1.2 Android での ONNX Runtime 利用事例

PaddleOCR 公式の「On-Device Deployment Demo」は PP-OCRv3/v4/v5 のモバイル版を Android 上で動かす手順を提供しているが、これは Paddle Lite / PaddleX-Lite ベースの C++ デモであり、ONNX Runtime Android を前提としていない。一方、中国語ブログでは ch_PP-OCRv2_rec_infer.onnx を onnxruntime-android 1.6.0 と組み合わせて Android アプリに組み込む手順を紹介しており、Java/Kotlin から ORT を直接呼び出すパターンが一般的であることが分かる。ただしこの記事で使われているのは v2 系の onnx モデルであり、PP-OCRv4/v5 に特化した Android + ONNX Runtime の公式サンプルではない。[^3][^9]

Web ブラウザ向けには、PP-OCRv4 の ONNX 版を ONNX Runtime Web で実行するチュートリアルが公開されており、ブラウザ内で ch_PP-OCRv4_det.onnx / ch_PP-OCRv4_rec.onnx を読み込んで OCR を行う実装例が示されている。これは Android ではないものの、同じ ORT ファミリ上で PP-OCRv4 を ONNX として動かす実例である。[^10]

Android + ORT + PP-OCRv4/v5_rec を明示的に扱った OSS リポジトリは調査範囲では見当たらず、既存のデスクトップ／Web 向け ONNX 実装（RapidOCR や HoVDuc/ppocrv5-onnx など）を踏まえて、開発者自身が onnxruntime-android に載せ替えるのが現状現実的なルートと考えられる。[^11][^7]

## 2. paddle2onnx の「テスト済み PaddleOCR v4/v5 組み合わせ」について

### 2.1 PaddleOCR 側の paddle2onnx ドキュメント

PaddleOCR リポジトリには `deploy/paddle2onnx/readme.md` と、バージョン 2.x 系のレガシー paddle2onnx ドキュメントが用意されている。これらは PP-OCRv3/PP-OCRv4 を含む各種モデルを opset_version 11 で ONNX 変換し、ONNX Runtime (Python) で推論する手順を説明しているが、以下の点が特徴である。[^5][^6]

- 推奨 opset は 11（文中では 9〜11 が「安定」と記載）[^5]
- PP-OCRv3 / en_PP-OCRv4_mobile_rec の具体的な変換コマンド例はあるが、PaddlePaddle 本体や paddle2onnx のバージョン番号は明示されていない[^6]
- 「テスト済み組み合わせ表（PaddlePaddle バージョン × paddle2onnx バージョン × opset_version）」のようなマトリクスは公開されていない[^6][^5]

Paddle2ONNX の model_zoo でも OCR カテゴリについては「Chinese and English ultra-lightweight OCR model (9.4M)」「Chinese and English general OCR model (143.4M)」といった抽象的な記述に留まり、PP-OCRv4/v5 に対する個別のテスト状況は示されていない。[^12]

### 2.2 実務で確認された組み合わせ

前述の RapidOCR v5 ブログでは、PaddlePaddle 3.0.0 + paddle2onnx 2.0.2.rc1 + opset 14 で PP-OCRv5_mobile_rec を変換し、ONNX Runtime 1.16.0 で推論できている。これは公式マトリクスではないものの、少なくとも v5 モデルに対する実運用上の成功事例として参考になる。[^7]

同様に、ブラウザ向けチュートリアルでは PP-OCRv4 の ONNX 版を ORT Web で問題なく動かしており、v4 系モデルが ONNX に変換しても挙動が保たれることを裏付けている。[^10]

## 3. PP-OCRv3 ONNX 版が全 blank になる事象の既知情報

### 3.1 公式に「既知バグ」としては扱われていない

PaddleOCR / Paddle2ONNX の issue や discussion では、PP-OCRv3_rec / SVTR 系モデルを ONNX 化した後に結果が不正になる事例は散発的に報告されているが、多くは「環境やコードを最新にして公式の変換・推論スクリプトをそのまま実行すると正常に動く」という回答で締められており、「PP-OCRv3_rec の ONNX 変換版は必ず blank になる」というような既知バグとしては整理されていない。[^13][^14]

SVTR ベースのモデルを ONNX 化した後、独自コードで推論した際に空文字列になる報告に対しては、開発者側が最新版の PaddleOCR / paddle2onnx / onnxruntime を用いて `tools/infer/predict_rec.py --use_onnx=True` で検証したところ正常に文字列が得られた、と回答している。このことから、[^13]

- 公式ツールチェーン（export_model.py → paddle2onnx → predict_rec.py --use_onnx=True）では PP-OCRv3_rec ONNX は基本的に正常動作する
- 空文字列になるケースは、独自の前処理・後処理や dict 設定、軸の扱いなどがズレている可能性が高い

という傾向が読み取れる。[^13]

### 3.2 blank 連発を引き起こしやすい要因

PP-OCRv3/v4 の英語モデルは、SVTR_LCNet + MultiHead（CTCHead + NRTRHead）構成であり、PostProcess 部分は CTCLabelDecode を用いる。以下のような要因があると、softmax の最大値が常に blank クラスに偏り、結果として全タイムステップ blank となりやすい。[^15]

- `character_dict_path` と実際の学習時の辞書が一致していない（文字数や順序がズレている）[^16][^17]
- `use_space_char` の設定が推論コードと学習時で一致していない（空白文字を含めるかどうか）[^18][^15]
- 画像のリサイズ・正規化・チャネル順が、学習時の `rec_image_shape` および `DecodeImage`/`RecResizeImg` のパイプラインと異なる[^17][^15]
- ONNX 版の出力テンソル形状（通常は [batch, seq_len, num_classes]）を誤って転置・reshape してから CTC デコードしている

特に、他者事例では Japan モデルを RKNN に変換する際、文字辞書の内容が学習時のクラス構成と異なっていたために blank / space / dummy 用のエントリを追加して修正している。このように、dict の行数とモデルが内部で想定しているクラス構成が一致していないと、出力が意味をなさない、あるいは blank に偏ることがある。[^16]

## 4. 公式モデルを ONNX 変換する際の落とし穴・参考資料

### 4.1 PaddleOCR 公式 paddle2onnx ガイド

PaddleOCR リポジトリの `deploy/paddle2onnx/readme.md` と Web ドキュメントの paddle2onnx ページは、PP-OCRv3/PP-OCRv4 系モデルの ONNX 変換と ONNXRuntime での推論手順を説明している。ここで明示的に述べられている注意点は以下の通りである。[^5][^6]

- OCR モデルの変換は必ず **dynamic shape** で行うこと（そうでないと Paddle 直実行と結果が少し異なる）[^6][^5]
- 一部の認識モデル（NRTR, SAR, RARE, SRN）は ONNX 変換未対応[^5]
- v1.2.3 以降の paddle2onnx では dynamic shape がデフォルトであり、`--input_shape_dict` は deprecated で、形状変更が必要な場合は `paddle2onnx.optimize` を使う[^6][^5]

これらは v3/v4 系全般に共通するため、en_PP-OCRv4_mobile_rec / en_PP-OCRv5_mobile_rec を扱う場合も同様の制約がかかると考えられる。

### 4.2 実務系ブログ・記事

PP-OCRv4 モデルを ONNX へ変換する際のポイントをまとめた中国語ブログでは、環境構築・動的入力最適化・ポリグラフィによる定数折り畳みなど、paddle2onnx を利用する際の実践的なノウハウが紹介されている。また、RapidOCR チームの v5 ブログも、PaddleX を介して v5_mobile_rec を ONNX 化し、ONNXRuntime での精度検証まで行っており、「変換前後で前後処理コードを共用できる」「変換後も精度はほぼ一致し、速度だけ向上した」といった知見を示している。[^19][^7]

これらの記事は公式ドキュメントではないが、PP-OCRv4/v5 系モデルの ONNX 実運用における落とし穴（環境バージョン、dynamic shape、定数折り畳み、dict 埋め込みなど）を把握するのに有用である。[^19][^7]

## 5. PaddleOCR 公式の "Paddle2ONNX → ONNX Runtime Android" 手順の有無

PaddleOCR 公式ドキュメントは、

- `deploy/paddle2onnx` で **Paddle → ONNX → ONNXRuntime (Python)** の流れ[^5][^6]
- `OCR On-Device Deployment Demo` で **PP-OCRv3/v4/v5 モバイルモデルを Android Shell 上で動かす PaddleX-Lite ベースの C++ デモ**[^3]

をそれぞれ解説しているが、「Paddle2ONNX で変換した ONNX を onnxruntime-android ライブラリから読み込む Android Java/Kotlin 向け公式チュートリアル」は公開されていない。[^3][^6][^5]

Android で PP-OCR モデルを動かしたい場合、公式としては PaddleX-Lite / Paddle Lite を使う on-device デプロイを推奨しており、ONNX Runtime Android はサードパーティ的な選択肢に留まっている。従って、"Paddle2ONNX → ONNX Runtime Android" を完全にカバーする公式ドキュメントは現時点では存在しないと考えられる。[^9][^3]

## 6. en_PP-OCRv4_rec のクラス数 97 と en_dict.txt 95 文字の差の理由

### 6.1 en_PP-OCRv4_rec の設定

`en_PP-OCRv4_rec.yml` では以下の設定が確認できる。[^15]

- `character_dict_path: ppocr/utils/en_dict.txt`
- `use_space_char: true`
- PostProcess: `CTCLabelDecode`

PaddleOCR の CTCLabelDecode は、一般に以下のようなロジックでクラス数を決める実装になっている。

1. `character_dict_path` で読み込んだ文字集合を `character` リストとする
2. `use_space_char: true` の場合、`character` の末尾にスペース文字（" ") を追加する[^18]
3. CTC の blank クラスを 1 つ追加し、最終的な `num_classes = len(character) + 1` となる（blank は通常 ID 0、文字は 1 以降など）[^20]

仮に en_dict.txt に 95 文字が列挙されているとすると、

- dict 文字数: 95
- use_space_char によるスペース追加: +1 → 96
- CTC blank: +1 → 97

となり、ONNX モデル上の出力クラス数 97 と整合する。これと類似の例として、日本語モデルの辞書を他フレームワーク向けに調整する際に、先頭に `blank`、末尾に `__space__` や `@dummy` を追加してクラス構成を一致させている事例がある。[^16]

### 6.2 実装上の注意

このため、ONNX 側で CTC デコードを実装する場合は、

- `num_classes = dict_len + (use_space_char ? 1 : 0) + 1(blank)` となる前提でラベルインデックスを解釈する
- 文字リストは `[dict, dict[^1], ..., dict[n-1], ' ']`（use_space_char=true の場合）に対し、blank を 0 番、文字を 1..N 番のように割り当てる

といった PaddleOCR 側の仕様を忠実に再現する必要がある。これが崩れると、出力クラスと辞書の対応がズレ、無意味な文字列や blank 連発を引き起こす可能性がある。[^17][^15][^18][^16]

## 7. PP-OCRv4/v5 モデルの ONNX 形式での公式配布状況

### 7.1 PaddleOCR 公式配布

PaddleOCR 公式が配布しているのは、基本的に **Paddle Inference 用の静的グラフモデル（inference.pdmodel / inference.pdiparams）** や PaddleX で利用される形式であり、ONNX 形式のモデルは配布していない。PP-OCRv4_mobile_rec / en_PP-OCRv5_mobile_rec などのモデルは、PaddleOCR のモデル一覧や PaddleX のテキスト認識モジュールのドキュメントからダウンロード可能だが、いずれも Paddle 形式である。[^21][^22]

Hugging Face 上の `PaddlePaddle/PP-OCRv4_mobile_rec` などのリポジトリも、モデルカードを見る限り Paddle モデルの配布が中心であり、ONNX 形式は明示されていない。[^23][^24]

### 7.2 サードパーティによる ONNX 配布

一方で、PP-OCRv4/v5 系の ONNX モデルはサードパーティから多数配布されている。

- RapidOCR 関連の Hugging Face モデルや `monkt/paddleocr-onnx` など、PP-OCRv3/v4 ベースの rec.onnx / det.onnx を配布しているコレクション[^25][^26]
- `webnn/PP-OCRv4-ONNX` など、PP-OCRv4 を ONNX として WebNN/ORT Web で動かすために公開されたモデル[^27]
- `onnxocr-ppocrv5`（PyPI パッケージ）や `HoVDuc/ppocrv5-onnx` リポジトリなど、PP-OCRv5 系を ONNX + ORT で動かすためのパイプライン[^28][^11]

これらは PaddleOCR 公式ではなく、各プロジェクトの作者が Paddle モデルを paddle2onnx や独自ツールで変換したものである。したがって、「ONNX Hub 等で公式に配布されている PP-OCRv4/v5 ONNX モデル」は現状存在せず、必要であれば公式 Paddle モデルから自前で ONNX を生成するか、第三者配布モデルを信頼性を確認しつつ利用する形になる。[^8][^28]

## 8. 実務的な示唆とワークアラウンド

1. PP-OCRv4/v5 英語 rec モデル自体は、RapidOCR や `ppocrv5-onnx` の事例から ONNX + ORT で問題なく動作しうることが確認されているため、pnnx_english の代替として採用する余地は十分ある。[^11][^7]
2. PaddlePaddle 3.3.1 + paddle2onnx 2.1.0 で PP-OCRv4_rec 変換時に Concat の shape inference error が出ている場合、RapidOCR ブログと同等の組み合わせ（PaddlePaddle 3.0.0 + paddle2onnx 2.0.2.rc1 + opset 14）や、paddlex 経由の変換を試すことで状況が改善する可能性がある。[^7][^5]
3. PP-OCRv3_rec ONNX 版が全 blank になる場合、まずは PaddleOCR 公式の `tools/infer/predict_rec.py --use_onnx=True` で同じ ONNX を検証し、そこで正しく文字列が出るかを確認すると原因切り分けが容易になる

---

## References

1. [2026-04-24_PaddleOCR_v4-v5_ONNXBian-Huan-_AndroidZu-miIp-mirisati.md](https://ppl-ai-file-upload.s3.amazonaws.com/web/direct-files/attachments/66541474/c6013bde-18f9-4441-9046-4579714d7d39/2026-04-24_PaddleOCR_v4-v5_ONNXBian-Huan-_AndroidZu-miIp-mirisati.md?AWSAccessKeyId=ASIA2F3EMEYES4L36IPW&Signature=ov8LixXLrZCopM2EM%2BRTNzdf2B4%3D&x-amz-security-token=IQoJb3JpZ2luX2VjEKX%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMSJHMEUCIQDeGw5VyVV52Q8FwJKxgwI%2FcQGqwT5xMLCa3%2B6nM0ZmjwIgDuL2dWdZjSO0o%2BK0k61CrkLb0WAFQxFjyJ2OJsyfzAEq8wQIbhABGgw2OTk3NTMzMDk3MDUiDBboyUGSO1uOnxhC9CrQBF3hCYWNKn23LYp%2F8Vk%2FFcS2kxNCxfGaQod%2FVHltGJnIOnoDQuoycYuKnHI8D4qOFANsKEJwO9VxTYyZA24ZMJ7na1Mgtd4jsHv9IlsK0qGtStd0FGKAYHy0Df8OgTZwsMfQmIPMSNbLFJEfYMneR0uM%2B5y%2FDUWocv1kdRc4X4fOWskeHN7TDMY%2BqXkee45dVMXIqPE1kzC0UQrGAA9DEa8CmHnRbr3DeAvnQDIqGAQeL%2B8qP7KhGPxsnkK3IP5WIeKyQvtJiFd40xizkstRV6Xe9SOrEJPaslcY8Ihdo7UEbXKMcc3xsAEL73cev6KdeT%2BqO2aj0fuMVI4vey23cC0I4Losj1PF6IiwtN5w5HGUrCgb8fELKKQQpI351cOi6YQojYJulwY%2F6dSNfeaBkNIfpD3aCN77rTP5cKapf6WQTpXCmFvNstQBr7ov5HwXrt8uCKeNg%2FQCzMIAANXEzTuf%2Bj0AUAbxumKqM1hkbTWw5QHWhn%2BEmbx2NSk2s8zVNl8um6dhE%2FeOzYIDb283qJAT6l65EHTQmHdwZCXFOzgf3ufypHZCSf0zbSMimwOe5s4V3pK4YGm847KceQReWoI9QrHtArQAH2wRb%2Bv%2F8F6Avpuavw64XGVm%2FJF9PseeARGtdDkHx3%2B8BCMEAu0Sn6tE8aeEzR2XepX2xrCzED%2Fb1znsAyH5H3LcALHVUP6y1CZ1ry%2B%2Fr9nKUYy32OfJcilfDySAh0r7Hhq0rtSTqqjeqJ3aDfKIz4PMXU9IMCBcYb1Mf4ozvpfQAlAwoVZeMrkwjY6qzwY6mAG%2B20jywtWPzK7TffrfbRuj6pReD%2B9h1SdVPlizdjA%2BsnGm4dWqH1IQ6OsGdXpnBMHr9GcnQAZAfsWpEGFt4REL%2FnHyxQ3pi5nZu%2FRDGNjdNbFB%2FxFWN47XBv3gvKZYsJQpmKe4QkW%2F3%2BZ3tHVElFF2cHuvq%2FWPVxfU%2BX8MTFiyqDevszX4H%2FexRtStNFx8zHx17mZmqjbO%2BQ%3D%3D&Expires=1776981216) - # PaddleOCR v4/v5 ONNX変換・Android組み込みリサーチ依頼

**作成日:** 2026-04-24

---

## 1. 環境情報

### OS / ツール
- **開...

2. [GitHub - PaddlePaddle/PaddleOCR: Turn any PDF or image ...](https://github.com/PADDLEPADDLE/PADDLEOCR) - Turn any PDF or image document into structured data for your AI. A powerful, lightweight OCR toolkit...

3. [OCR On-Device Deployment Demo Usage Guide](https://paddlepaddle.github.io/PaddleOCR/main/en/version3.x/deployment/on_device_deployment.html) - This guide mainly introduces how to run the PaddleX on-device deployment demo for OCR text recogniti...

4. [RapidOCR集成PP-OCRv5_rec_mobile模型记录- Danno - 博客园](https://www.cnblogs.com/shiwanghualuo/p/18905337) - 该文章主要记录RapidOCR集成PP-OCRv5_mobile_rec和PP-OCRv5_server_rec模型记录的，涉及模型转换，模型精度测试等步骤。 详细完整记录请前往官方博客 ...

5. [PaddleOCR/deploy/paddle2onnx/readme.md at main - GitHub](https://github.com/PaddlePaddle/PaddleOCR/blob/main/deploy/paddle2onnx/readme.md) - This chapter describes how the PaddleOCR model is converted into an ONNX model and predicted based o...

6. [Paddle2ONNX - PaddleOCR Documentation](https://paddlepaddle.github.io/PaddleOCR/main/en/version2.x/legacy/paddle2onnx.html) - Paddle2ONNX supports converting models in the PaddlePaddle format to the ONNX format. Operators curr...

7. [PaddleOCR v5をonnx化する - Zenn](https://zenn.dev/mokuichi147/scraps/cf00bff624dcbb) - 次に paddleocr をインストールし、 paddlex --install paddle2onnx コマンドを実行する。 これにより以下のコマンドで変換できるらしい。 paddlex ...

8. [Successful Integration of PPOCR v4/v5 with DEEPX's On-Device ...](https://github.com/PaddlePaddle/PaddleOCR/discussions/16523) - We wanted to share our successful integration of PPOCR v4/v5 models with DEEPX's DX-M1 for on-device...

9. [PaddleOCR系列-训练模型并部署android手机 - 稀土掘金](https://juejin.cn/post/7129780261394317325) - 2. ocr模型部署安卓手机 · 2.1 AndroidStudio 2021.2.1或以上； · 2.2. NDK下载，在SDK Tools中下载，版本选最新版； · 2.3. cmake 3.4....

10. [Build a Browser-Based Passport MRZ Scanner with OCR and Face ...](https://www.dynamsoft.com/codepool/build-javascript-passport-scanner-mrz-ocr-face-detection.html) - PaddleOCR (PP-OCRv4) runs entirely in the browser via ONNX Runtime Web — no API key or external serv...

11. [HoVDuc/ppocrv5-onnx - GitHub](https://github.com/HoVDuc/ppocrv5-onnx) - PP-OCRv5 ONNX (uv-based workflow). This repo runs PaddleOCR v5 (detection + recognition) exported to...

12. [Paddle2ONNX/docs/en/model_zoo.md at develop - GitHub](https://github.com/PaddlePaddle/Paddle2ONNX/blob/develop/docs/en/model_zoo.md) - Paddle2ONNX supports converting PaddlePaddle model to ONNX format. Due to the differences between fr...

13. [SVTR 轉 onnx 後，inference 的結果變成空字串· Issue #7821 - GitHub](https://github.com/PaddlePaddle/PaddleOCR/issues/7821) - 如果你不确定出现了什么问题，建议新建环境，下载最新的PaddleOCR代码和模型，根据上面我的测试流程尝试是否可以跑通，如果能跑通，可能是因为你修改了某些关键 ...

14. [基于变长SVTR的文字识别模型及ONNX部署 - 飞桨AI Studio](https://aistudio.baidu.com/projectdetail/4436515) - PP-OCRv3是百度开源的超轻量级场景文本检测识别模型库，其中超轻量的场景中文识别模型SVTR_LCNet使用SVTR作为基础结构，进行精度优化和轻量化。 详见PP- ...

15. [PaddlePaddle/PaddleOCR - Gitee](https://gitee.com/paddlepaddle/PaddleOCR/blob/dygraph/configs/rec/PP-OCRv4/en_PP-OCRv4_rec.yml) - PP-OCRv4 / en_PP-OCRv4_rec.yml. 123456789101112131415161718192021222324252627 ... use_space_char: tr...

16. [ppocr command - github.com/maxwelljun/go-rknnlite/example/ppocr](https://pkg.go.dev/github.com/maxwelljun/go-rknnlite/example/ppocr) - Add the word blank at the top of the on line 1. Scroll to end of file and replace the last line whic...

17. [PaddleOCR api call results on inference model don't make any sense](https://github.com/PaddlePaddle/PaddleOCR/issues/14286) - The rec_image_shape parameter in your configuration file (e.g., en_PP-OCRv3_rec.yml ) should match t...

18. [Configuration - PaddlePaddle/PaddleOCR](https://gitee.com/paddlepaddle/PaddleOCR/blob/release/2.4/doc/doc_en/config_en.md) - 25, \. use_space_char, Set whether to recognize spaces, True, |. label_list, Set the ... CTCLabelDec...

19. [PP-OCRv4模型ONNX导出实战：从环境配置到动态输入优化的完整避 ...](https://blog.csdn.net/gold/article/details/151694864) - 如果你正在尝试将PP-OCRv4部署到边缘设备或嵌入式平台，那么将PaddlePaddle模型转换为ONNX格式几乎是必经之路。但这个过程远不止运行 ...

20. [基于CRNN的文本字符交易验证码识别--Paddle实战 - 阿里云开发者社区](https://developer.aliyun.com/article/1053621) - CTC是一种Loss计算方法，用CTC代替Softmax Loss，训练样本无需对齐。引入blank字符，解决有些位置没有字符的问题,通过递推，快速计算梯度。

21. [General OCR Pipeline Usage Tutorial](https://paddlepaddle.github.io/PaddleOCR/main/en/version3.x/pipeline_usage/OCR.html) - This pipeline supports the use of PP-OCRv3, PP-OCRv4, and PP-OCRv5 models, with the default model be...

22. [Text Recognition - PaddleX Documentation - GitHub Pages](https://paddlepaddle.github.io/PaddleX/3.2/en/module_usage/tutorials/ocr_modules/text_recognition.html) - The lightweight recognition model of PP-OCRv4 has high inference efficiency and can be deployed on v...

23. [PaddlePaddle/PP-OCRv4_mobile_rec - Hugging Face](https://huggingface.co/PaddlePaddle/PP-OCRv4_mobile_rec) - A lightweight recognition model of PP-OCRv4 with high inference efficiency, suitable for deployment ...

24. [PaddlePaddle/PP-OCRv4_mobile_det - Hugging Face](https://huggingface.co/PaddlePaddle/PP-OCRv4_mobile_det) - ... PP-OCRv4_mobile_rec \ --use_doc_orientation_classify False \ --use_doc_unwarping False \ --use_t...

25. [monkt/paddleocr-onnx - Hugging Face](https://huggingface.co/monkt/paddleocr-onnx) - Multilingual OCR models from PaddleOCR, converted to ONNX format for production deployment. Use as a...

26. [PP-OCRv4/en_PP-OCRv3_det_infer.onnx · SWHL/RapidOCR at main](https://huggingface.co/SWHL/RapidOCR/blob/main/PP-OCRv4/en_PP-OCRv3_det_infer.onnx) - Model card Files Files and versions. xet · Community. main. RapidOCR / PP-OCRv4 / ... Upload en_PP-O...

27. [webnn/PP-OCRv4-ONNX · Hugging Face](https://huggingface.co/webnn/PP-OCRv4-ONNX) - PP-OCRv4-ONNX. like 0. Follow. Web Neural Network 8. ONNX. License: apache-2.0. Model card Files Fil...

28. [onnxocr-ppocrv5 - PyPI](https://pypi.org/project/onnxocr-ppocrv5/) - ONNX-based OCR (PP-OCRv5) inference pipeline ... onnx" "models/ppocrv5/rec/rec.onnx". pip install on...

