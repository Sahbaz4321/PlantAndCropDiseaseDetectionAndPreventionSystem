package com.example.ca2.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class Classifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var labelList: List<String> = emptyList()
    private val inputSize = 224

    init {
        try {
            labelList = loadLabels()
            val modelBuffer = loadModelFile()
            interpreter = createInterpreter(modelBuffer)
            Log.d("Classifier", "Model loaded successfully")

        } catch (e: Exception) {
            Log.e("Classifier", "Init Error: ${e.message}", e)
            throw RuntimeException("Model Error: ${e.message}")
        }
    }

    private fun createInterpreter(modelBuffer: MappedByteBuffer): Interpreter {
        try {
            val defaultOptions = Interpreter.Options().apply {
                setNumThreads(4)
                setUseXNNPACK(true)
            }
            return Interpreter(modelBuffer, defaultOptions)
        } catch (e: Exception) {
            Log.e("Classifier", "Default interpreter creation failed", e)
            throw IllegalStateException(
                "Could not create TensorFlow Lite interpreter.\n\nDefault interpreter failed: ${e.stackTraceToString()}",
                e
            )
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("final_model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabels(): List<String> {
        val labels = mutableListOf<String>()
        val jsonString = context.assets.open("classes picker.json").bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)
        val keys = jsonObject.keys()
        val sortedKeys = mutableListOf<Int>()
        while (keys.hasNext()) {
            val key = keys.next()
            try { sortedKeys.add(key.toInt()) } catch (e: Exception) {}
        }
        sortedKeys.sort()
        for (key in sortedKeys) {
            labels.add(jsonObject.getString(key.toString()))
        }
        return labels
    }

    fun classify(bitmap: Bitmap): Pair<String, Float> {
        val interp = interpreter ?: return Pair("Model Not Loaded", 0f)
        val inputTensor = interp.getInputTensor(0)
        val outputTensor = interp.getOutputTensor(0)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val inputBuffer = createInputBuffer(scaledBitmap, inputTensor.dataType())
        val probabilities = createOutputArray(outputTensor.shape())

        interp.run(inputBuffer, probabilities)

        val outputValues = probabilities[0]
        var maxIndex = 0
        var maxProb = -1f
        for (i in outputValues.indices) {
            if (outputValues[i] > maxProb) {
                maxProb = outputValues[i]
                maxIndex = i
            }
        }

        return Pair(if (maxIndex < labelList.size) labelList[maxIndex] else "Unknown", maxProb)
    }

    private fun createInputBuffer(bitmap: Bitmap, dataType: DataType): ByteBuffer {
        val channels = 3
        val bytesPerChannel = if (dataType == DataType.UINT8) 1 else 4
        val inputBuffer = ByteBuffer.allocateDirect(inputSize * inputSize * channels * bytesPerChannel)
            .order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            if (dataType == DataType.UINT8) {
                inputBuffer.put(r.toByte())
                inputBuffer.put(g.toByte())
                inputBuffer.put(b.toByte())
            } else {
                inputBuffer.putFloat(r.toFloat())
                inputBuffer.putFloat(g.toFloat())
                inputBuffer.putFloat(b.toFloat())
            }
        }

        inputBuffer.rewind()
        return inputBuffer
    }

    private fun createOutputArray(shape: IntArray): Array<FloatArray> {
        val outputClasses = when {
            shape.size >= 2 -> shape[shape.lastIndex]
            labelList.isNotEmpty() -> labelList.size
            else -> 1
        }
        return Array(1) { FloatArray(outputClasses) }
    }

    fun close() {
        interpreter?.close()
    }
}
