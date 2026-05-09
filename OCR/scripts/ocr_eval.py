#!/usr/bin/env python3
"""
PP-OCRv3 ONNX 評価スクリプト
OCRディレクトリの画像・モデルを使用して精度評価
"""
import numpy as np
import cv2
import onnxruntime as ort
from pathlib import Path
import re
import json
import time

# === パス設定 ===
SCRIPT_DIR = Path(__file__).parent
OCR_DIR = SCRIPT_DIR.parent
MODELS_DIR = OCR_DIR / "models"
IMAGES_DIR = OCR_DIR / "images"
RESULTS_DIR = OCR_DIR / "results"
RESULTS_DIR.mkdir(exist_ok=True)

DET_MODEL = MODELS_DIR / "det.onnx"
REC_MODEL = MODELS_DIR / "en_PP-OCRv3_rec.onnx"
DICT_FILE = MODELS_DIR / "en_PP-OCRv3_dict.txt"

# === 正解データ ===
GROUND_TRUTH = {
    "IMG_0787.jpg":            ["BISb30N-16"],
    "M5sb24-10.jpg":           ["M5sb24-10"],
    "IMG_1537.jpg":            ["BISb40N-27", "BISb40N-8", "BISb40N-26", "BISb40N-9"],
    "IMG_1548.jpg":            ["BISb40N-46"],
    "IMG_1571.jpg":            ["BISb30N-13A", "BISb30N-13"],
    "IMG_0422.jpg":            ["BISb30N-7A", "BISb30N-4I", "BISb30N-7A"],
    "IMG20250416153020.jpg":   ["BISb30N-23", "BISb30N-7"],
    "multi_008_yellow_green.JPG": ["BISb30N-7A", "BISb30N-41", "BISb30N-7A", "BISb30N-53", "BISb30N-37A"],
    "N5G15-X1Y1.jpg":          ["N5G15-X1Y1"],
    "multi_001_sunny_yellow.JPG": ["BISb30N-36", "BISb30N-40", "BISb30N-19", "BISb30N-20", "BISb30N-56", "BISb30N-34", "BISb30N-12", "BISb30N-26"],
    "015_B15b30N-41__B15b30N-7A_45deg_NW_yellow.JPG": ["BISb30N-7A", "BISb30N-4I", "BISb30N-7A", "BISb30N-53"],
    "IMG20250416160821.jpg":   ["BISb30N-19", "BISb30N-20"],
    "IMG_1536.jpg":            ["BISb40N-23", "BISb40N-15"],
    "IMG_0771.jpg":            ["BISb40N-23", "BISb40N-15", "BISb40N-27"],
}

# === 画像特徴メタデータ ===
IMAGE_FEATURES = {
    "IMG_0787.jpg":            {"色": "黄色",     "ノイズ": "なし",       "傾き": "なし",   "光線": "明暗"},
    "M5sb24-10.jpg":           {"色": "水色",     "ノイズ": "さび",       "傾き": "なし",   "光線": "なし"},
    "IMG_1537.jpg":            {"色": "水色",     "ノイズ": "なし",       "傾き": "垂直",   "光線": "なし"},
    "IMG_1548.jpg":            {"色": "水色",     "ノイズ": "さび+ピントぼけ", "傾き": "垂直", "光線": "なし"},
    "IMG_1571.jpg":            {"色": "水色",     "ノイズ": "なし",       "傾き": "なし",   "光線": "なし"},
    "IMG_0422.jpg":            {"色": "黄色+水色混合", "ノイズ": "なし",   "傾き": "なし",   "光線": "明暗"},
    "IMG20250416153020.jpg":   {"色": "黄色",     "ノイズ": "なし",       "傾き": "斜め",   "光線": "明暗"},
    "multi_008_yellow_green.JPG": {"色": "黄色+水色混合", "ノイズ": "なし", "傾き": "複数の向き", "光線": "なし"},
    "N5G15-X1Y1.jpg":          {"色": "水色",     "ノイズ": "さび",       "傾き": "なし",   "光線": "なし"},
    "multi_001_sunny_yellow.JPG": {"色": "黄色",   "ノイズ": "なし",       "傾き": "複数の向き", "光線": "明暗"},
    "015_B15b30N-41__B15b30N-7A_45deg_NW_yellow.JPG": {"色": "黄色", "ノイズ": "なし", "傾き": "45度", "光線": "なし"},
    "IMG20250416160821.jpg":   {"色": "黄色(白背景部分あり)", "ノイズ": "なし", "傾き": "少し斜め", "光線": "明暗混ざり"},
    "IMG_1536.jpg":            {"色": "水色",     "ノイズ": "大きさ大小", "傾き": "逆向き", "光線": "なし"},
    "IMG_0771.jpg":            {"色": "水色",     "ノイズ": "色々な大きさ", "傾き": "なし", "光線": "なし"},
}


def load_char_dict(dict_path):
    chars = []
    with open(dict_path, 'r', encoding='utf-8') as f:
        for line in f:
            chars.append(line.strip())
    return chars


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
    for contour in contours:
        if len(contour) < 4:
            continue
        rect = cv2.minAreaRect(contour)
        box = cv2.boxPoints(rect)
        box_w = rect[1][0]
        box_h = rect[1][1]
        if box_w < 5 or box_h < 5:
            continue
        box[:, 0] *= scale_x
        box[:, 1] *= scale_y
        box = box.astype(np.int32)
        box[:, 0] = np.clip(box[:, 0], 0, orig_w - 1)
        box[:, 1] = np.clip(box[:, 1], 0, orig_h - 1)
        boxes.append(box)
    return boxes


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
    for b in range(preds.shape[0]):
        seq = preds[b]
        result = []
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
            else:
                result.append('?')
            prev = c
        results.append(''.join(result))
    return results


def recognize_region(crop, rec_sess, char_dict):
    blob = rec_preprocess(crop)
    input_name = rec_sess.get_inputs()[0].name
    output_name = rec_sess.get_outputs()[0].name
    output = rec_sess.run([output_name], {input_name: blob})[0]
    texts = ctc_decode(output, char_dict)
    return texts[0] if texts else ""


def extract_products(text):
    pattern = r'[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*'
    matches = re.findall(pattern, text.upper())
    return [m for m in matches if len(m) >= 3]


def process_image(img_path, det_sess, rec_sess, char_dict):
    img = cv2.imread(str(img_path), cv2.IMREAD_COLOR)
    if img is None:
        return [], [], 0

    det_input, det_scale, orig_hw = det_preprocess(img)
    det_input_name = det_sess.get_inputs()[0].name
    det_output_name = det_sess.get_outputs()[0].name
    det_result = det_sess.run([det_output_name], {det_input_name: det_input})[0]
    boxes = det_postprocess(det_result, orig_hw)

    if not boxes:
        full_text = recognize_region(img, rec_sess, char_dict)
        return [full_text], [], img.shape[:2]

    centers = [(np.mean(b[:, 0]), np.mean(b[:, 1])) for b in boxes]
    sorted_indices = sorted(range(len(boxes)), key=lambda i: (centers[i][1], centers[i][0]))

    texts = []
    valid_boxes = []
    for idx in sorted_indices:
        box = boxes[idx]
        x_min = max(0, int(box[:, 0].min()) - 5)
        y_min = max(0, int(box[:, 1].min()) - 5)
        x_max = min(img.shape[1], int(box[:, 0].max()) + 5)
        y_max = min(img.shape[0], int(box[:, 1].max()) + 5)
        if x_max <= x_min or y_max <= y_min:
            continue
        crop = img[y_min:y_max, x_min:x_max]
        text = recognize_region(crop, rec_sess, char_dict)
        texts.append(text)
        valid_boxes.append(box)

    return texts, valid_boxes, img.shape[:2]


def lcs_length(a, b):
    """Longest Common Subsequence length"""
    n, m = len(a), len(b)
    dp = [[0] * (m + 1) for _ in range(n + 1)]
    for i in range(1, n + 1):
        for j in range(1, m + 1):
            if a[i-1] == b[j-1]:
                dp[i][j] = dp[i-1][j-1] + 1
            else:
                dp[i][j] = max(dp[i-1][j], dp[i][j-1])
    return dp[n][m]


def edit_distance_similarity(a, b):
    """編集距離類似度 (1 - normalized_edit_distance)"""
    n, m = len(a), len(b)
    dp = list(range(m + 1))
    for i in range(1, n + 1):
        prev = dp[0]
        dp[0] = i
        for j in range(1, m + 1):
            temp = dp[j]
            if a[i-1] == b[j-1]:
                dp[j] = prev
            else:
                dp[j] = 1 + min(prev, dp[j], dp[j-1])
            prev = temp
    dist = dp[m]
    max_len = max(n, m)
    return 1.0 - dist / max_len if max_len > 0 else 1.0


def evaluate_one(filename, gt_codes, img_path, det_sess, rec_sess, char_dict):
    t0 = time.time()
    texts, boxes, img_hw = process_image(img_path, det_sess, rec_sess, char_dict)
    elapsed = time.time() - t0

    all_found = []
    for t in texts:
        all_found.extend(extract_products(t))

    gt_upper = [c.upper() for c in gt_codes]
    tp = [c for c in all_found if c in gt_upper]
    fp = [c for c in all_found if c not in gt_upper]
    fn = [c for c in gt_upper if c not in all_found]

    # 部分マッチ: 全連結テキストに正解が含まれるか
    full_text = ''.join(texts).upper()
    partial = [c for c in gt_upper if c in full_text and c not in tp]

    # 文字レベル類似度: 全連結テキスト vs 全連結正解
    combined_ocr = ''.join(texts).upper()
    combined_gt = ''.join(gt_upper)
    lcs_sim = lcs_length(combined_ocr, combined_gt) / max(len(combined_gt), 1)
    edit_sim = edit_distance_similarity(combined_ocr, combined_gt)

    return {
        "filename": filename,
        "gt_codes": gt_codes,
        "texts": texts,
        "num_boxes": len(boxes),
        "found": all_found,
        "true_pos": tp,
        "false_pos": fp,
        "false_neg": fn,
        "partial_matches": partial,
        "lcs_similarity": round(lcs_sim, 4),
        "edit_similarity": round(edit_sim, 4),
        "elapsed": round(elapsed, 2),
        "img_hw": list(img_hw) if isinstance(img_hw, tuple) else [0, 0],
    }


def main():
    print("=" * 80)
    print("PP-OCRv3 ONNX 精度評価")
    print("=" * 80)

    char_dict = load_char_dict(DICT_FILE)
    print(f"辞書: {len(char_dict)}文字")

    print("\nモデル読み込み...")
    det_sess = ort.InferenceSession(str(DET_MODEL), providers=['CPUExecutionProvider'])
    rec_sess = ort.InferenceSession(str(REC_MODEL), providers=['CPUExecutionProvider'])
    print(f"  Det: {det_sess.get_inputs()[0].name} → {det_sess.get_outputs()[0].name}")
    print(f"  Rec: {rec_sess.get_inputs()[0].name} → {rec_sess.get_outputs()[0].name}")

    results = []
    for filename, gt_codes in GROUND_TRUTH.items():
        img_path = IMAGES_DIR / filename
        if not img_path.exists():
            print(f"\n  SKIP: {filename} (画像なし)")
            continue
        print(f"\n--- {filename} ---")
        result = evaluate_one(filename, gt_codes, img_path, det_sess, rec_sess, char_dict)
        results.append(result)
        print(f"  GT: {gt_codes}")
        print(f"  Boxes: {result['num_boxes']}")
        print(f"  Texts: {result['texts']}")
        print(f"  Found: {result['found']}")
        print(f"  LCS: {result['lcs_similarity']:.1%}  Edit: {result['edit_similarity']:.1%}")
        print(f"  Time: {result['elapsed']:.2f}s")

    # 集計
    total_gt = sum(len(r['gt_codes']) for r in results)
    total_tp = sum(len(r['true_pos']) for r in results)
    total_fp = sum(len(r['false_pos']) for r in results)
    total_fn = sum(len(r['false_neg']) for r in results)
    precision = total_tp / (total_tp + total_fp) if (total_tp + total_fp) > 0 else 0
    recall = total_tp / (total_gt) if total_gt > 0 else 0
    f1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0

    total_partial = sum(len(r['partial_matches']) for r in results)
    partial_recall = total_partial / total_gt if total_gt > 0 else 0

    avg_lcs = np.mean([r['lcs_similarity'] for r in results])
    avg_edit = np.mean([r['edit_similarity'] for r in results])
    total_time = sum(r['elapsed'] for r in results)

    print("\n" + "=" * 80)
    print("評価サマリー")
    print("=" * 80)
    print(f"画像数:           {len(results)} / {len(GROUND_TRUTH)}")
    print(f"正解コード総数:    {total_gt}")
    print(f"True Positive:     {total_tp}")
    print(f"False Positive:    {total_fp}")
    print(f"False Negative:    {total_fn}")
    print(f"Precision:         {precision:.1%}")
    print(f"Recall:            {recall:.1%}")
    print(f"F1 Score:          {f1:.1%}")
    print(f"Partial Recall:    {partial_recall:.1%}")
    print(f"平均LCS類似度:     {avg_lcs:.1%}")
    print(f"平均編集距離類似度: {avg_edit:.1%}")
    print(f"合計処理時間:      {total_time:.1f}s")
    print(f"平均処理時間:      {total_time/len(results):.2f}s/image")

    print("\n--- 画像別結果 ---")
    for r in results:
        feat = IMAGE_FEATURES.get(r['filename'], {})
        tp = len(r['true_pos'])
        total = len(r['gt_codes'])
        pm = len(r['partial_matches'])
        print(f"  {r['filename'][:45]:45s} | TP={tp}/{total} | LCS={r['lcs_similarity']:.0%} | Partial={pm} | {feat.get('色','')} {feat.get('ノイズ','')}")

    # JSON出力
    output = {
        "summary": {
            "num_images": len(results),
            "total_gt": total_gt,
            "tp": total_tp, "fp": total_fp, "fn": total_fn,
            "precision": round(precision, 4),
            "recall": round(recall, 4),
            "f1": round(f1, 4),
            "partial_recall": round(partial_recall, 4),
            "avg_lcs_similarity": round(avg_lcs, 4),
            "avg_edit_similarity": round(avg_edit, 4),
            "total_time": round(total_time, 2),
            "avg_time": round(total_time / len(results), 2),
        },
        "details": results
    }
    out_path = RESULTS_DIR / "eval_results.json"
    with open(out_path, 'w', encoding='utf-8') as f:
        json.dump(output, f, ensure_ascii=False, indent=2)
    print(f"\n結果保存: {out_path}")


if __name__ == "__main__":
    main()
