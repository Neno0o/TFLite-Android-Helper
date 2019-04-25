package com.neno0o.tflitehelper.imageclassification

import android.content.res.AssetManager
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder.nativeOrder

enum class ClassifierModel {
    FLOAT,
    QUANTIZED
}

abstract class ImageClassification protected constructor(
    val interpreter: Interpreter,
    val labelList: List<String>,
    val inputSize: Int,
    val numberOfResults: Int,
    val confidenceThreshold: Float
) {

    protected val imageByteBuffer: ByteBuffer by lazy {
        ByteBuffer.allocateDirect(byteNumbersPerChannel() * BATCH_SIZE * inputSize * inputSize * PIXEL_SIZE)
            .order(nativeOrder())
    }

    fun classifyImage(bitmap: Bitmap): List<Recognizable> {
        convertBitmapToByteBuffer(bitmap)

        runInterpreter()

        return getResult()
    }

    fun close() {
        interpreter.close()
    }

    protected abstract fun byteNumbersPerChannel(): Int

    protected abstract fun addPixelValueToBuffer(pixelValue: Int)

    protected abstract fun normalizedProbability(labelIndex: Int): Float

    protected abstract fun runInterpreter()

    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        imageByteBuffer.rewind()

        val emptyIntArray = IntArray(inputSize * inputSize)
        bitmap.getPixels(emptyIntArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (x in 0 until inputSize) {
            for (y in 0 until inputSize) {
                val pixelValue = emptyIntArray[pixel++]
                addPixelValueToBuffer(pixelValue)
            }
        }
    }

    private fun getResult(): List<Recognizable> {
        val priorityQueue = priorityQueue<Recognizable>(
            numberOfResults,
            Comparator { o1, o2 ->
                o2.confidence.compareTo(o1.confidence)
            })

        labelList.forEachIndexed { index, label ->
            val confidence = normalizedProbability(index)
            if (confidence > confidenceThreshold) {
                priorityQueue.add(
                    Recognizable(
                        id = index.toString(),
                        name = if (labelList.size > index) label else "unknown",
                        confidence = confidence
                    )
                )
            }
        }

        return priorityQueue.take(numberOfResults)
    }

    companion object {
        private const val DEFAULT_MAX_RESULTS = 3 // nu of results to show on UI
        private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.1f
        private const val DEFAULT_INPUT_SIZE = 224
        private const val BATCH_SIZE = 1 // dimensions of input
        private const val PIXEL_SIZE = 3 // dimensions of input

        fun create(
            classifierModel: ClassifierModel,
            assetManager: AssetManager,
            modelPath: String,
            labelPath: String,
            inputSize: Int = DEFAULT_INPUT_SIZE,
            interpreterOptions: Interpreter.Options = Interpreter.Options(),
            numberOfResults: Int = DEFAULT_MAX_RESULTS,
            confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
        ): ImageClassification {

            val interpreter = Interpreter(
                assetManager.loadModelFile(modelPath),
                interpreterOptions
            )

            return when (classifierModel) {
                ClassifierModel.QUANTIZED -> QuantizedClassifier(
                    interpreter = interpreter,
                    labelList = assetManager.loadLabelList(labelPath),
                    inputSize = inputSize,
                    numberOfResults = numberOfResults,
                    confidenceThreshold = confidenceThreshold
                )
                ClassifierModel.FLOAT -> FloatClassifier(
                    interpreter = interpreter,
                    labelList = assetManager.loadLabelList(labelPath),
                    inputSize = inputSize,
                    numberOfResults = numberOfResults,
                    confidenceThreshold = confidenceThreshold
                )
            }
        }
    }
}

internal class QuantizedClassifier(
    interpreter: Interpreter,
    labelList: List<String>,
    inputSize: Int,
    numberOfResults: Int,
    confidenceThreshold: Float
) : ImageClassification(interpreter, labelList, inputSize, numberOfResults, confidenceThreshold) {

    private val labelResults = Array(1) { ByteArray(labelList.size) }

    override fun byteNumbersPerChannel(): Int {
        return 1
    }

    override fun addPixelValueToBuffer(pixelValue: Int) {
        imageByteBuffer.put(((pixelValue shr 16) and 0xFF).toByte())
        imageByteBuffer.put(((pixelValue shr 8) and 0xFF).toByte())
        imageByteBuffer.put((pixelValue and 0xFF).toByte())
    }

    override fun normalizedProbability(labelIndex: Int): Float {
        return (labelResults[0][labelIndex].toInt() and 0xff) / 255.0f
    }

    override fun runInterpreter() {
        interpreter.run(imageByteBuffer, labelResults)
    }
}

internal class FloatClassifier(
    interpreter: Interpreter,
    labelList: List<String>,
    inputSize: Int,
    numberOfResults: Int,
    confidenceThreshold: Float
) : ImageClassification(interpreter, labelList, inputSize, numberOfResults, confidenceThreshold) {

    private val labelResults = Array(1) { FloatArray(labelList.size) }

    override fun byteNumbersPerChannel(): Int {
        return 4
    }

    override fun addPixelValueToBuffer(pixelValue: Int) {
        imageByteBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255f)
        imageByteBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255f)
        imageByteBuffer.putFloat((pixelValue and 0xFF) / 255f)
    }

    override fun normalizedProbability(labelIndex: Int): Float {
        return labelResults[0][labelIndex]
    }

    override fun runInterpreter() {
        interpreter.run(imageByteBuffer, labelResults)
    }
}
