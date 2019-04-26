/*
 * Copyright 2019 Ahmed Gamal. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.neno0o.tflitehelper.imageclassification

import android.content.res.AssetManager
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder.nativeOrder

/**
 * The model type used for classification. Make sure the trained model is correctly matched with its type.
 */
enum class ClassifierModel {
    FLOAT,
    QUANTIZED
}

/**
 * Abstract class that is used for classification.
 */
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

    /**
     * Returns a list of [Recognizable] objects from image.
     */
    fun classifyImage(bitmap: Bitmap): List<Recognizable> {
        convertBitmapToByteBuffer(bitmap)

        runInterpreter()

        return getResult()
    }

    /**
     * Closes the interpreter.
     */
    fun close() {
        interpreter.close()
    }

    /**
     * The number of bytes that is used to store a single color.
     *
     * In case of [ClassifierModel.QUANTIZED], the result is 1.
     * [ClassifierModel.FLOAT], the result is 4.
     */
    protected abstract fun byteNumbersPerChannel(): Int

    /**
     * Add pixel value to the byte buffer.
     */
    protected abstract fun addPixelValueToBuffer(pixelValue: Int)

    /**
     * Gets the normalized probability for the indexed label, this will represent the confidence.
     */
    protected abstract fun normalizedProbability(labelIndex: Int): Float

    /**
     * Run the [Interpreter].
     */
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
        /**
         * The default number of results to show.
         */
        private const val DEFAULT_MAX_RESULTS = 3 // nu of results to show on UI

        /**
         * The default confidence threshold, [classifyImage] will return all [Recognizable] objects whose confidence more than this value.
         */
        private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.1f

        /**
         * The default input size that is used in the model.
         */
        private const val DEFAULT_INPUT_SIZE = 224

        /**
         * Dimensions of inputs.
         */
        private const val BATCH_SIZE = 1
        private const val PIXEL_SIZE = 3

        /**
         * Factory method, which returns [ImageClassification] based on the model type [ClassifierModel].
         *
         * [assetManager] provides access to an application's raw asset files.
         * [modelPath] the path name of the model.
         * [labelList] the labels list name.
         * [inputSize] the size that is used in the model.
         * [interpreterOptions] the options that will be used by [Interpreter].
         * [numberOfResults] the number of results to show.
         * [confidenceThreshold] the threshold confidence, [classifyImage] will return results whose confidence more than this value.
         *
         */
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

/**
 * A concrete implementation of [ImageClassification] of type [ClassifierModel.QUANTIZED].
 */
private class QuantizedClassifier(
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

/**
 * A concrete implementation of [ImageClassification] of type [ClassifierModel.FLOAT].
 */
private class FloatClassifier(
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
