#!/usr/bin/env python3
"""
PP-OCRv3 ONNX バッチ全パイプライン検証スクリプト v2
- 回転バウンディングボックス対応（傾きテキストの正確なクロップ）
- 使用可能文字フィルタリング
- 正解ラベルとの照合（完全一致・部分一致の計算）
- 結果を results/batch_full_pipeline2/ に出力
"""
import numpy as np
import cv2
import onnxruntime as ort
from pathlib import Path
import json
import time
import re
import sys
import csv

# === パス設定 ===
SCRIPT_DIR = Path(__file__).parent
OCR_DIR = SCRIPT_DIR.parent
MODELS_DIR = OCR_DIR / "models"
IMAGES_DIR = OCR_DIR / "images"
RESULTS_DIR = OCR_DIR / "results" / "batch_full_pipeline2"
RESULTS_DIR.mkdir(exist_ok=True)

DET_MODEL = MODELS_DIR / "det.onnx"
REC_MODEL = MODELS_DIR / "en_PP-OCRv3_rec.onnx"
DICT_FILE = MODELS_DIR / "en_PP-OCRv3_dict.txt"
LABEL_CSV = IMAGES_DIR / "正解ラベル.csv"

# 対象画像拡張子
IMG_EXTS = {".jpg", ".jpeg", ".png", ".JPG", ".JPEG", ".PNG"}

# === 使用可能文字セット ===
# 英大文字 A-Z, 英小文字 a-z, 数字 0-9, ハイフン-, スラッシュ/, プラス+, ピリオド., アンダースコア_, スペース
ALLOWED_CHARS = set(
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    "abcdefghijklmnopqrstuvwxyz"
    "0123456789"
    "-+/._ "
)


def filter_allowed(text):
    """使用可能文字のみを残す"""
    return "".join(c for c in text if c in ALLOWED_CHARS)


def load_char_dict(dict_path):
    chars = []
    with open(dict_path, 'r', encoding='utf-8') as f:
        for line in f:
            chars.append(line.strip())
    return chars


def normalize_filename_for_matching(name):
    """
    CSVのファイル名と画像ファイル名の不一致を吸収する。
    - ディレクトリプレフィックス (bundle/ 等) を除去
    - 先頭の余分な文字 (b015_ → 015_, II → I) を修正
    - 拡張子がない場合は .jpg を補完
    - 大文字小文字を統一
    """
    name = name.strip().replace('\n', '').replace('\r', '')
    # ディレクトリプレフィックス除去
    name = Path(name).name
    # 拡張子がない場合は .jpg 補完
    if not Path(name).suffix:
        name += ".jpg"
    # 先頭の余分な文字を修正
    # b015_ → 015_ (b + 数字で始まる場合)
    if name.startswith('b') and len(name) > 1 and name[1].isdigit():
        name = name[1:]
    # II → I (IIMG → IMG)
    if name.startswith('II') and not name.startswith('III'):
        name = name[1:]
    return name.lower()


def load_labels(csv_path, img_files):
    """正解ラベルCSVを読み込む。画像ファイル名→正解コードリストの辞書を返す"""
    # 画像ファイル名の正規化マップ
    img_names = {f.name.lower(): f.name for f in img_files}

    labels = {}
    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        header = next(reader)  # skip header
        for row in reader:
            if len(row) < 8:
                continue
            filename_raw = row[1].strip()
            correct_codes = row[7].strip()
            codes = [c.strip() for c in correct_codes.split('\n') if c.strip()]
            if not codes:
                continue

            norm = normalize_filename_for_matching(filename_raw)
            # 完全一致
            if norm in img_names:
                labels[img_names[norm]] = codes
            else:
                # 部分一致: stem でマッチ
                stem = Path(norm).stem
                for lower_name, orig_name in img_names.items():
                    if Path(lower_name).stem == stem:
                        labels[orig_name] = codes
                        break
                else:
                    print(f"  WARNING: No matching image for CSV label '{filename_raw}' (normalized: '{norm}')")
    return labels


def det_preprocess(img):
    h, w = img.shape[:2]
    scale = min(960 / h, 960 / w)
    nh = int(h * scale) // 32 * 32
    nw = int(w * scale) // 32 * 32
    if nh == 0: nh = 32
    if nw == 0: nw = 32
    resized = cv2.resize(img, (nw, nh))
    blob = cv2.dnn.blobFromImage(resized, 1.0, (nw, nh),
                                  (123.675, 116.28, 103.53), swapRB=True)
    mean = np.array([0.485, 0.456, 0.406]).reshape(1, 3, 1, 1)
    std = np.array([0.229, 0.224, 0.225]).reshape(1, 3, 1, 1)
    blob = (blob / 255.0 - mean) / std
    return blob.astype(np.float32), scale, (h, w)


def det_postprocess(output, orig_hw, det_thresh=0.3):
    pred = output[0, 0]
    binary = pred > det_thresh
    binary_uint8 = (binary * 255).astype(np.uint8)
    contours, _ = cv2.findContours(binary_uint8, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)

    h, w = pred.shape
    orig_h, orig_w = orig_hw
    scale_x = orig_w / w
    scale_y = orig_h / h

    boxes = []
    angles = []
    scores = []
    for contour in contours:
        if len(contour) < 4:
            continue
        rect = cv2.minAreaRect(contour)
        box = cv2.boxPoints(rect)
        box_w = rect[1][0]
        box_h = rect[1][1]
        angle = rect[2]

        if box_w < 5 or box_h < 5:
            continue

        # スコア計算: その領域内の平均スコア
        mask = np.zeros((h, w), dtype=np.uint8)
        # contourは元画像座標系なので、予測特徴マップ座標系に変換
        contour_scaled = contour.copy()
        contour_scaled[:, :, 0] = np.clip(contour_scaled[:, :, 0] / scale_x, 0, w - 1).astype(np.int32)
        contour_scaled[:, :, 1] = np.clip(contour_scaled[:, :, 1] / scale_y, 0, h - 1).astype(np.int32)
        cv2.fillPoly(mask, [contour_scaled.astype(np.int32)], [255])
        score = float(np.mean(pred[mask > 0])) if np.sum(mask) > 0 else 0.0

        box[:, 0] *= scale_x
        box[:, 1] *= scale_y
        box = box.astype(np.int32)
        box[:, 0] = np.clip(box[:, 0], 0, orig_w - 1)
        box[:, 1] = np.clip(box[:, 1], 0, orig_h - 1)

        # BBoxを拡張（min/max座標ベースで30%パディングを追加）
        # 中心からの放射状拡大ではなく、縦横それぞれにパディングを加える
        x_min = np.min(box[:, 0])
        y_min = np.min(box[:, 1])
        x_max = np.max(box[:, 0])
        y_max = np.max(box[:, 1])
        box_w = x_max - x_min
        box_h = y_max - y_min
        pad_x = box_w * 0.3
        pad_y = box_h * 0.3

        # 各点をmin/max方向に拡張
        for j in range(4):
            if box[j, 0] < (x_min + x_max) / 2:
                box[j, 0] -= pad_x
            else:
                box[j, 0] += pad_x
            if box[j, 1] < (y_min + y_max) / 2:
                box[j, 1] -= pad_y
            else:
                box[j, 1] += pad_y

        box[:, 0] = np.clip(box[:, 0], 0, orig_w - 1)
        box[:, 1] = np.clip(box[:, 1], 0, orig_h - 1)
        boxes.append(box)
        angles.append(angle)
        scores.append(score)
    return boxes, angles, scores


def crop_rotated(img, box, angle, padding=4):
    """
    回転バウンディングボックスからテキスト領域を正確にクロップする。
    1. バウンディングボックスの最小外接矩形を計算
    2. 画像全体を回転させてテキストを水平にする
    3. 水平なテキスト領域をクロップ
    """
    # box: 4点の座標 (cv2.boxPoints の出力)
    # angle: minAreaRect の角度

    # バウンディングボックスの幅と高さを計算
    rect = cv2.minAreaRect(box.astype(np.int32))
    center = rect[0]
    w, h = rect[1]
    angle_deg = rect[2]

    if w < 1 or h < 1:
        return None

    # 幅と高さが逆になっている場合があるので修正
    # minAreaRect は w < h のとき角度が変わるため
    if w < h:
        w, h = h, w
        angle_deg = angle_deg + 90

    # パディング追加
    w_pad = int(w) + padding * 2
    h_pad = int(h) + padding * 2

    # 画像全体を回転
    M = cv2.getRotationMatrix2D(center, angle_deg, 1.0)
    rotated = cv2.warpAffine(img, M, (img.shape[1], img.shape[0]),
                             flags=cv2.INTER_CUBIC,
                             borderMode=cv2.BORDER_REPLICATE)

    # 回転後の中心からクロップ
    cx, cy = int(center[0]), int(center[1])
    x1 = max(0, cx - w_pad // 2)
    y1 = max(0, cy - h_pad // 2)
    x2 = min(img.shape[1], cx + w_pad // 2)
    y2 = min(img.shape[0], cy + h_pad // 2)

    if x2 <= x1 or y2 <= y1:
        return None

    crop = rotated[y1:y2, x1:x2]
    if crop.size == 0:
        return None
    return crop


def rec_preprocess(crop):
    h, w = crop.shape[:2]
    REC_H, REC_W = 48, 320
    ratio = w / h
    resized_w = int(ratio * REC_H)
    if resized_w > REC_W:
        resized_w = REC_W
    if resized_w < 1:
        resized_w = 1
    resized = cv2.resize(crop, (resized_w, REC_H))
    img = resized.astype(np.float32)
    img = (img - 127.5) / 127.5
    padded = np.full((REC_H, REC_W, 3), -1.0, dtype=np.float32)
    padded[:, :resized_w, :] = img
    blob = np.transpose(padded, (2, 0, 1))[np.newaxis, :, :, :].copy()
    return blob


def ctc_decode(logits, char_dict):
    preds = np.argmax(logits, axis=-1)
    results = []
    confidences = []
    for b in range(preds.shape[0]):
        seq = preds[b]
        result = []
        confs = []
        prev = -1
        for t in range(len(seq)):
            c = seq[t]
            if c == 0:
                prev = -1
                continue
            if c == prev:
                continue
            dict_idx = c - 1
            if 0 <= dict_idx < len(char_dict):
                result.append(char_dict[dict_idx])
                # 信頼度 = 最大確率
                prob = float(np.exp(logits[b, t, c]) / np.sum(np.exp(logits[b, t])))
                confs.append(prob)
            else:
                result.append('?')
                confs.append(0.0)
            prev = c
        results.append(''.join(result))
        avg_conf = np.mean(confs) if confs else 0.0
        confidences.append(avg_conf)
    return results, confidences


def recognize_region(crop, rec_sess, char_dict):
    blob = rec_preprocess(crop)
    input_name = rec_sess.get_inputs()[0].name
    output_name = rec_sess.get_outputs()[0].name
    output = rec_sess.run([output_name], {input_name: blob})[0]
    texts, confs = ctc_decode(output, char_dict)
    return (texts[0] if texts else ""), (confs[0] if confs else 0.0)


def extract_products(text):
    """製品コードらしい文字列を抽出（英数字とハイフンの組み合わせ）"""
    pattern = r'[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*'
    matches = re.findall(pattern, text.upper())
    return [m for m in matches if len(m) >= 3]


def normalize_code(code):
    """コードを正規化して比較用に"""
    return code.strip().upper()


def compute_match(recognized_codes, correct_codes):
    """
    認識結果と正解ラベルの一致率を計算
    - 完全一致: 認識コード == 正解コード
    - 部分一致: 正解コードの一部が認識コードに含まれる（またはその逆）
    """
    if not correct_codes:
        return {
            "exact_matches": [],
            "partial_matches": [],
            "misses": [],
            "extra": [],
            "exact_rate": 0.0,
            "partial_rate": 0.0
        }

    rec_norm = [normalize_code(c) for c in recognized_codes]
    cor_norm = [normalize_code(c) for c in correct_codes]

    exact_matches = []
    partial_matches = []
    matched_rec = set()
    matched_cor = set()

    # 完全一致チェック
    for i, r in enumerate(rec_norm):
        for j, c in enumerate(cor_norm):
            if j in matched_cor:
                continue
            if r == c:
                exact_matches.append({"recognized": recognized_codes[i], "correct": correct_codes[j]})
                matched_rec.add(i)
                matched_cor.add(j)
                break

    # 部分一致チェック（完全一致しなかったもの同士）
    for i, r in enumerate(rec_norm):
        if i in matched_rec:
            continue
        for j, c in enumerate(cor_norm):
            if j in matched_cor:
                continue
            # 部分一致: 片方が他方を含む、または編集距離が小さい
            if r in c or c in r or levenshtein_ratio(r, c) >= 0.7:
                partial_matches.append({"recognized": recognized_codes[i], "correct": correct_codes[j]})
                matched_rec.add(i)
                matched_cor.add(j)
                break

    misses = [correct_codes[j] for j in range(len(correct_codes)) if j not in matched_cor]
    extra = [recognized_codes[i] for i in range(len(recognized_codes)) if i not in matched_rec]

    total_correct = len(correct_codes)
    exact_rate = len(exact_matches) / total_correct if total_correct > 0 else 0.0
    partial_rate = (len(exact_matches) + len(partial_matches)) / total_correct if total_correct > 0 else 0.0

    return {
        "exact_matches": exact_matches,
        "partial_matches": partial_matches,
        "misses": misses,
        "extra": extra,
        "exact_rate": round(exact_rate, 4),
        "partial_rate": round(partial_rate, 4)
    }


def levenshtein_ratio(s1, s2):
    """編集距離ベースの類似度 (0.0〜1.0)"""
    if not s1 and not s2:
        return 1.0
    if not s1 or not s2:
        return 0.0
    max_len = max(len(s1), len(s2))
    if max_len == 0:
        return 1.0

    # 簡易的な編集距離
    m, n = len(s1), len(s2)
    dp = list(range(n + 1))
    for i in range(1, m + 1):
        prev = dp[0]
        dp[0] = i
        for j in range(1, n + 1):
            temp = dp[j]
            if s1[i-1] == s2[j-1]:
                dp[j] = prev
            else:
                dp[j] = 1 + min(prev, dp[j], dp[j-1])
            prev = temp
    dist = dp[n]
    return 1.0 - dist / max_len


def process_image(img_path, det_sess, rec_sess, char_dict):
    img = cv2.imread(str(img_path), cv2.IMREAD_COLOR)
    if img is None:
        print(f"  FAILED to load image: {img_path}")
        return None

    orig_h, orig_w = img.shape[:2]

    # Detection
    det_input, det_scale, orig_hw = det_preprocess(img)
    det_input_name = det_sess.get_inputs()[0].name
    det_output_name = det_sess.get_outputs()[0].name
    det_result = det_sess.run([det_output_name], {det_input_name: det_input})[0]
    boxes, angles, det_scores = det_postprocess(det_result, orig_hw, det_thresh=0.3)

    # Recognition with rotated crop
    result_img = img.copy()
    detections = []
    all_products = []

    for i, (box, angle, det_score) in enumerate(zip(boxes, angles, det_scores)):
        # 回転クロップ（パディングを大きくして十分な余白を確保）
        crop = crop_rotated(img, box, angle, padding=20)
        if crop is None:
            # フォールバック: 通常の外接矩形クロップ
            x_min, y_min = box[:, 0].min(), box[:, 1].min()
            x_max, y_max = box[:, 0].max(), box[:, 1].max()
            pad = 4
            x_min = max(0, x_min - pad)
            y_min = max(0, y_min - pad)
            x_max = min(orig_w, x_max + pad)
            y_max = min(orig_h, y_max + pad)
            if x_max <= x_min or y_max <= y_min:
                continue
            crop = img[y_min:y_max, x_min:x_max]

        text, confidence = recognize_region(crop, rec_sess, char_dict)
        text_filtered = filter_allowed(text)
        products = extract_products(text_filtered)
        all_products.extend(products)

        # 描画: 回転バウンディングボックス
        cv2.polylines(result_img, [box], True, (0, 255, 0), 2)

        # ラベル表示
        center_x = int(box[:, 0].mean())
        center_y = int(box[:, 1].mean())
        label = f"{angle:.1f}|{text_filtered}"
        cv2.putText(result_img, label, (center_x - 200, center_y - 40),
                   cv2.FONT_HERSHEY_SIMPLEX, 4.0, (0, 0, 255), 5)

        detections.append({
            "box_index": i,
            "angle_degree": round(float(angle), 2),
            "det_score": round(float(det_score), 4),
            "box_points": box.tolist(),
            "recognized_text": text,
            "text_filtered": text_filtered,
            "confidence": round(float(confidence), 4),
            "product_codes": products
        })

    # 結果画像保存
    out_img_path = RESULTS_DIR / f"{img_path.stem}_full_pipeline.jpg"
    cv2.imwrite(str(out_img_path), result_img)

    # JSON保存
    out_json_path = RESULTS_DIR / f"{img_path.stem}_full_pipeline.json"
    result_data = {
        "image": img_path.name,
        "image_size": [orig_w, orig_h],
        "num_detections": len(detections),
        "all_product_codes": all_products,
        "detections": detections
    }
    with open(out_json_path, 'w', encoding='utf-8') as f:
        json.dump(result_data, f, indent=2, ensure_ascii=False)

    return result_data


def main():
    # 画像ファイル収集
    img_files = sorted([f for f in IMAGES_DIR.iterdir()
                        if f.is_file() and f.suffix in IMG_EXTS])
    print(f"Found {len(img_files)} images in {IMAGES_DIR}")

    # 正解ラベル読み込み
    print("Loading correct labels...")
    labels = load_labels(LABEL_CSV, img_files)
    print(f"  Loaded labels for {len(labels)} images")

    if not img_files:
        print("No images found. Exiting.")
        sys.exit(1)

    # モデル読み込み（1回だけ）
    print("Loading models...")
    det_sess = ort.InferenceSession(str(DET_MODEL))
    rec_sess = ort.InferenceSession(str(REC_MODEL))
    char_dict = load_char_dict(DICT_FILE)
    print("Models loaded.")

    # 全画像処理
    summary = []
    total_start = time.time()
    total_exact_rate = 0.0
    total_partial_rate = 0.0
    images_with_labels = 0

    for idx, img_path in enumerate(img_files, 1):
        print(f"\n[{idx}/{len(img_files)}] Processing: {img_path.name}")
        t0 = time.time()
        result = process_image(img_path, det_sess, rec_sess, char_dict)
        elapsed = time.time() - t0

        if result is None:
            summary.append({
                "image": img_path.name,
                "status": "FAILED",
                "num_detections": 0,
                "time_seconds": round(elapsed, 2)
            })
            continue

        num_det = result["num_detections"]
        all_products = result["all_product_codes"]

        # 正解ラベルとの照合
        correct_codes = labels.get(img_path.name, [])
        match_result = compute_match(all_products, correct_codes) if correct_codes else None

        if match_result:
            total_exact_rate += match_result["exact_rate"]
            total_partial_rate += match_result["partial_rate"]
            images_with_labels += 1

        print(f"  -> {num_det} regions, products: {all_products}, time: {elapsed:.2f}s")
        if match_result:
            print(f"     Exact: {match_result['exact_rate']:.0%} ({len(match_result['exact_matches'])}/{len(correct_codes)}), "
                  f"Partial: {match_result['partial_rate']:.0%}")
            if match_result["misses"]:
                print(f"     Misses: {match_result['misses']}")
            if match_result["extra"]:
                print(f"     Extra: {match_result['extra']}")

        entry = {
            "image": img_path.name,
            "status": "OK",
            "num_detections": num_det,
            "product_codes": all_products,
            "correct_codes": correct_codes,
            "time_seconds": round(elapsed, 2)
        }
        if match_result:
            entry["match"] = match_result
        summary.append(entry)

    total_elapsed = time.time() - total_start

    # 全体サマリー
    avg_exact = total_exact_rate / images_with_labels if images_with_labels > 0 else 0.0
    avg_partial = total_partial_rate / images_with_labels if images_with_labels > 0 else 0.0

    summary_data = {
        "total_images": len(img_files),
        "images_with_labels": images_with_labels,
        "total_time_seconds": round(total_elapsed, 2),
        "average_exact_match_rate": round(avg_exact, 4),
        "average_partial_match_rate": round(avg_partial, 4),
        "results": summary
    }

    summary_path = RESULTS_DIR / "_summary.json"
    with open(summary_path, 'w', encoding='utf-8') as f:
        json.dump(summary_data, f, indent=2, ensure_ascii=False)

    # コンソール出力
    print(f"\n{'='*70}")
    print(f"BATCH OCR v2 RESULTS SUMMARY")
    print(f"{'='*70}")
    print(f"Total images: {len(img_files)}")
    print(f"Images with labels: {images_with_labels}")
    print(f"Total time: {total_elapsed:.2f}s")
    print(f"Average Exact Match Rate: {avg_exact:.1%}")
    print(f"Average Partial Match Rate: {avg_partial:.1%}")
    print(f"Output directory: {RESULTS_DIR}")
    print(f"{'='*70}")

    for s in summary:
        status = s['status']
        dets = s.get('num_detections', 0)
        products = s.get('product_codes', [])
        t = s['time_seconds']
        match_str = ""
        if "match" in s:
            m = s["match"]
            match_str = f" | exact={m['exact_rate']:.0%} partial={m['partial_rate']:.0%}"
        print(f"  {s['image']:45s} | {status:6s} | {dets:3d} dets | {products}{match_str} | {t:.2f}s")
    print(f"{'='*70}")
    print(f"Summary saved to: {summary_path}")


if __name__ == "__main__":
    main()
