# TensorFlow Lite Helper for Android

This library helps in getting started with TensorFlow Lite on Android. Inspired by [TensorFlow Lite Android image classification example](https://www.tensorflow.org/lite/models/image_classification/android)

This is an experimental library and subject to change. It's written entirely in [Kotlin](https://kotlinlang.org/) and powered by [TensorFlow Lite](https://www.tensorflow.org/lite/).

## Download
Gradle

```groovy
repositories {
        maven {
            url  "https://dl.bintray.com/neno0o/tflite_helper"
        }
    }
```

```groovy
implementation 'com.neno0o.tflitehelper:imageclassification:0.0.1'
```

Maven

```xml
<dependency>
  <groupId>com.neno0o.tflitehelper</groupId>
  <artifactId>imageclassification</artifactId>
  <version>0.0.1</version>
  <type>pom</type>
</dependency>
```

## Usage

### Image Classification
The library provides a helper class for Image Classification at the minimum usage. In the future releases, the library may provide helpers for Object Detection and Smart Reply.

To get started with Image Classification, get instance by providing your model and label paths in the asset folder to `create` factory method
```kotlin
private lateinit var imageClassification: ImageClassification

imageClassification = ImageClassification.create(
                classifierModel = ClassifierModel.QUANTIZED,
                assetManager = assets,
                modelPath = MODEL_PATH,
                labelPath = LABEL_PATH
            )
```

To classify (recognize) an image, call `classifyImage` with `bitmap` of the image.
```kotlin
val results = imageClassification.classifyImage(bitmap)
```

The results are list of [Recognizable](https://github.com/Neno0o/TensorflowLiteHelper/blob/master/imageclassification/src/main/java/com/neno0o/tflitehelper/imageclassification/Recognizable.kt).

The sample app uses [Coroutines](https://kotlinlang.org/docs/reference/coroutines-overview.html) while initializing `ImageClassification` and when calling `classifyImage` to not block the main thread. 
```kotlin
val results = async { imageClassification.classifyImage(bitmap) }

withContext(Dispatchers.Main) {
    object_name.text = results.await().toString()
}
```
Check out the sample app for more details.

`ImageClassification` uses default input size 224, you should override this value depending on your model, in `create` method. Also, you can override the number of results that will be returned, and the default confidence threshold.
```kotlin
imageClassification = ImageClassification.create(
                classifierModel = ClassifierModel.QUANTIZED,
                assetManager = assets,
                modelPath = MODEL_PATH,
                labelPath = LABEL_PATH,
                inputSize = YOUR_MODEL_INPUT_SIZE,
                numberOfResults = 10,
                confidenceThreshold = .5f
            )
``` 

To configure the `Interpreter` via the `Interpreter.Options`, create instance of `Interpreter.Options` alongside its configurations such as the number of threads that are used to configure the `Interpreter`, and use it in `create` method. 
```kotlin
private lateinit var imageClassification: ImageClassification

imageClassification = ImageClassification.create(
                classifierModel = ClassifierModel.QUANTIZED,
                assetManager = assets,
                modelPath = MODEL_PATH,
                labelPath = LABEL_PATH,
                interpreterOptions = InterpreterOptions object
            )
```

## License
Copyright 2019 Ahmed Gamal. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
    
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.