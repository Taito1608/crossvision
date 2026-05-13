#!/usr/bin/env python3
"""
PP-OCRv3 ONNX 全パイプライン検証スクリプト
検出・角度抽出・認識を統合し、結果を可視化
"""
import numpy as np
import cv2
import onnxruntime as ort
from pathlib import Path
import json
import time
import re

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

def main():
    print("Loading models...")
    det_sess = ort.InferenceSession(str(DET_MODEL))
    rec_sess = ort.InferenceSession(str(REC_MODEL))
    char_dict = load_char_dict(DICT_FILE)

    img_path = IMAGES_DIR / "M4sb29-2.jpg"
    print(f"Processing image: {img_path}")

    img = cv2.imread(str(img_path), cv2.IMREAD_COLOR)
    if img is None:
        print("Failed to load image")
        return

    # Detection
    print("Running detection...")
    det_input, det_scale, orig_hw = det_preprocess(img)
    det_input_name = det_sess.get_inputs()[0].name
    det_output_name = det_sess.get_outputs()[0].name
    det_result = det_sess.run([det_output_name], {det_input_name: det_input})[0]
    boxes, angles = det_postprocess(det_result, orig_hw, det_thresh=0.3)

    print(f"Detected {len(boxes)} text regions")

    # Recognition and result drawing
    result_img = img.copy()
    results = []
    for i, (box, angle) in enumerate(zip(boxes, angles)):
        # Expand box slightly to include more context
        x_min, y_min = box[:, 0].min(), box[:, 1].min()
        x_max, y_max = box[:, 0].max(), box[:, 1].max()
        w = x_max - x_min
        h = y_max - y_min
        # Add 20% padding
        pad_x = int(w * 0.2)
        pad_y = int(h * 0.2)
        x_min = max(0, x_min - pad_x)
        y_min = max(0, y_min - pad_y)
        x_max = min(img.shape[1], x_max + pad_x)
        y_max = min(img.shape[0], y_max + pad_y)
        if x_max <= x_min or y_max <= y_min:
            continue
        crop = img[y_min:y_max, x_min:x_max]

        # Recognize
        text = recognize_region(crop, rec_sess, char_dict)
        products = extract_products(text)
        print(f"Box {i}: angle = {angle:.1f}°, text = '{text}', products = {products}")

        # Draw expanded box
        cv2.polylines(result_img, [np.array([[x_min, y_min], [x_max, y_min], [x_max, y_max], [x_min, y_max]])], True, (0, 255, 0), 2)

        # Draw angle and text
        center_x = int((x_min + x_max) / 2)
        center_y = int((y_min + y_max) / 2)
        label = f"Angle: {angle:.1f}°"
        if text:
            label += f" | Text: {text}"
        cv2.putText(result_img, label, (center_x - 100, center_y - 20),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)

        results.append({
            "box_index": i,
            "angle_degree": float(angle),
            "box_points": box.tolist(),
            "expanded_box": [int(x_min), int(y_min), int(x_max), int(y_max)],
            "recognized_text": text,
            "product_codes": products
        })

    # Save result
    output_path = RESULTS_DIR / "M4sb29-2_full_pipeline.jpg"
    cv2.imwrite(str(output_path), result_img)
    print(f"Result saved to: {output_path}")

    # Save JSON
    json_path = RESULTS_DIR / "M4sb29-2_full_pipeline.json"
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump({
            "image": str(img_path.name),
            "detections": results
        }, f, indent=2, ensure_ascii=False)
    print(f"Full data saved to: {json_path}")

if __name__ == "__main__":
    main()