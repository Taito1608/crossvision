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

    companion object {
        private const val TAG = "OnnxOcrEngine"

        // 本番モデル（PaddleOCR English Recognition Model）
        private const val MODEL_NAME = "onnx/pnnx_english.onnx"

        // 入力/出力名（PaddleOCR 標準 – 実際のモデルに合わせる）
        private const val INPUT_NAME = "x"                  // モデル実際の入力名
        private const val OUTPUT_NAME = "fetch_name_0"      // モデル実際の出力名（pnnx_english.onnx は fetch_name_0 を出力）

        // 文字辞書ファイル
        private const val DICT_FILE = "onnx/dict.txt"

        // 入力画像サイズ（PaddleOCR 標準: 48x320）
        private const val INPUT_WIDTH = 320
        private const val INPUT_HEIGHT = 48
        private const val INPUT_CHANNELS = 3

        // ImageNet mean/std for BGR channels (0-255 スケール用)
        private val MEAN_255 = floatArrayOf(103.53f, 116.28f, 123.675f) // B, G, R
        private val STD_255 = floatArrayOf(57.375f, 57.12f, 58.395f)

        // 製品コード抽出用正規表現: 大文字英数字3文字以上 + 任意のハイフンと数字
        private val PRODUCT_CODE_PATTERN = Pattern.compile("[A-Z0-9]{3,}-?[0-9]*")
    }

    private var session: OrtSession? = null
    private var ortEnvironment: OrtEnvironment? = null
    private var isModelLoaded = false

    // 文字辞書（インデックス → 文字）
    private val charDict = mutableListOf<String>()
    private var dictLoaded = false

    /**
     * 文字辞書を assets から読み込む
     */
    private fun loadCharDict(): Boolean {
        return try {
            charDict.clear()
            context.assets.open(DICT_FILE).use { inputStream ->
                inputStream.bufferedReader().forEachLine { line ->
                    charDict.add(line)
                }
            }
            dictLoaded = charDict.isNotEmpty()
            Log.d(TAG, "Char dict loaded: ${charDict.size} characters")
            dictLoaded
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load char dict", e)
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
        Log.d(TAG, "loadModel() called, model: $MODEL_NAME")
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
                context.assets.open(MODEL_NAME).close()
                Log.d(TAG, "Model file exists in assets: $MODEL_NAME")
            } catch (e: Exception) {
                Log.e(TAG, "Model file NOT found in assets: $MODEL_NAME", e)
                throw Exception("Model file '$MODEL_NAME' not found in assets/. Please place the PaddleOCR English ONNX model.")
            }

            // モデルの読み込み
            val modelBytes = loadModelFromAssets(MODEL_NAME)
            Log.d(TAG, "Model loaded from assets, size: ${modelBytes.size} bytes")

            // セッションの作成
            val sessionOptions = OrtSession.SessionOptions()
            session = ortEnvironment?.createSession(modelBytes, sessionOptions)

            if (session != null) {
                Log.d(TAG, "Session created successfully")
                Log.d(TAG, "Input names: ${session?.inputNames}")
                Log.d(TAG, "Output names: ${session?.outputNames}")
                Log.d(TAG, "Input info: ${session?.inputInfo}")
                Log.d(TAG, "Output info: ${session?.outputInfo}")
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
     * 1. リサイズ (INPUT_HEIGHT x INPUT_WIDTH)
     * 2. BGR チャンネル順（PaddleOCR標準）
     * 3. 正規化 (0-255 → mean/std で標準化)
     * 4. NCHW 形式（planar）に変換
     */
    private fun bitmapToInputTensor(bitmap: Bitmap): OnnxTensor {
        // 画像をリサイズ
        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_WIDTH, INPUT_HEIGHT, true)

        val totalSize = INPUT_CHANNELS * INPUT_HEIGHT * INPUT_WIDTH
        val floatArray = FloatArray(totalSize)
        val planeSize = INPUT_HEIGHT * INPUT_WIDTH  // 1チャンネルあたりの画素数

        // ピクセル値を抽出し、BGR planar (0-255)
        for (y in 0 until INPUT_HEIGHT) {
            for (x in 0 until INPUT_WIDTH) {
                val pixel = resized.getPixel(x, y)
                val r = ((pixel shr 16) and 0xFF).toFloat()   // 0-255
                val g = ((pixel shr 8) and 0xFF).toFloat()
                val b = (pixel and 0xFF).toFloat()

                val base = y * INPUT_WIDTH + x
                floatArray[base] = b                       // B チャンネル
                floatArray[planeSize + base] = g           // G チャンネル
                floatArray[2 * planeSize + base] = r       // R チャンネル
            }
        }

        // ImageNet mean/std 正規化 (0-255 -> 標準化)
        for (i in 0 until planeSize) {
            floatArray[i] = (floatArray[i] - MEAN_255[0]) / STD_255[0]
            floatArray[planeSize + i] = (floatArray[planeSize + i] - MEAN_255[1]) / STD_255[1]
            floatArray[2 * planeSize + i] = (floatArray[2 * planeSize + i] - MEAN_255[2]) / STD_255[2]
        }

        // Debug: log input stats
        Log.d(TAG, "Input stats: min=${floatArray.minOrNull()}, max=${floatArray.maxOrNull()}, avg=${floatArray.average()}")
        Log.d(TAG, "First 20 input values (B): ${floatArray.take(20).joinToString { "%.4f".format(it) }}")
        Log.d(TAG, "First 20 G values: ${floatArray.slice(planeSize until planeSize+20).joinToString { "%.4f".format(it) }}")
        Log.d(TAG, "First 20 R values: ${floatArray.slice(2*planeSize until 2*planeSize+20).joinToString { "%.4f".format(it) }}")

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

        Log.d(TAG, "decodeOutput: shape=${java.util.Arrays.toString(shape)}, batch=$batch, timeSteps=$timeSteps, numClasses=$numClasses")
        Log.d(TAG, "Vocab diff: numClasses=$numClasses, dictSize=${charDict.size}")

        require(logits.size == batch * timeSteps * numClasses) {
            "logits size ${logits.size} does not match shape[0]*shape[1]*shape[2] = ${batch * timeSteps * numClasses}"
        }
        if (batch != 1) {
            Log.w(TAG, "Batch size > 1 detected; only first batch will be processed")
        }

        // 単一バッチについて argmax でラベル列を得る
        val labels = IntArray(timeSteps)
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
            Log.d(TAG, "t=$t: argmax class=$bestClass prob=$bestProb")
        }

        // blank index を決定 (標準 CTC では 0)
        val blankIndex = 0
        Log.d(TAG, "Using blankIndex=$blankIndex")

        // CTC デコード: blank をスキップ、連続重複を除去
        val sb = StringBuilder()
        var last = -1
        for (t in 0 until timeSteps) {
            val cur = labels[t]
            if (cur == blankIndex) {
                last = -1
                continue
            }
            if (cur == last) continue
            // dict 対応: blankIndex=0 → class 0=blank, class 1..N → dict[0..N-1]
            val dictIndex = cur - 1
            if (dictIndex in 0 until charDict.size) {
                sb.append(charDict[dictIndex])
            } else {
                Log.w(TAG, "Label $cur → dictIndex=$dictIndex out of range (0..${charDict.size-1})")
            }
            last = cur
        }

        val result = sb.toString()
        Log.d(TAG, "CTC decode: raw labels=${labels.take(20).joinToString()}, final='$result'")
        return result
    }

    /**
     * 認識テキストから製品コードを抽出
     */
    private fun extractProductCodes(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val matcher = PRODUCT_CODE_PATTERN.matcher(text)
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
        Log.d(TAG, "extractProductCode() called, bitmap size: ${bitmap.width}x${bitmap.height}")
        if (!isModelLoaded || session == null || ortEnvironment == null) {
            Log.w(TAG, "Engine not ready (loaded=$isModelLoaded)")
            return emptyList()
        }
        if (!dictLoaded) {
            Log.e(TAG, "Character dictionary not loaded")
            return emptyList()
        }
        return try {
            Log.d(TAG, "Creating input tensor (${INPUT_WIDTH}x${INPUT_HEIGHT})")
            val inputTensor = bitmapToInputTensor(bitmap)
            Log.d(TAG, "Input tensor created, shape: ${inputTensor.info.shape}")

            val inputs = hashMapOf(INPUT_NAME to inputTensor)
            Log.d(TAG, "Running session.run() with input: $INPUT_NAME")

            session?.run(inputs)?.use { result ->
                Log.d(TAG, "Session run completed")

                // Choose the expected output name; if not in session's outputNames, pick the first.
                val chosenKey = if (session!!.outputNames.contains(OUTPUT_NAME)) {
                    OUTPUT_NAME
                } else {
                    Log.w(TAG, "OUTPUT_NAME '$OUTPUT_NAME' not in session outputs. Trying first output...")
                    session!!.outputNames.firstOrNull()
                        ?: run { Log.e(TAG, "No output names available from session"); return@use emptyList() }
                }

                Log.d(TAG, "Using output key: '$chosenKey'")

                val optionalVal = result.get(chosenKey)
                if (optionalVal == null || !optionalVal.isPresent) {
                    Log.e(TAG, "Optional value for key '$chosenKey' is empty or null")
                    return@use emptyList()
                }

                @Suppress("UNCHECKED_CAST")
                val outputTensor = optionalVal.get() as? OnnxTensor
                    ?: run {
                        Log.e(TAG, "Output value is not OnnxTensor (type=${optionalVal.get().javaClass.simpleName})")
                        return@use emptyList()
                    }

                Log.d(TAG, "Output tensor obtained: shape=${java.util.Arrays.toString(outputTensor.info.shape)}, type=${outputTensor.info.type}")

                val floatBuffer = outputTensor.floatBuffer
                val outputArray = FloatArray(floatBuffer.remaining())
                floatBuffer.get(outputArray)

                Log.d(TAG, "Output array size: ${outputArray.size}")
                if (outputArray.isNotEmpty()) {
                    Log.d(TAG, "Output stats: min=${outputArray.minOrNull()}, max=${outputArray.maxOrNull()}, avg=${outputArray.average()}")
                    Log.d(TAG, "First 20 values: ${outputArray.take(20).joinToString { "%.6f".format(it) }}")
                }

                if (outputArray.all { it == 0f }) {
                    Log.w(TAG, "All output values are zero - model may not have produced valid output")
                    return@use emptyList()
                }

                val decodedText = decodeOutput(outputArray, outputTensor.info.shape)
                Log.d(TAG, "Decoded text: '$decodedText'")

                if (decodedText.isBlank()) {
                    Log.w(TAG, "Decoded text is blank")
                    return@use emptyList()
                }

                val productCodes = extractProductCodes(decodedText)
                Log.d(TAG, "Extracted ${productCodes.size} product code(s): $productCodes")

                productCodes
            } ?: emptyList()
        } catch (e: OrtException) {
            Log.e(TAG, "OrtException in extractProductCode", e)
            Log.e(TAG, "Model shape mismatch or inference error: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Exception in extractProductCode", e)
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}, message: ${e.message}")
            e.printStackTrace()
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
