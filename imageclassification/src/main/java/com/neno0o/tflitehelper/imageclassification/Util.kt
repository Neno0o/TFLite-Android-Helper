package com.neno0o.tflitehelper.imageclassification

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.*
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.Comparator

fun <T> priorityQueue(capacity: Int, block: (Comparator<T>)) =
    PriorityQueue<T>(capacity, block)

fun ByteArray.toBitmap(): Bitmap {
    val byteArrayInputString = ByteArrayInputStream(this)
    return BitmapFactory.decodeStream(byteArrayInputString)
}

@Throws(IOException::class)
fun AssetManager.loadModelFile(modelPath: String): MappedByteBuffer {
    val fileDescriptor = this.openFd(modelPath)
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
}

@Throws(IOException::class)
fun AssetManager.loadLabelList(labelPath: String): List<String> {
    val labelList = ArrayList<String>()
    val reader = BufferedReader(InputStreamReader(this.open(labelPath)))
    reader.use {
        while (true) {
            val line = reader.readLine() ?: break
            labelList.add(line)
        }
    }
    return labelList
}