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
        // Official PP-OCRv3 English PP-OCR model (converted from PaddlePaddle)
        PP_OCRV3_OFFICIAL(
            modelFile = "onnx/en_PP-OCRv3_rec.onnx",
            dictFile = "onnx/en_PP-OCRv3_dict.txt",
            outputName = "fetch_name_0",   // default output name from paddle2onnx conversion
            numClasses = 97           // 95 en_dict chars + 1 space + 1 blank = 97
        )
    }

    companion object {
        private const val TAG = "OnnxOcrEngine"

        // Recognition model input spec
        private const val INPUT_NAME = "x"
        private const val INPUT_WIDTH = 320
        private const val INPUT_HEIGHT = 48
        private const val INPUT_CHANNELS = 3

        // PaddleOCR normalization: (pixel - 127.5) / 127.5
        private const val PADDLE_MEAN = 127.5f
        private const val PADDLE_STD = 127.5f

        // Detection model: ImageNet normalization
        private val DET_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val DET_STD = floatArrayOf(0.229f, 0.224f, 0.225f)
        private const val DET_MAX_SIDE = 960
        private const val DET_THRESH = 0.3f
        private const val DET_PAD_EXPAND = 30  // pixels to expand detected box

        // 製品コード抽出用正規表現: 大文字英数字3文字以上 + 任意のハイフンと数字
        private val PRODUCT_CODE_PATTERN = Pattern.compile("[A-Z0-9]{3,}-?[0-9]*")

        // Allowed characters for product codes (filter step)
        private val ALLOWED_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-+# "
    }

    // Recognition session
    private var recSession: OrtSession? = null
    // Detection session
    private var detSession: OrtSession? = null
    private var ortEnvironment: OrtEnvironment? = null
    private var isModelLoaded = false

    // 選択中のモデル設定
    private var currentPreset: ModelPreset = ModelPreset.PP_OCRV3_OFFICIAL

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
     * ONNXモデルをロード（det + rec）
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

            // === Load Recognition model ===
            try {
                context.assets.open(currentPreset.modelFile).close()
                Log.d(TAG, "Rec model file exists: ${currentPreset.modelFile}")
            } catch (e: Exception) {
                Log.e(TAG, "Rec model file NOT found: ${currentPreset.modelFile}", e)
                throw Exception("Model file '${currentPreset.modelFile}' not found in assets.")
            }
            val recModelBytes = loadModelFromAssets(currentPreset.modelFile)
            Log.d(TAG, "Rec model loaded, size: ${recModelBytes.size} bytes")
            val sessionOptions = OrtSession.SessionOptions()
            recSession = ortEnvironment?.createSession(recModelBytes, sessionOptions)
            Log.d(TAG, "Rec session created. Output names: ${recSession?.outputNames}")

            // === Load Detection model (DB text detection) ===
            val detModelPath = "onnx/det.onnx"
            try {
                context.assets.open(detModelPath).close()
                Log.d(TAG, "Det model file exists: $detModelPath")
                val detModelBytes = loadModelFromAssets(detModelPath)
                Log.d(TAG, "Det model loaded, size: ${detModelBytes.size} bytes")
                detSession = ortEnvironment?.createSession(detModelBytes, sessionOptions)
                Log.d(TAG, "Det session created. Output names: ${detSession?.outputNames}")
            } catch (e: Exception) {
                Log.w(TAG, "Det model not found, falling back to full-image recognition: ${e.message}")
                detSession = null
            }

            isModelLoaded = recSession != null
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
     * メイン推論エントリーポイント: det → rec → filter
     * If det model is loaded, detect text regions first, then recognize each.
     * Otherwise, recognize the full image directly.
     */
    fun extractProductCode(bitmap: Bitmap): List<String> {
        Log.d(TAG, "extractProductCode() called, bitmap=${bitmap.width}x${bitmap.height}, preset=${currentPreset.name}")
        if (!isModelLoaded || recSession == null || ortEnvironment == null) {
            Log.w(TAG, "Engine not ready (loaded=$isModelLoaded)")
            return emptyList()
        }
        if (!dictLoaded) {
            Log.e(TAG, "Character dictionary not loaded")
            return emptyList()
        }

        return try {
            if (detSession != null) {
                // === det → rec pipeline ===
                val regions = detectTextRegions(bitmap)
                Log.d(TAG, "Detected ${regions.size} text regions")
                if (regions.isEmpty()) return emptyList()

                val allCodes = mutableSetOf<String>()
                for ((i, region) in regions.withIndex()) {
                    val crop = cropBitmap(bitmap, region)
                    if (crop == null) continue
                    // Expand crop with padding
                    val padded = padCrop(crop)
                    val text = recognizeBitmap(padded)
                    Log.d(TAG, "Region $i: '${text}' (crop=${crop.width}x${crop.height})")
                    if (text.isNotBlank()) {
                        // Filter to allowed characters
                        val filtered = filterProductCode(text)
                        if (filtered.isNotBlank()) {
                            val codes = extractProductCodes(filtered)
                            allCodes.addAll(codes)
                        }
                    }
                }
                Log.d(TAG, "All extracted codes: $allCodes")
                allCodes.toList()
            } else {
                // === Fallback: recognition only (full image) ===
                Log.d(TAG, "No det model, using full image recognition")
                val text = recognizeBitmap(bitmap)
                Log.d(TAG, "Decoded: '$text'")
                if (text.isBlank()) return emptyList()
                val filtered = filterProductCode(text)
                extractProductCodes(filtered)
            }
        } catch (e: OrtException) {
            Log.e(TAG, "OrtException in extractProductCode", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Exception in extractProductCode", e)
            emptyList()
        }
    }

    /**
     * Detect text regions using DB text detection model.
     * Returns list of [x1, y1, x2, y2] bounding boxes in original image coordinates.
     */
    private fun detectTextRegions(bitmap: Bitmap): List<IntArray> {
        val srcH = bitmap.height
        val srcW = bitmap.width

        // Resize for detection (max side = DET_MAX_SIDE, multiple of 32)
        val scale = minOf(DET_MAX_SIDE.toFloat() / srcH, DET_MAX_SIDE.toFloat() / srcW)
        val newH = ((srcH * scale).toInt() + 31) / 32 * 32
        val newW = ((srcW * scale).toInt() + 31) / 32 * 32

        val resized = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        val pixels = IntArray(newW * newH)
        resized.getPixels(pixels, 0, newW, 0, 0, newW, newH)

        // Build NCHW float array with ImageNet normalization
        val totalSize = 3 * newH * newW
        val floatArray = FloatArray(totalSize)
        val planeSize = newH * newW
        for (y in 0 until newH) {
            for (x in 0 until newW) {
                val pixel = pixels[y * newW + x]
                val r = ((pixel shr 16) and 0xFF).toFloat() / 255f
                val g = ((pixel shr 8) and 0xFF).toFloat() / 255f
                val b = (pixel and 0xFF).toFloat() / 255f
                val base = y * newW + x
                floatArray[base] = (r - DET_MEAN[0]) / DET_STD[0]
                floatArray[planeSize + base] = (g - DET_MEAN[1]) / DET_STD[1]
                floatArray[2 * planeSize + base] = (b - DET_MEAN[2]) / DET_STD[2]
            }
        }

        val env = ortEnvironment ?: return emptyList()
        val inputBuffer = ByteBuffer.allocateDirect(totalSize * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        inputBuffer.put(floatArray); inputBuffer.rewind()
        val shape = longArrayOf(1L, 3L, newH.toLong(), newW.toLong())
        val inputTensor = OnnxTensor.createTensor(env, inputBuffer, shape)
        val inputName = detSession?.inputNames?.firstOrNull() ?: "x"

        val outputs = detSession?.run(hashMapOf(inputTensor)) ?: return emptyList()
        inputTensor.close()

        // Output: [1, 1, H, W] text segmentation map
        val outputTensor = useWithClose(outputs) { outs ->
            val key = detSession!!.outputNames.firstOrNull() ?: return@useWithClose null
            @Suppress("UNCHECKED_CAST")
            (outs.get(key)?.get() as? OnnxTensor)
        } ?: return emptyList()

        val outShape = outputTensor.info.shape
        val outH = outShape[2].toInt()
        val outW = outShape[3].toInt()
        val outPixels = FloatArray(outputTensor.floatBuffer.remaining())
        outputTensor.floatBuffer.get(outPixels)

        // Threshold to get binary map
        val binary = Array(outH) { BooleanArray(outW) }
        for (y in 0 until outH) {
            for (x in 0 until outW) {
                binary[y][x] = outPixels[y * outW + x] > DET_THRESH
            }
        }

        // Simple connected component finder (flood fill)
        val visited = Array(outH) { BooleanArray(outW) }
        val boxes = mutableListOf<IntArray>()
        val scaleX = srcW.toFloat() / outW
        val scaleY = srcH.toFloat() / outH

        for (y in 0 until outH) {
            for (x in 0 until outW) {
                if (binary[y][x] && !visited[y][x]) {
                    // BFS flood fill
                    val queue = ArrayDeque<Pair<Int, Int>>()
                    queue.add(Pair(x, y))
                    visited[y][x] = true
                    var minX = x; var maxX = x; var minY = y; var maxY = y
                    var count = 0
                    while (queue.isNotEmpty()) {
                        val (cx, cy) = queue.removeFirst()
                        count++
                        if (cx < minX) minX = cx; if (cx > maxX) maxX = cx
                        if (cy < minY) minY = cy; if (cy > maxY) maxY = cy
                        for (dy in -1..1) {
                            for (dx in -1..1) {
                                val nx = cx + dx; val ny = cy + dy
                                if (nx in 0 until outW && ny in 0 until outH && binary[ny][nx] && !visited[ny][nx]) {
                                    visited[ny][nx] = true
                                    queue.add(Pair(nx, ny))
                                }
                            }
                        }
                    }
                    if (count >= 5) {  // minimum component size
                        val x1 = (minX * scaleX).toInt() - DET_PAD_EXPAND
                        val y1 = (minY * scaleY).toInt() - DET_PAD_EXPAND
                        val x2 = (maxX * scaleX).toInt() + DET_PAD_EXPAND
                        val y2 = (maxY * scaleY).toInt() + DET_PAD_EXPAND
                        boxes.add(intArrayOf(
                            x1.coerceIn(0, srcW - 1),
                            y1.coerceIn(0, srcH - 1),
                            x2.coerceIn(0, srcW - 1),
                            y2.coerceIn(0, srcH - 1)
                        ))
                    }
                }
            }
        }
        outputTensor.close()
        return boxes
    }

    /** Crop bitmap to region [x1, y1, x2, y2] */
    private fun cropBitmap(bitmap: Bitmap, region: IntArray): Bitmap? {
        return try {
            val x1 = region[0]; val y1 = region[1]; val x2 = region[2]; val y2 = region[3]
            if (x2 <= x1 || y2 <= y1) return null
            Bitmap.createBitmap(bitmap, x1, y1, x2 - x1, y2 - y1)
        } catch (e: Exception) {
            Log.e(TAG, "cropBitmap failed", e)
            null
        }
    }

    /** Add gray padding around a crop for better recognition */
    private fun padCrop(crop: Bitmap): Bitmap {
        val pad = 10
        val newW = crop.width + 2 * pad
        val newH = crop.height + 2 * pad
        val result = Bitmap.createBitmap(newW, newH, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        canvas.drawColor(android.graphics.Color.rgb(128, 128, 128))
        canvas.drawBitmap(crop, pad.toFloat(), pad.toFloat(), null)
        return result
    }

    /** Run recognition on a bitmap and return decoded text */
    private fun recognizeBitmap(bitmap: Bitmap): String {
        if (recSession == null || ortEnvironment == null) return ""
        val inputTensor = bitmapToInputTensor(bitmap)
        val inputs = hashMapOf(INPUT_NAME to inputTensor)
        return recSession?.run(inputs)?.use { result ->
            val chosenKey = if (recSession!!.outputNames.contains(currentPreset.outputName)) {
                currentPreset.outputName
            } else {
                recSession!!.outputNames.firstOrNull() ?: return@use ""
            }
            @Suppress("UNCHECKED_CAST")
            val outputTensor = result.get(chosenKey)?.get() as? OnnxTensor
                ?: return@use ""
            val shape = outputTensor.info.shape
            val outputArray = FloatArray(outputTensor.floatBuffer.remaining())
            outputTensor.floatBuffer.get(outputArray)
            decodeOutput(outputArray, shape)
        } ?: ""
    }

    /** Filter text to only allowed product code characters (preserving mixed case) */
    private fun filterProductCode(text: String): String {
        val sb = StringBuilder()
        for (c in text) {
            if (ALLOWED_CHARS.contains(c)) {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    /** Auto-close helper */
    private inline fun <T> useWithClose(closeable: AutoCloseable, block: (AutoCloseable) -> T): T {
        return try {
            block(closeable)
        } finally {
            try { closeable.close() } catch (_: Exception) {}
        }
    }

    /**
     * リソース解放
     */
    fun release() {
        try {
            recSession?.close()
            detSession?.close()
            ortEnvironment?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            recSession = null
            detSession = null
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
