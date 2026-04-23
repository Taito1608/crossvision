# ONNX Runtime Android PoC - 実装メモ

## 実装済みのファイル

### 1. app/build.gradle
- ONNX Runtime依存関係を追加済み:
```gradle
implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.16.3'
```

### 2. OnnxOcrEngine.kt
- ONNX RuntimeのKotlinラッパーを作成
- 主なメソッド:
  - `loadModel()`: ONNXモデル読み込み
  - `runInference(input: FloatArray)`: 推論実行
  - `extractProductCode(bitmap: Bitmap)`: 本番API設計（仮）

### 3. OnnxTestActivity.kt
- PoCテスト用Activity
- モデル読み込みと推論のテスト機能

### 4. activity_onnx_test.xml
- テスト用UIレイアウト

### 5. AndroidManifest.xml
- OnnxTestActivityを追加

## 残タスク

### ビルドとテスト（手動実行が必要）
```bash
# 1. Android Studioでプロジェクトを開く
# 2. Android SDK locationを確認（local.propertiesの設定）
# 3. Sync Project with Gradle Files
# 4. Run「OnnxTestActivity」
```

### 次のステップ
1. **ONNX Runtime AARが正しく依存解決されているか確認**
   - ビルドエラーがなければOK

2. **ダミーモデルの作成（任意）**
   - 本格的な前処理・後処理は明日以降

## API設計メモ

```kotlin
class OnnxOcrEngine {
    // Singleton pattern
    fun getInstance(): OnnxOcrEngine
    
    // Initialize
    fun loadModel(): Boolean
    
    // Core API
    fun extractProductCode(bitmap: Bitmap): List<String>
    
    // Internal
    fun runInference(inputTensor: FloatArray): FloatArray?
    fun release()
}
```

## 参考リンク
- ONNX Runtime Android: https://onnxruntime.ai/docs/get-started/with-java/android.html
- API Docs: https://javadoc.io/doc/com.microsoft.onnxruntime/onnxruntime-android/latest/index.html

## 備考
- JNIは使用せず、ONNX RuntimeのKotlin APIのみを使用
- モデルファイルは手動でassetsフォルダに配置が必要
