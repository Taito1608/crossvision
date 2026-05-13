#!/usr/bin/env python3
"""
PP-OCRv3 ONNX 検出・角度抽出検証スクリプト
特定画像の領域抽出と角度取得をテスト
"""
import numpy as np
import cv2
import onnxruntime as ort
from pathlib import Path
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
    angles = []  # 各ボックスの角度を格納
    for contour in contours:
        if len(contour) < 4:
            continue
        rect = cv2.minAreaRect(contour)
        box = cv2.boxPoints(rect)
        box_w = rect[1][0]
        box_h = rect[1][1]
        angle = rect[2]  # 角度を取得

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

def main():
    # モデルロード
    print("Loading models...")
    det_sess = ort.InferenceSession(str(DET_MODEL))
    rec_sess = ort.InferenceSession(str(REC_MODEL))
    char_dict = load_char_dict(DICT_FILE)

    # テスト画像
    img_path = IMAGES_DIR / "M4sb29-2.jpg"
    print(f"Processing image: {img_path}")

    img = cv2.imread(str(img_path), cv2.IMREAD_COLOR)
    if img is None:
        print("Failed to load image")
        return

    # 検出実行
    print("Running detection...")
    det_input, det_scale, orig_hw = det_preprocess(img)
    det_input_name = det_sess.get_inputs()[0].name
    det_output_name = det_sess.get_outputs()[0].name
    det_result = det_sess.run([det_output_name], {det_input_name: det_input})[0]
    boxes, angles = det_postprocess(det_result, orig_hw)

    print(f"Detected {len(boxes)} text regions")

    # 結果を描画
    result_img = img.copy()
    for i, (box, angle) in enumerate(zip(boxes, angles)):
        # バウンディングボックスを描画
        cv2.polylines(result_img, [box], True, (0, 255, 0), 2)

        # 中心点を計算
        center_x = int(np.mean(box[:, 0]))
        center_y = int(np.mean(box[:, 1]))

        # 角度テキストを描画
        text = f"Angle: {angle:.1f}°"
        cv2.putText(result_img, text, (center_x - 30, center_y - 10),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)

        print(f"Box {i}: angle = {angle:.1f}°")

    # 結果保存
    output_path = RESULTS_DIR / "M4sb29-2_detection_angle.jpg"
    cv2.imwrite(str(output_path), result_img)
    print(f"Result saved to: {output_path}")

    # 角度情報をJSONで保存
    angle_data = {
        "image": str(img_path.name),
        "detections": [
            {
                "box_index": i,
                "angle_degree": float(angle),
                "box_points": box.tolist()
            }
            for i, (box, angle) in enumerate(zip(boxes, angles))
        ]
    }

    json_path = RESULTS_DIR / "M4sb29-2_detection_angle.json"
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(angle_data, f, indent=2, ensure_ascii=False)
    print(f"Angle data saved to: {json_path}")

if __name__ == "__main__":
    main()