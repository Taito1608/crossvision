# ONNX ModelPlaceholders

このフォルダには、実際のONNXモデルファイルを配置します。

## 必要なモデルファイル

### ocr_model.onnx
- **入力**: `data_0` - float[1, 3, 224, 224]
- **出力**: `softmaxout_1` - float[1, N] (Nはラベル数)
- **用途**: 商品コード認識

## ダミーモデルの作成方法（任意）

Python + ONNXで簡易モデルを作成:

```python
import torch
import torch.nn as nn
import onnx

class DummyOCRModel(nn.Module):
    def __init__(self):
        super().__init__()
        self.fc = nn.Linear(3 * 224 * 224, 1000)
    
    def forward(self, x):
        return self.fc(x.flatten(1))

model = DummyOCRModel()
dummy_input = torch.randn(1, 3, 224, 224)
torch.onnx.export(model, dummy_input, "ocr_model.onnx")
```

## 参考
- ONNX Runtime Android: https://onnxruntime.ai/docs/get-started/with-java/android.html
- PaddleOCR轻量モデル: https://github.com/PaddlePaddle/PaddleOCR
