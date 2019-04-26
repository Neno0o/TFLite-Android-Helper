package com.neno0o.tflitehelper

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.neno0o.tflitehelper.imageclassification.ClassifierModel
import com.neno0o.tflitehelper.imageclassification.ImageClassification
import com.neno0o.tflitehelper.imageclassification.toBitmap
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

private const val MODEL_PATH = "mobilenet_v1_1.0_224_quant.tflite"
private const val LABEL_PATH = "labels.txt"
private const val INPUT_SIZE = 224

class MainActivity : AppCompatActivity(), CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    private lateinit var imageClassification: ImageClassification

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadModule()

        detect_button.setOnClickListener {
            camera.captureImage { cameraKitView, bytes ->
                val bitmap =
                    Bitmap.createScaledBitmap(
                        bytes.toBitmap(),
                        INPUT_SIZE,
                        INPUT_SIZE,
                        false
                    )

                object_image.setImageBitmap(bitmap)

                detectObject(bitmap)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        camera.onStart()
    }

    override fun onResume() {
        super.onResume()
        camera.onResume()
    }

    override fun onPause() {
        camera.onPause()
        super.onPause()
    }

    override fun onStop() {
        camera.onStop()
        super.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        camera.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun loadModule() = launch {
        withContext(Dispatchers.IO) {
            imageClassification = ImageClassification.create(
                classifierModel = ClassifierModel.QUANTIZED,
                assetManager = assets,
                modelPath = MODEL_PATH,
                labelPath = LABEL_PATH
            )
        }
    }

    private fun detectObject(bitmap: Bitmap) = launch {
        val results = async { imageClassification.classifyImage(bitmap) }

        Log.d("results", results.await().toString())
        withContext(Dispatchers.Main) {
            object_name.text = results.await().toString()
        }
    }
}
