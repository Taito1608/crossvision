package com.example.solution_development

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * OnnxOcrEngine - ONNX PaddleOCR v3 実装
 * 
 * 特徴:
 * - 8 パターン検証：色反転 (2) × 回転角度 (4)
 * - 固定サイズ 320×48 px 強制リサイズ
 * - 正規化：(x-0.5)/0.5（ImageNet 非使用）
 * - CTC decode: blank スキップ + 連続同一文字圧縮
 */
class OnnxOcrEngine(private val context: Context) {
    companion object {
        private const val TAG = "OnnxOcrEngine"
        const val FIXED_WIDTH = 320
        const val FIXED_HEIGHT = 48
        private val ALLOWED_CHARS = listOf('0','1','2','3','4','5','6','7','8','9',
            'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
            'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
            '+','-','.','/','_',' ')
    }

    private var session: Any? = null
    private val charDict = mutableListOf<String>()

    @Synchronized fun loadModel(): Boolean {
        return try {
            Log.d(TAG, "ONNX モデル初期化開始")
            if (!loadCharDictionary()) {
                Log.e(TAG, "辞書読み込みに失敗")
                return false
            }
            Log.d(TAG, "辞書読み込み完了：${charDict.size}文字")

            val modelPath = "models/en_PP-OCRv3_rec.onnx"
            val modelFile = copyAssetToCache(modelPath)

            val optionsClass = Class.forName("org.onnxruntime.SessionOptions")!!
            val options: Any = optionsClass.getDeclaredConstructor().newInstance()!!
            optionsClass.getMethod("setIntraOpNumThreads", Int::class.java).invoke(options, 1)
            optionsClass.getMethod("setInterOpNumThreads", Int::class.java).invoke(options, 1)

            val sessionClass = Class.forName("org.onnxruntime.InferenceSession")!!
            session = sessionClass.getDeclaredConstructor(String::class.java, optionsClass)
                .newInstance(modelFile.absolutePath, options)

            Log.d(TAG, "ONNX セッション初期化完了")
            true
        } catch (e: Exception) {
            Log.e(TAG, "初期化エラー", e)
            false
        }
    }

    private fun copyAssetToCache(assetPath: String): File {
        val cacheFile = File(context.cacheDir, assetPath.substringAfterLast('/'))
        if (!cacheFile.exists()) {
            context.assets.open(assetPath).use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return cacheFile
    }

    private fun loadCharDictionary(): Boolean {
        return try {
            charDict.clear()
            charDict.add("blank")

            val dictPath = "models/en_PP-OCRv3_dict.txt"
            val lines = context.assets.open(dictPath).bufferedReader().readLines()
            for (line in lines) {
                val trimmed = line.trim { it <= ' ' }
                if (trimmed.isNotEmpty()) charDict.add(trimmed)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "辞書読み込み例外：${e.message}")
            setupDefaultDictionary()
            true
        }
    }

    private fun setupDefaultDictionary() {
        val defaultDict = listOf(" ", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
            "-", ".", "_", "!","?")
        charDict.addAll(defaultDict)
        Log.w(TAG, "デフォルト辞書を使用：${charDict.size}文字")
    }

    @Synchronized fun extractProductCode(bitmap: Bitmap): List<String> {
        return try {
            if (session == null) {
                Log.e(TAG, "エンジンが初期化されていません")
                emptyList()
            } else {
                val recognizedText = recognize(bitmap)
                val cleaned = recognizedText.toCharArray().filter { ALLOWED_CHARS.contains(it) }.joinToString("").trim()
                if (cleaned.isNotEmpty()) listOf(cleaned) else emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Product code extraction error", e)
            emptyList()
        }
    }

    @Synchronized private fun recognize(bitmap: Bitmap): String {
        return try {
            if (session == null) {
                Log.e(TAG, "エンジンが初期化されていません")
                return ""
            }

            Log.d(TAG, "認識開始：${bitmap.width}x${bitmap.height}")

            var bestText = ""
            var bestScore = 0f

            for (inverted in listOf(false, true)) {
                for (rotation in listOf(0, 90, 180, 270)) {
                    val processed = prepareImage(bitmap, rotation, inverted)
                    try {
                        val (text, score) = runInference(processed)
                        if (score > bestScore) {
                            bestText = text
                            bestScore = score
                            Log.d(TAG, "New best: '$text' (sc:$score) rot:$rotation inv:$inverted")
                        }
                    } finally {
                        processed.recycle()
                    }
                }
            }

            Log.d(TAG, "認識完了：'$bestText'")
            bestText
        } catch (e: Exception) {
            Log.e(TAG, "認識エラー", e)
            ""
        }
    }

    private fun prepareImage(bitmap: Bitmap, rotate: Int, invert: Boolean): Bitmap {
        var result = bitmap
        if (invert) result = invertColors(result)
        if (rotate != 0) result = rotateBitmap(result, rotate)
        return resizeToFixedSize(result, FIXED_WIDTH, FIXED_HEIGHT)
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun invertColors(bitmap: Bitmap): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = 255 - (pixel and 0xFF0000 shr 16)
            val g = 255 - (pixel and 0x00FF00 shr 8)
            val b = 255 - (pixel and 0xFF)
            pixels[i] = (-0x1000000 or (r shl 16) or (g shl 8) or b) and 0xFFFFFFFF.toInt()
        }
        return Bitmap.createBitmap(pixels, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    }

    private fun resizeToFixedSize(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    @Synchronized private fun runInference(bitmap: Bitmap): Pair<String, Float> {
        val sess = session ?: throw IllegalStateException("session is null")

        val inputBuffer = bitmapToOnnxInput(bitmap)
        try {
            val ortValueClass = Class.forName("org.onnxruntime.OrtValue")!!
            val createMethod = ortValueClass.getMethod("createForTensor",
                Any::class.java, Long::class.java, LongArray::class.java)

            val input = createMethod.invoke(null,
                inputBuffer,
                FIXED_HEIGHT.toLong(),
                longArrayOf(FIXED_HEIGHT.toLong(), FIXED_WIDTH.toLong(), 3.toLong()))!!

            val inputsList = Class.forName("java.util.Arrays")!!
                .getMethod("asList", Any::class.java)
                .invoke(null, input)!!

            val runMethod = sess::class.java.getMethod("run", Class.forName("java.util.List"))
            val results = runMethod.invoke(sess, inputsList) as List<*>

            val ortValue: Any = results[0]!!
            val floatBuffer = ortValue::class.java.getField("floatBuffer").get(ortValue) as java.nio.FloatBuffer

            val decoded = decodeCTC(floatBuffer)
            val score = computeConfidence(floatBuffer)

            return Pair(decoded, score)
        } finally {
            // inputBuffer は auto-close 不要
        }
    }

    private fun bitmapToOnnxInput(bitmap: Bitmap): java.nio.FloatBuffer {
        val floatBuffer = java.nio.FloatBuffer.allocate(FIXED_WIDTH * FIXED_HEIGHT * 3)
        val pixels = IntArray(FIXED_WIDTH * FIXED_HEIGHT)
        bitmap.getPixels(pixels, 0, FIXED_WIDTH, 0, 0, FIXED_WIDTH, FIXED_HEIGHT)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = ((pixel and 0xFF0000) ushr 16) / 255f
            val g = ((pixel and 0xFF00) ushr 8) / 255f
            val b = (pixel and 0xFF) / 255f

            floatBuffer.put((r - 0.5f) / 0.5f)
            floatBuffer.put((g - 0.5f) / 0.5f)
            floatBuffer.put((b - 0.5f) / 0.5f)
        }

        floatBuffer.rewind()
        return floatBuffer
    }

    private fun decodeCTC(probs: java.nio.FloatBuffer): String {
        val sb = StringBuilder()
        var lastIdx = -1
        val charCount = charDict.size
        val totalElements = probs.remaining()
        val seqLength = totalElements / charCount

        for (i in 0 until seqLength) {
            var maxIdx = 0
            var maxProb = Float.MIN_VALUE

            for (j in 0 until charCount) {
                val prob = probs.get(i * charCount + j)
                if (prob > maxProb) {
                    maxProb = prob
                    maxIdx = j
                }
            }

            if (maxIdx != 0 && maxIdx != lastIdx) {
                if (maxIdx < charDict.size) {
                    sb.append(charDict[maxIdx])
                }
            }
            lastIdx = maxIdx
        }

        return sb.toString()
    }

    private fun computeConfidence(probs: java.nio.FloatBuffer): Float {
        var sum = 0f
        var count = 0
        val charCount = charDict.size
        val totalElements = probs.remaining()
        val seqLength = totalElements / charCount

        for (i in 0 until seqLength) {
            var maxProb = 0f
            for (j in 0 until charCount) {
                val prob = probs.get(i * charCount + j)
                if (prob > maxProb) maxProb = prob
            }
            sum += maxProb
            count++
        }

        return if (count > 0) sum / count.toFloat() else 0f
    }

    @Synchronized fun release() {
        session?.let { sess ->
            try {
                sess::class.java.getMethod("close").invoke(sess)
            } catch (e: Exception) {
                Log.e(TAG, "Close error", e)
            }
        }
        session = null
        charDict.clear()
        Log.d(TAG, "OnnxOcrEngine 解放済み")
    }
}
