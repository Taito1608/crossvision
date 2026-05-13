import os
import cv2
import numpy as np
import onnxruntime as ort

# --- 設定 ---
IMAGE_PATH = r"C:\Solution\resize14\M5sb24-10\M5sb24-10_resize.png"
MODEL_PATH = r"C:\Users\rinoa\Downloads\ppocrv3_rec.onnx"
DICT_PATH  = r"C:\Users\rinoa\Downloads\ppocrv3_dict.txt"

def load_dict(path):
    with open(path, 'r', encoding='utf-8') as f:
        char_list = [line.strip("\r\n") for line in f.readlines() if line.strip("\r\n")]
    return ['blank'] + char_list

def get_ocr_result(session, img, char_dict):
    """画像から文字と信頼度スコアを計算"""
    # モデルの入力に合わせてリサイズ
    target_h, target_w = 48, 320
    img_resized = cv2.resize(img, (target_w, target_h), interpolation=cv2.INTER_CUBIC)
    
    # 前処理
    img_proc = cv2.cvtColor(img_resized, cv2.COLOR_BGR2RGB).transpose((2, 0, 1))
    img_proc = (img_proc.astype('float32') / 255.0 - 0.5) / 0.5
    img_proc = np.expand_dims(img_proc, axis=0)

    # 推論
    input_name = session.get_inputs()[0].name
    outputs = session.run(None, {input_name: img_proc})[0][0]
    
    # 確率計算
    exp_out = np.exp(outputs - np.max(outputs, axis=1, keepdims=True))
    probs = exp_out / np.sum(exp_out, axis=1, keepdims=True)
    
    preds_idx = np.argmax(probs, axis=1)
    preds_prob = np.max(probs, axis=1)
    
    result_text = []
    conf_scores = []
    last_idx = 0
    for i, idx in enumerate(preds_idx):
        if idx > 0 and idx != last_idx:
            if idx < len(char_dict):
                result_text.append(char_dict[idx])
                conf_scores.append(preds_prob[i])
        last_idx = idx
    
    avg_score = np.mean(conf_scores) if conf_scores else 0.0
    return "".join(result_text), avg_score, img_resized

def main():
    print("\n" + "="*50)
    print("  4方向対応（0/90/180/270度）OCRテスト")
    print("="*50)

    char_dict = load_dict(DICT_PATH)
    session = ort.InferenceSession(MODEL_PATH, providers=['CPUExecutionProvider'])

    # 画像の読み込み
    n = np.fromfile(IMAGE_PATH, np.uint8)
    img = cv2.imdecode(n, cv2.IMREAD_COLOR)
    if img is None:
        print("画像が読み込めません。")
        return

    # 4パターンの画像を生成してOCR実行
    # 辞書形式で結果を保存
    patterns = {
        "0度 (そのまま)": img,
        "90度 (時計回り)": cv2.rotate(img, cv2.ROTATE_90_CLOCKWISE),
        "180度 (逆さま)": cv2.rotate(img, cv2.ROTATE_180),
        "270度 (反時計回り)": cv2.rotate(img, cv2.ROTATE_90_COUNTERCLOCKWISE)
    }

    best_text = ""
    best_score = -1.0
    best_orient = ""
    best_img = None

    for label, rotated_img in patterns.items():
        text, score, res_img = get_ocr_result(session, rotated_img, char_dict)
        print(f"【{label}】")
        print(f"   認識: '{text}'  (信頼度: {score:.4f})")
        
        # スコアが一番高いものを記憶
        if score > best_score:
            best_score = score
            best_text = text
            best_orient = label
            best_img = res_img

    print("\n" + "-"*40)
    print(f">>> 判定結果: {best_orient} を採用")
    print(f"最終認識文字列: {best_text}")
    print("-"*40 + "\n")

    # 結果画像の表示
    cv2.imshow("Final Decision Image", cv2.resize(best_img, (640, 96)))
    print("どれかキーを押すと終了します。")
    cv2.waitKey(0)
    cv2.destroyAllWindows()

if __name__ == "__main__":
    main()