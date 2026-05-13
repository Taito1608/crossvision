#!/usr/bin/env python3
"""
PP-OCRv3 ONNX バッチ全パイプライン検証スクリプト
OCR/images/ 内の全画像に対して検出・角度抽出・認識を実行し、
results/batch_full_pipeline/ に結果を出力する
"""
import numpy as np
import cv2
import onnxruntime as ort
from pathlib import Path
import json
import time
import re
import sys

# === パス設定 ===
SCRIPT_DIR = Path(__file__).parent
OCR_DIR = SCRIPT_DIR.parent
MODELS_DIR = OCR_DIR / "models"
IMAGES_DIR = OCR_DIR / "images"
RESULTS_DIR = OCR_DIR / "results" / "batch_full_pipeline"
RESULTS_DIR.mkdir(exist_ok=True)

DET_MODEL = MODELS_DIR / "det.onnx"
REC_MODEL = MODELS_DIR / "en_PP-OCRv3_rec.onnx"
DICT_FILE = MODELS_DIR / "en_PP-OCRv3_dict.txt"

# 対象画像拡張子
IMG_EXTS = {".jpg", ".jpeg", ".png", ".JPG", ".JPEG", ".PNG"}

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
    angles = []
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
        box[:, 0] *= scale_x
        box[:, 1] *= scale_y
        box = box.astype(np.int32)
        box[:, 0] = np.clip(box[:, 0], 0, orig_w - 1)
        box[:, 1] = np.clip(box[:, 1], 0, orig_h - 1)
        boxes.append(box)
        angles.append(angle)
    return boxes, angles

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
        print(f"  FAILED to load image: {img_path}")
        return None

    orig_h, orig_w = img.shape[:2]

    # Detection
    det_input, det_scale, orig_hw = det_preprocess(img)
    det_input_name = det_sess.get_inputs()[0].name
    det_output_name = det_sess.get_outputs()[0].name
    det_result = det_sess.run([det_output_name], {det_input_name: det_input})[0]
    boxes, angles = det_postprocess(det_result, orig_hw, det_thresh=0.3)

    # Recognition and result drawing
    result_img = img.copy()
    detections = []
    for i, (box, angle) in enumerate(zip(boxes, angles)):
        x_min, y_min = box[:, 0].min(), box[:, 1].min()
        x_max, y_max = box[:, 0].max(), box[:, 1].max()
        w = x_max - x_min
        h = y_max - y_min
        pad_x = int(w * 0.2)
        pad_y = int(h * 0.2)
        x_min = max(0, x_min - pad_x)
        y_min = max(0, y_min - pad_y)
        x_max = min(orig_w, x_max + pad_x)
        y_max = min(orig_h, y_max + pad_y)
        if x_max <= x_min or y_max <= y_min:
            continue
        crop = img[y_min:y_max, x_min:x_max]

        text = recognize_region(crop, rec_sess, char_dict)
        products = extract_products(text)

        # Draw expanded box
        cv2.polylines(result_img, [np.array([[x_min, y_min], [x_max, y_min], [x_max, y_max], [x_min, y_max]])], True, (0, 255, 0), 2)

        # Draw angle and text
        center_x = int((x_min + x_max) / 2)
        center_y = int((y_min + y_max) / 2)
        label = f"{angle:.1f}"
        if text:
            label += f"|{text}"
        cv2.putText(result_img, label, (center_x - 100, center_y - 20),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)

        detections.append({
            "box_index": i,
            "angle_degree": float(angle),
            "box_points": box.tolist(),
            "expanded_box": [int(x_min), int(y_min), int(x_max), int(y_max)],
            "recognized_text": text,
            "product_codes": products
        })

    # Save result image
    out_img_path = RESULTS_DIR / f"{img_path.stem}_full_pipeline.jpg"
    cv2.imwrite(str(out_img_path), result_img)

    # Save JSON
    out_json_path = RESULTS_DIR / f"{img_path.stem}_full_pipeline.json"
    result_data = {
        "image": img_path.name,
        "image_size": [orig_w, orig_h],
        "num_detections": len(detections),
        "detections": detections
    }
    with open(out_json_path, 'w', encoding='utf-8') as f:
        json.dump(result_data, f, indent=2, ensure_ascii=False)

    return result_data

def main():
    # Collect image files
    img_files = sorted([f for f in IMAGES_DIR.iterdir()
                        if f.is_file() and f.suffix in IMG_EXTS])
    print(f"Found {len(img_files)} images in {IMAGES_DIR}")

    if not img_files:
        print("No images found. Exiting.")
        sys.exit(1)

    # Load models once
    print("Loading models...")
    det_sess = ort.InferenceSession(str(DET_MODEL))
    rec_sess = ort.InferenceSession(str(REC_MODEL))
    char_dict = load_char_dict(DICT_FILE)
    print("Models loaded.")

    # Process all images
    summary = []
    total_start = time.time()

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
        else:
            num_det = result["num_detections"]
            all_products = []
            for d in result["detections"]:
                all_products.extend(d["product_codes"])
            print(f"  -> {num_det} regions, products: {all_products}, time: {elapsed:.2f}s")
            summary.append({
                "image": img_path.name,
                "status": "OK",
                "num_detections": num_det,
                "product_codes": all_products,
                "time_seconds": round(elapsed, 2)
            })

    total_elapsed = time.time() - total_start

    # Save summary
    summary_data = {
        "total_images": len(img_files),
        "total_time_seconds": round(total_elapsed, 2),
        "results": summary
    }
    summary_path = RESULTS_DIR / "_summary.json"
    with open(summary_path, 'w', encoding='utf-8') as f:
        json.dump(summary_data, f, indent=2, ensure_ascii=False)

    # Print summary
    print(f"\n{'='*60}")
    print(f"BATCH RESULTS SUMMARY")
    print(f"{'='*60}")
    print(f"Total images: {len(img_files)}")
    print(f"Total time: {total_elapsed:.2f}s")
    print(f"Output directory: {RESULTS_DIR}")
    print(f"{'='*60}")
    for s in summary:
        status = s['status']
        dets = s.get('num_detections', 0)
        products = s.get('product_codes', [])
        t = s['time_seconds']
        print(f"  {s['image']:40s} | {status:6s} | {dets:3d} dets | products: {products} | {t:.2f}s")
    print(f"{'='*60}")
    print(f"Summary saved to: {summary_path}")

if __name__ == "__main__":
    main()
