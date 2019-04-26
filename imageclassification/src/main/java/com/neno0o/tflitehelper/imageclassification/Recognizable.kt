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
