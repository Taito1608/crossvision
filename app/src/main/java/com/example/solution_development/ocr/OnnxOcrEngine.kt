package com.example.solution_development.ocr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
            numClasses = 438
        ),
        PP_OCR_V3_RAPIDOCR(
            modelFile = "onnx/ppocrv3_rec.onnx",
            dictFile = "onnx/ppocrv3_dict.txt",
            outputName = "softmax_2.tmp_0",
            numClasses = 97
        ),
        PP_OCR_V5_CUSTOM(
            modelFile = "onnx/rec_v5.onnx",
            dictFile = "onnx/custom_dict.txt",
            outputName = "softmax_2.tmp_0",
            numClasses = 438
        )
    }

    companion object {
        private const val TAG = "OnnxOcrEngine"

        // Recognition model input spec
        private const val REC_INPUT_NAME = "x"
        private const val REC_INPUT_WIDTH = 320
        private const val REC_INPUT_HEIGHT = 48
        private const val REC_INPUT_CHANNELS = 3

        // Detection model input spec
        private const val DET_INPUT_NAME = "x"
        // det.onnx: ImageNet normalization, aspect-ratio resize, max side = 960, pad to multiple of 32
        private const val DET_MAX_SIDE = 960

        // Recognition model normalization (PaddleOCR)
        private const val PADDLE_MEAN = 127.5f
        private const val PADDLE_STD = 127.5f

        // Detection model normalization (ImageNet) — det.onnx was trained with this
        private val DET_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val DET_STD = floatArrayOf(0.229f, 0.224f, 0.225f)

        // Detection post-processing parameters
        private const val DET_DB_THRESH = 0.3f
        private const val DET_PAD_EXPAND = 30  // pixels to expand detected box
        private const val DET_MIN_COMPONENT_SIZE = 5

        // Product code extraction pattern
        private val PRODUCT_CODE_PATTERN = Pattern.compile("[A-Z0-9]{3,}-?[0-9]*")

        // Throttle: run detection every N ms to avoid overwhelming the device
        const val DETECTION_INTERVAL_MS = 500L
    }

    // Recognition session
    private var recSession: OrtSession? = null
    // Detection session
    private var detSession: OrtSession? = null
    private var ortEnvironment: OrtEnvironment? = null

    private var isRecLoaded = false
    private var isDetLoaded = false

    private var currentPreset: ModelPreset = ModelPreset.PP_OCR_V3_RAPIDOCR

    private val charDict = mutableListOf<String>()
    private var dictLoaded = false

    /**
     * Bounding box for a detected text region
     */
    data class TextRegion(
        val rect: Rect,
        val confidence: Float
    )

    fun setModelPreset(preset: ModelPreset) {
        release()
        currentPreset = preset
        charDict.clear()
        dictLoaded = false
    }

    // ─── Character Dictionary ──────────────────────────────────

    private fun loadCharDict(): Boolean {
        return try {
            charDict.clear()
            context.assets.open(currentPreset.dictFile).use { inputStream ->
                inputStream.bufferedReader().forEachLine { line ->
                    charDict.add(line)
                }
            }
            dictLoaded = charDict.isNotEmpty()
            Log.d(TAG, "Char dict loaded: ${charDict.size} chars from ${currentPreset.dictFile}")
            dictLoaded
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load char dict from ${currentPreset.dictFile}", e)
            false
        }
    }

    // ─── Asset Loading ─────────────────────────────────────────

    /**
     * Copy an asset file to internal storage and return the absolute path.
     * This avoids loading the entire model into the Java heap, which is
     * critical for large models (>50MB) on emulators with limited heap.
     */
    private fun copyAssetToInternalStorage(assetPath: String): String {
        val outFile = File(context.filesDir, assetPath.substringAfterLast("/"))
        if (outFile.exists() && outFile.length() > 0) {
            Log.d(TAG, "Model already cached: ${outFile.absolutePath} (${outFile.length()} bytes)")
            return outFile.absolutePath
        }
        Log.i(TAG, "Copying asset $assetPath → ${outFile.absolutePath}")
        context.assets.open(assetPath).use { input ->
            outFile.parentFile?.mkdirs()
            outFile.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
        }
        Log.i(TAG, "Model cached: ${outFile.absolutePath} (${outFile.length()} bytes)")
        return outFile.absolutePath
    }

    // ─── Model Loading ─────────────────────────────────────────

    fun loadModel(): Boolean {
        Log.i(TAG, "loadModel() called, preset: ${currentPreset.name}")
        return try {
            if (!loadCharDict()) {
                Log.e(TAG, "Failed to load character dictionary")
                return false
            }

            ortEnvironment = OrtEnvironment.getEnvironment()
            Log.i(TAG, "OrtEnvironment created")

            // Load recognition model — copy to internal storage first, then load by path
            // This avoids OOM from loading the entire model into the Java heap
            try {
                val recModelPath = copyAssetToInternalStorage(currentPreset.modelFile)
                Log.i(TAG, "Rec model path: $recModelPath")

                val recSessionOptions = OrtSession.SessionOptions().apply {
                    setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                    setIntraOpNumThreads(2)
                }
                recSession = ortEnvironment?.createSession(recModelPath, recSessionOptions)
                isRecLoaded = recSession != null
                Log.i(TAG, "Rec session created: $isRecLoaded, inputs=${recSession?.inputNames}, outputs=${recSession?.outputNames}")
            } catch (e: Exception) {
                Log.e(TAG, "Rec model load failed: ${currentPreset.modelFile}", e)
                throw Exception("Model '${currentPreset.modelFile}' not found or load failed.", e)
            }

            // Load detection model (det.onnx — same model that worked in soya-ocr)
            try {
                val detPath = "onnx/det.onnx"
                context.assets.open(detPath).close()  // verify asset exists
                val detModelPath = copyAssetToInternalStorage(detPath)
                Log.i(TAG, "Det model path: $detModelPath")

                val detSessionOptions = OrtSession.SessionOptions().apply {
                    setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                    setIntraOpNumThreads(2)
                }
                detSession = ortEnvironment?.createSession(detModelPath, detSessionOptions)
                isDetLoaded = detSession != null
                Log.i(TAG, "Det session created: $isDetLoaded, inputs=${detSession?.inputNames}, outputs=${detSession?.outputNames}")
            } catch (e: Exception) {
                Log.w(TAG, "Det model not found, detection disabled: ${e.message}")
                isDetLoaded = false
            }

            Log.i(TAG, "Model loading complete: rec=$isRecLoaded, det=$isDetLoaded")
            isRecLoaded
        } catch (e: OrtException) {
            Log.e(TAG, "OrtException in loadModel", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception in loadModel", e)
            false
        }
    }

    // ─── Detection: det_v5.onnx → TextRegion list ─────────────

    /**
     * Run detection model on the full image and return text regions.
     * Uses DBNet post-processing: threshold → find contours → unclip boxes.
     */
    fun detectTextRegions(bitmap: Bitmap): List<TextRegion> {
        if (!isDetLoaded || detSession == null || ortEnvironment == null) {
            Log.w(TAG, "Detection model not loaded, returning empty list")
            return emptyList()
        }

        val t0 = System.currentTimeMillis()
        return try {
            val inputTensor = bitmapToDetTensor(bitmap)
            val t1 = System.currentTimeMillis()
            Log.i(TAG, "Det input prep: ${t1 - t0}ms")

            val inputs = hashMapOf(DET_INPUT_NAME to inputTensor)
            detSession?.run(inputs)?.use { result ->
                val outputName = detSession!!.outputNames.firstOrNull()
                    ?: run { Log.e(TAG, "Det session has no outputs"); return@use emptyList() }

                @Suppress("UNCHECKED_CAST")
                val outputTensor = result.get(outputName)?.get() as? OnnxTensor
                    ?: run { Log.e(TAG, "Det output '$outputName' is not OnnxTensor"); return@use emptyList() }

                val shape = outputTensor.info.shape
                Log.i(TAG, "Det output: name=$outputName, shape=${shape.contentToString()}")

                val outputArray = FloatArray(outputTensor.floatBuffer.remaining())
                outputTensor.floatBuffer.get(outputArray)

                val t2 = System.currentTimeMillis()
                Log.i(TAG, "Det inference: ${t2 - t1}ms, outputSize=${outputArray.size}")

                // Log output stats for debugging
                if (outputArray.isNotEmpty()) {
                    var minF = Float.MAX_VALUE; var maxF = Float.MIN_VALUE; var sumF = 0f
                    for (v in outputArray) { if (v < minF) minF = v; if (v > maxF) maxF = v; sumF += v }
                    Log.i(TAG, "Det output stats: min=$minF, max=$maxF, avg=${sumF / outputArray.size}")
                }

                // DBNet output: [1, 1, H, W] or [1, H, W] probability map
                val regions = when {
                    shape.size == 4 -> {
                        val detH = shape[2].toInt()
                        val detW = shape[3].toInt()
                        Log.i(TAG, "Det post-processing: ${detW}x$detH → ${bitmap.width}x${bitmap.height}")
                        postProcessDet(outputArray, detH, detW, bitmap.width, bitmap.height)
                    }
                    shape.size == 3 -> {
                        val detH = shape[1].toInt()
                        val detW = shape[2].toInt()
                        postProcessDet(outputArray, detH, detW, bitmap.width, bitmap.height)
                    }
                    else -> {
                        Log.w(TAG, "Unexpected det output shape: ${shape.contentToString()}")
                        emptyList()
                    }
                }

                val t3 = System.currentTimeMillis()
                Log.i(TAG, "Det total: ${t3 - t0}ms, regions=${regions.size}")
                regions
            } ?: emptyList()
        } catch (e: OrtException) {
            Log.e(TAG, "OrtException in detectTextRegions", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Exception in detectTextRegions", e)
            emptyList()
        }
    }

    /**
     * Convert bitmap to detection model input tensor.
     * Matches soya-ocr preprocessing: aspect-ratio resize, max side = DET_MAX_SIDE,
     * pad to multiple of 32, ImageNet normalization.
     */
    private fun bitmapToDetTensor(bitmap: Bitmap): OnnxTensor {
        val srcH = bitmap.height
        val srcW = bitmap.width

        // Resize: aspect ratio preserved, max side = DET_MAX_SIDE, round up to multiple of 32
        val scale = minOf(DET_MAX_SIDE.toFloat() / srcH, DET_MAX_SIDE.toFloat() / srcW)
        val newH = ((srcH * scale).toInt() + 31) / 32 * 32
        val newW = ((srcW * scale).toInt() + 31) / 32 * 32

        Log.i(TAG, "Det resize: ${srcW}x${srcH} → ${newW}x$newH (scale=$scale)")

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

        // Log input stats for debugging
        var minF = Float.MAX_VALUE; var maxF = Float.MIN_VALUE; var sumF = 0f
        for (v in floatArray) { if (v < minF) minF = v; if (v > maxF) maxF = v; sumF += v }
        Log.i(TAG, "Det input stats: min=$minF, max=$maxF, avg=${sumF / floatArray.size}")

        val floatBuffer = ByteBuffer
            .allocateDirect(totalSize * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(floatArray)
        floatBuffer.rewind()

        val shape = longArrayOf(1L, 3L, newH.toLong(), newW.toLong())
        return OnnxTensor.createTensor(ortEnvironment, floatBuffer, shape)
    }

    /**
     * Post-process DBNet probability map → list of bounding boxes.
     * Matches soya-ocr: threshold → connected component (flood fill) → bounding rect → scale to original.
     */
    private fun postProcessDet(
        probMap: FloatArray,
        detH: Int,
        detW: Int,
        origW: Int,
        origH: Int
    ): List<TextRegion> {
        Log.i(TAG, "Det post-process: ${detW}x$detH → ${origW}x$origH, thresh=$DET_DB_THRESH")

        // 1. Threshold the probability map
        val binary = Array(detH) { BooleanArray(detW) }
        var aboveThresh = 0
        for (y in 0 until detH) {
            for (x in 0 until detW) {
                val v = probMap[y * detW + x]
                if (v > DET_DB_THRESH) {
                    binary[y][x] = true
                    aboveThresh++
                }
            }
        }
        Log.i(TAG, "Det binary: $aboveThresh / ${detH * detW} pixels above threshold")

        // 2. Connected component finder (flood fill)
        val visited = Array(detH) { BooleanArray(detW) }
        val boxes = mutableListOf<TextRegion>()
        val scaleX = origW.toFloat() / detW
        val scaleY = origH.toFloat() / detH

        for (y in 0 until detH) {
            for (x in 0 until detW) {
                if (binary[y][x] && !visited[y][x]) {
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
                                if (nx in 0 until detW && ny in 0 until detH && binary[ny][nx] && !visited[ny][nx]) {
                                    visited[ny][nx] = true
                                    queue.add(Pair(nx, ny))
                                }
                            }
                        }
                    }
                    if (count >= DET_MIN_COMPONENT_SIZE) {
                        val x1 = (minX * scaleX).toInt() - DET_PAD_EXPAND
                        val y1 = (minY * scaleY).toInt() - DET_PAD_EXPAND
                        val x2 = (maxX * scaleX).toInt() + DET_PAD_EXPAND
                        val y2 = (maxY * scaleY).toInt() + DET_PAD_EXPAND
                        val safeX1 = x1.coerceIn(0, origW - 1)
                        val safeY1 = y1.coerceIn(0, origH - 1)
                        val safeX2 = x2.coerceIn(0, origW - 1)
                        val safeY2 = y2.coerceIn(0, origH - 1)
                        if (safeX2 > safeX1 && safeY2 > safeY1) {
                            boxes.add(TextRegion(Rect(safeX1, safeY1, safeX2, safeY2), 1.0f))
                        }
                    }
                }
            }
        }

        Log.i(TAG, "Det post-process: ${boxes.size} regions after filtering (minSize=$DET_MIN_COMPONENT_SIZE)")

            // Compute average confidence for this region
            var sumConf = 0f
            for ((px, py) in pixels) {
                sumConf += probMap[py * detW + px]
            }
            val avgConf = sumConf / pixels.size

            regions.add(TextRegion(Rect(origMinX, origMinY, origMaxX, origMaxY), avgConf))
        }

        Log.i(TAG, "Det post-process: ${boxes.size} regions after filtering")
        return boxes
    }

    // ─── Recognition: rec model ────────────────────────────────

    /**
     * Run recognition on a single cropped region bitmap.
     */
    fun recognizeRegion(bitmap: Bitmap): String {
        if (!isRecLoaded || recSession == null || ortEnvironment == null) {
            Log.w(TAG, "Recognition model not loaded")
            return ""
        }
        if (!dictLoaded) {
            Log.e(TAG, "Character dictionary not loaded")
            return ""
        }

        return try {
            val inputTensor = bitmapToRecTensor(bitmap)
            val inputs = hashMapOf(REC_INPUT_NAME to inputTensor)

            recSession?.run(inputs)?.use { result ->
                val chosenKey = if (recSession!!.outputNames.contains(currentPreset.outputName)) {
                    currentPreset.outputName
                } else {
                    recSession!!.outputNames.firstOrNull() ?: return@use ""
                }

                @Suppress("UNCHECKED_CAST")
                val outputTensor = result.get(chosenKey)?.get() as? OnnxTensor
                    ?: return@use ""

                val outputArray = FloatArray(outputTensor.floatBuffer.remaining())
                outputTensor.floatBuffer.get(outputArray)

                if (outputArray.all { it == 0f }) {
                    Log.w(TAG, "All recognition output values are zero")
                    return@use ""
                }

                decodeOutput(outputArray, outputTensor.info.shape)
            } ?: ""
        } catch (e: OrtException) {
            Log.e(TAG, "OrtException in recognizeRegion", e)
            ""
        } catch (e: Exception) {
            Log.e(TAG, "Exception in recognizeRegion", e)
            ""
        }
    }

    /**
     * Convert bitmap to recognition model input tensor.
     * Resize maintaining aspect ratio (height=48), pad width to 320, normalize, NCHW.
     */
    private fun bitmapToRecTensor(bitmap: Bitmap): OnnxTensor {
        val srcW = bitmap.width
        val srcH = bitmap.height

        val ratio = srcW.toFloat() / srcH.toFloat()
        var resizedW = (ratio * REC_INPUT_HEIGHT).toInt()
        if (resizedW > REC_INPUT_WIDTH) resizedW = REC_INPUT_WIDTH
        if (resizedW < 1) resizedW = 1

        val resized = Bitmap.createScaledBitmap(bitmap, resizedW, REC_INPUT_HEIGHT, true)

        val totalSize = REC_INPUT_CHANNELS * REC_INPUT_HEIGHT * REC_INPUT_WIDTH
        val floatArray = FloatArray(totalSize) { -1.0f }
        val planeSize = REC_INPUT_HEIGHT * REC_INPUT_WIDTH

        for (y in 0 until REC_INPUT_HEIGHT) {
            for (x in 0 until resizedW) {
                val pixel = resized.getPixel(x, y)
                val r = ((pixel shr 16) and 0xFF).toFloat()
                val g = ((pixel shr 8) and 0xFF).toFloat()
                val b = (pixel and 0xFF).toFloat()

                val base = y * REC_INPUT_WIDTH + x
                floatArray[base] = (r - PADDLE_MEAN) / PADDLE_STD
                floatArray[planeSize + base] = (g - PADDLE_MEAN) / PADDLE_STD
                floatArray[2 * planeSize + base] = (b - PADDLE_MEAN) / PADDLE_STD
            }
        }

        val floatBuffer = ByteBuffer
            .allocateDirect(floatArray.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(floatArray)
        floatBuffer.rewind()

        val shape = longArrayOf(1L, REC_INPUT_CHANNELS.toLong(), REC_INPUT_HEIGHT.toLong(), REC_INPUT_WIDTH.toLong())
        return OnnxTensor.createTensor(ortEnvironment, floatBuffer, shape)
    }

    // ─── CTC Decode ────────────────────────────────────────────

    private fun decodeOutput(logits: FloatArray, shape: LongArray): String {
        require(shape.size == 3) { "Expected 3D output shape, got ${shape.size}D" }
        val batch = shape[0].toInt()
        val timeSteps = shape[1].toInt()
        val numClasses = shape[2].toInt()

        if (batch != 1) {
            Log.w(TAG, "Batch size > 1; only first batch processed")
        }

        val blankIndex = 0
        val sb = StringBuilder()
        var last = -1

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

            if (bestClass == blankIndex) {
                last = -1
                continue
            }
            if (bestClass == last) continue

            val dictIndex = bestClass - 1
            if (dictIndex in 0 until charDict.size) {
                sb.append(charDict[dictIndex])
            }
            last = bestClass
        }

        return sb.toString()
    }

    // ─── Product Code Extraction ───────────────────────────────

    private fun extractProductCodes(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val matcher = PRODUCT_CODE_PATTERN.matcher(text.uppercase())
        val codes = mutableSetOf<String>()
        while (matcher.find()) {
            codes.add(matcher.group())
        }
        return codes.toList()
    }

    // ─── Main Pipeline: det → crop → rec → codes ──────────────

    /**
     * Full pipeline: detect text regions → recognize each → extract product codes.
     * Returns a pair of (productCodes, detectedRegions).
     * If detection model is not loaded, falls back to whole-image recognition.
     */
    fun extractProductCode(bitmap: Bitmap): Pair<List<String>, List<TextRegion>> {
        Log.d(TAG, "extractProductCode() called, bitmap: ${bitmap.width}x${bitmap.height}")

        if (!isRecLoaded || recSession == null || ortEnvironment == null) {
            Log.w(TAG, "Engine not ready (rec loaded=$isRecLoaded)")
            return Pair(emptyList(), emptyList())
        }
        if (!dictLoaded) {
            Log.e(TAG, "Character dictionary not loaded")
            return Pair(emptyList(), emptyList())
        }

        // Step 1: Detection
        val regions = if (isDetLoaded) {
            detectTextRegions(bitmap)
        } else {
            Log.w(TAG, "Detection not available, using full image as single region")
            null
        }

        // If detection found no regions, return empty (scan not complete)
        if (regions != null && regions.isEmpty()) {
            Log.d(TAG, "No text regions detected")
            return Pair(emptyList(), emptyList())
        }

        // Step 2: Crop regions (or use full image if no detection)
        val crops = if (regions != null) {
            regions.map { region ->
                val r = region.rect
                val safeRect = Rect(
                    max(0, r.left),
                    max(0, r.top),
                    min(bitmap.width, r.right),
                    min(bitmap.height, r.bottom)
                )
                if (safeRect.width() > 0 && safeRect.height() > 0) {
                    Bitmap.createBitmap(bitmap, safeRect.left, safeRect.top, safeRect.width(), safeRect.height())
                } else {
                    null
                }
            }.filterNotNull()
        } else {
            listOf(bitmap)
        }

        Log.d(TAG, "Processing ${crops.size} region(s) through recognition")

        // Step 3: Recognition on each crop
        val allCodes = mutableSetOf<String>()
        for ((i, crop) in crops.withIndex()) {
            val text = recognizeRegion(crop)
            Log.d(TAG, "Region $i: decoded='$text'")
            if (text.isNotBlank()) {
                val codes = extractProductCodes(text)
                allCodes.addAll(codes)
            }
            crop.recycle()
        }

        Log.d(TAG, "Extracted ${allCodes.size} code(s): $allCodes")
        return Pair(allCodes.toList(), regions ?: emptyList())
    }

    // ─── Resource Release ──────────────────────────────────────

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
            isRecLoaded = false
            isDetLoaded = false
            dictLoaded = false
            charDict.clear()
        }
    }

    protected fun finalize() {
        release()
    }
}
