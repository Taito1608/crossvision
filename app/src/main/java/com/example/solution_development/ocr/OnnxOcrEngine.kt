package com.example.solution_development.ocr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.regex.Pattern

class OnnxOcrEngine(private val context: Context) {

    enum class ModelPreset(
        val modelFile: String,
        val dictFile: String,
        val outputName: String,
        val numClasses: Int
    ) {
        PNNX(
            modelFile = "onnx/pnnx_english.onnx",
            dictFile = "onnx/dict.txt",
            outputName = "fetch_name_0",
            numClasses = 438          // 436 dict + 1 blank + 1 extra
        ),
        PP_OCR_V3_RAPIDOCR(
            modelFile = "onnx/ppocrv3_rec.onnx",
            dictFile = "onnx/ppocrv3_dict.txt",
            outputName = "softmax_2.tmp_0",
            numClasses = 97           // 95 en_dict + 1 space + 1 blank
        ),
        PP_OCR_V5_CUSTOM(
            modelFile = "onnx/rec_v5.onnx",
            dictFile = "onnx/custom_dict.txt",
            outputName = "softmax_2.tmp_0",
            numClasses = 438          // v5 full dict (437 chars + blank)
        )
    }

    companion object {
        private const val TAG = "OnnxOcrEngine"

        // 入力/出力共通仕様
        private const val INPUT_NAME = "x"
        private const val INPUT_WIDTH = 320
        private const val INPUT_HEIGHT = 48
        private const val INPUT_CHANNELS = 3

        // PaddleOCR normalization: (pixel - 127.5) / 127.5
        private const val PADDLE_MEAN = 127.5f
        private const val PADDLE_STD = 127.5f

        // 製品コード抽出用正規表現: 大文字英数字3文字以上 + 任意のハイフンと数字
        private val PRODUCT_CODE_PATTERN = Pattern.compile("[A-Z0-9]{3,}-?[0-9]*")
    }

    private var session: OrtSession? = null
    private var ortEnvironment: OrtEnvironment? = null
    private var isModelLoaded = false

    // 選択中のモデル設定
    private var currentPreset: ModelPreset = ModelPreset.PP_OCR_V3_RAPIDOCR

    // 文字辞書（インデックス → 文字）
    private val charDict = mutableListOf<String>()
    private var dictLoaded = false

    /**
     * 使用するモデルプリセットを設定
     */
    fun setModelPreset(preset: ModelPreset) {
        release()
        currentPreset = preset
        charDict.clear()
        dictLoaded = false
    }

    /**
     * 文字辞書を assets から読み込む
     */
    private fun loadCharDict(): Boolean {
        return try {
            charDict.clear()
            context.assets.open(currentPreset.dictFile).use { inputStream ->
                inputStream.bufferedReader().forEachLine { line ->
                    charDict.add(line)
                }
            }
            dictLoaded = charDict.isNotEmpty()
            Log.d(TAG, "Char dict loaded: ${charDict.size} characters from ${currentPreset.dictFile}")
            Log.d(TAG, "Expected numClasses: ${currentPreset.numClasses}, dictSize: ${charDict.size}, diff: ${currentPreset.numClasses - charDict.size}")
            dictLoaded
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load char dict from ${currentPreset.dictFile}", e)
            false
        }
    }

    /**
     * assets からモデルファイルを読み込む
     */
    private fun loadModelFromAssets(modelPath: String): ByteArray {
        context.assets.open(modelPath).use { inputStream ->
            ByteArrayOutputStream().use { outputStream ->
                val buffer = ByteArray(4 * 1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                return outputStream.toByteArray()
            }
        }
    }

    /**
     * ONNXモデルをロード
     */
    fun loadModel(): Boolean {
        Log.d(TAG, "loadModel() called, preset: ${currentPreset.name}")
        return try {
            // 文字辞書を先に読み込む
            if (!loadCharDict()) {
                Log.e(TAG, "Failed to load character dictionary")
                return false
            }

            // ONNX環境の作成
            ortEnvironment = OrtEnvironment.getEnvironment()
            Log.d(TAG, "OrtEnvironment created")

            // モデルファイルの確認
            try {
                context.assets.open(currentPreset.modelFile).close()
                Log.d(TAG, "Model file exists in assets: ${currentPreset.modelFile}")
            } catch (e: Exception) {
                Log.e(TAG, "Model file NOT found in assets: ${currentPreset.modelFile}", e)
                throw Exception("Model file '${currentPreset.modelFile}' not found in assets.")
            }

            // モデルの読み込み
            val modelBytes = loadModelFromAssets(currentPreset.modelFile)
            Log.d(TAG, "Model loaded from assets, size: ${modelBytes.size} bytes")

            // セッションの作成
            val sessionOptions = OrtSession.SessionOptions()
            session = ortEnvironment?.createSession(modelBytes, sessionOptions)

            if (session != null) {
                Log.d(TAG, "Session created successfully")
                Log.d(TAG, "Input names: ${session?.inputNames}")
                Log.d(TAG, "Output names: ${session?.outputNames}")
                Log.d(TAG, "Output key to use: ${session?.outputNames?.firstOrNull()}")
            }

            isModelLoaded = session != null
            isModelLoaded
        } catch (e: OrtException) {
            Log.e(TAG, "OrtException in loadModel", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception in loadModel", e)
            false
        }
    }

    /**
     * BitmapをONNX入力用テンソルに変換
     * PaddleOCR Recognitionモデル用前処理:
     * 1. アスペクト比を維持してリサイズ (高さ=48固定, 幅は比率に応じて)
     * 2. 幅をINPUT_WIDTHまで-1.0パディング (正規化後の黒)
     * 3. RGB チャンネル順
     * 4. 正規化 (pixel - 127.5) / 127.5
     * 5. NCHW 形式（planar）に変換
     */
    private fun bitmapToInputTensor(bitmap: Bitmap): OnnxTensor {
        val srcW = bitmap.width
        val srcH = bitmap.height

        // アスペクト比を維持してリサイズ (高さ=48固定, 幅は比率に応じて)
        val ratio = srcW.toFloat() / srcH.toFloat()
        var resizedW = (ratio * INPUT_HEIGHT).toInt()
        if (resizedW > INPUT_WIDTH) resizedW = INPUT_WIDTH
        if (resizedW < 1) resizedW = 1

        val resized = Bitmap.createScaledBitmap(bitmap, resizedW, INPUT_HEIGHT, true)

        // アスペクト比維持画像 + 幅方向のパディング
        val totalSize = INPUT_CHANNELS * INPUT_HEIGHT * INPUT_WIDTH
        // 重要: パディング領域は -1.0 で埋める (正規化後の黒)
        val floatArray = FloatArray(totalSize) { -1.0f }
        val planeSize = INPUT_HEIGHT * INPUT_WIDTH

        // ピクセル値を抽出し、RGB planar (0-255) → 正規化
        for (y in 0 until INPUT_HEIGHT) {
            for (x in 0 until resizedW) {
                val pixel = resized.getPixel(x, y)
                val r = ((pixel shr 16) and 0xFF).toFloat()
                val g = ((pixel shr 8) and 0xFF).toFloat()
                val b = (pixel and 0xFF).toFloat()

                val base = y * INPUT_WIDTH + x
                floatArray[base] = (r - PADDLE_MEAN) / PADDLE_STD
                floatArray[planeSize + base] = (g - PADDLE_MEAN) / PADDLE_STD
                floatArray[2 * planeSize + base] = (b - PADDLE_MEAN) / PADDLE_STD
            }
        }

        Log.d(TAG, "Resize: ${srcW}x${srcH} → ${resizedW}x${INPUT_HEIGHT} (padded to ${INPUT_WIDTH}x${INPUT_HEIGHT})")
        Log.d(TAG, "Input stats: min=${floatArray.minOrNull()}, max=${floatArray.maxOrNull()}, avg=${floatArray.average()}")

        val floatBuffer = ByteBuffer
            .allocateDirect(floatArray.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(floatArray)
        floatBuffer.rewind()

        // 形状: [1, 3, INPUT_HEIGHT, INPUT_WIDTH]  (NCHW)
        val shape = longArrayOf(1L, INPUT_CHANNELS.toLong(), INPUT_HEIGHT.toLong(), INPUT_WIDTH.toLong())
        return OnnxTensor.createTensor(ortEnvironment, floatBuffer, shape)
    }

    /**
     * モデル出力から文字列をデコード (CTC ベース)
     * @param logits フラット化された出力配列
     * @param shape 出力テンソルの形状 [batch, timeSteps, numClasses]
     * @return 認識された文字列
     */
    private fun decodeOutput(logits: FloatArray, shape: LongArray): String {
        require(shape.size == 3) { "Expected 3D output shape, got ${shape.size}D" }
        val batch = shape[0].toInt()
        val timeSteps = shape[1].toInt()
        val numClasses = shape[2].toInt()

        Log.d(TAG, "decodeOutput: shape=[${shape[0]}, ${shape[1]}, ${shape[2]}] (batch=$batch, timeSteps=$timeSteps, numClasses=$numClasses)")
        Log.d(TAG, "Vocab diff: numClasses=$numClasses, dictSize=${charDict.size}, diff=${numClasses - charDict.size}")

        require(logits.size == batch * timeSteps * numClasses) {
            "logits size ${logits.size} does not match ${batch * timeSteps * numClasses}"
        }
        if (batch != 1) {
            Log.w(TAG, "Batch size > 1 detected; only first batch will be processed")
        }

        // CTC greedy decode
        val labels = IntArray(timeSteps)
        var blankCount = 0
        val nonBlankClasses = mutableSetOf<Int>()
        val nonBlankChars = StringBuilder()

        for (t in 0 until timeSteps) {
            var bestClass = 0
            var bestProb = Float.NEGATIVE_INFINITY
            for (c in 0 until numClasses) {
                val idx = t * numClasses + c
                val p = logits[idx]
                if (p > bestProb) {
                    bestProb = p
                    bestClass = c
                }
            }
            labels[t] = bestClass
            if (bestClass == 0) {
                blankCount++
            } else {
                nonBlankClasses.add(bestClass)
                val dictIndex = bestClass - 1
                if (dictIndex in 0 until charDict.size) {
                    nonBlankChars.append(charDict[dictIndex])
                }
            }
        }

        val blankIndex = 0
        Log.d(TAG, "Argmax summary: blank=$blankCount/$timeSteps, nonBlankClasses=${nonBlankClasses.size} unique")
        Log.d(TAG, "Non-blank chars sequence: '$nonBlankChars'")

        // CTC decode: skip blanks, merge consecutive duplicates
        val sb = StringBuilder()
        var last = -1
        for (t in 0 until timeSteps) {
            val cur = labels[t]
            if (cur == blankIndex) {
                last = -1
                continue
            }
            if (cur == last) continue
            val dictIndex = cur - 1
            if (dictIndex in 0 until charDict.size) {
                sb.append(charDict[dictIndex])
            } else {
                Log.w(TAG, "Label $cur → dictIndex=$dictIndex out of range (0..${charDict.size-1})")
            }
            last = cur
        }

        val result = sb.toString()
        Log.d(TAG, "CTC decode: labels=[first10]=${labels.take(10).joinToString()}, final='$result'")
        return result
    }

    /**
     * 認識テキストから製品コードを抽出
     */
    private fun extractProductCodes(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val matcher = PRODUCT_CODE_PATTERN.matcher(text.uppercase())
        val codes = mutableSetOf<String>()
        while (matcher.find()) {
            val code = matcher.group()
            codes.add(code)
            Log.d(TAG, "Product code extracted: '$code'")
        }
        return codes.toList()
    }

    /**
     * メイン推論エントリーポイント
     */
    fun extractProductCode(bitmap: Bitmap): List<String> {
        Log.d(TAG, "extractProductCode() called, bitmap size: ${bitmap.width}x${bitmap.height}, preset: ${currentPreset.name}")
        if (!isModelLoaded || session == null || ortEnvironment == null) {
            Log.w(TAG, "Engine not ready (loaded=$isModelLoaded)")
            return emptyList()
        }
        if (!dictLoaded) {
            Log.e(TAG, "Character dictionary not loaded")
            return emptyList()
        }
        return try {
            val inputTensor = bitmapToInputTensor(bitmap)

            val inputs = hashMapOf(INPUT_NAME to inputTensor)
            session?.run(inputs)?.use { result ->
                // Select output key: try preset first, then fallback to actual output name
                val chosenKey = if (session!!.outputNames.contains(currentPreset.outputName)) {
                    currentPreset.outputName
                } else {
                    Log.w(TAG, "Output '${currentPreset.outputName}' not found, using first output")
                    session!!.outputNames.firstOrNull()
                        ?: run { Log.e(TAG, "No output names available"); return@use emptyList() }
                }

                @Suppress("UNCHECKED_CAST")
                val outputTensor = result.get(chosenKey)?.get() as? OnnxTensor
                    ?: run { Log.e(TAG, "Output '$chosenKey' is not OnnxTensor"); return@use emptyList() }

                Log.d(TAG, "Output shape=${java.util.Arrays.toString(outputTensor.info.shape)}, type=${outputTensor.info.type}")

                val outputArray = FloatArray(outputTensor.floatBuffer.remaining())
                outputTensor.floatBuffer.get(outputArray)

                if (outputArray.isNotEmpty()) {
                    Log.d(TAG, "Output stats: min=${outputArray.minOrNull()}, max=${outputArray.maxOrNull()}, avg=${outputArray.average()}")
                }

                if (outputArray.all { it == 0f }) {
                    Log.w(TAG, "All output values are zero")
                    return@use emptyList()
                }

                val decodedText = decodeOutput(outputArray, outputTensor.info.shape)
                Log.d(TAG, "Decoded text: '$decodedText'")

                if (decodedText.isBlank()) {
                    Log.w(TAG, "Decoded text is blank")
                    return@use emptyList()
                }

                val productCodes = extractProductCodes(decodedText)
                Log.d(TAG, "Extracted ${productCodes.size} code(s): $productCodes")
                productCodes
            } ?: emptyList()
        } catch (e: OrtException) {
            Log.e(TAG, "OrtException in extractProductCode", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Exception in extractProductCode", e)
            emptyList()
        }
    }

    /**
     * リソース解放
     */
    fun release() {
        try {
            session?.close()
            ortEnvironment?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            session = null
            ortEnvironment = null
            isModelLoaded = false
            dictLoaded = false
            charDict.clear()
        }
    }

    protected fun finalize() {
        release()
    }
}
