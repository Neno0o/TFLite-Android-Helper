package com.neno0o.tflitehelper.imageclassification

/**
 * This data class represents a recognizable object returned by [ImageClassification].
 *
 * [id] a unique identifier that is set when getting the results.
 * [name] the name for the recognition, which in the labels list.
 * [confidence] the score that comes by the trained model, to identify how good/bad the recognition is.
 *
 */
data class Recognizable(
    var id: String = "",
    var name: String = "",
    var confidence: Float = 0F
) {
    override fun toString(): String {
        return "Label Id = $id, Name = $name, Confidence = $confidence"
    }
}
