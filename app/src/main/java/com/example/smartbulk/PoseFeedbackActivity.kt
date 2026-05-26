package com.example.smartbulk

import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smartbulk.databinding.ActivityPoseFeedbackBinding
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

class PoseFeedbackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPoseFeedbackBinding
    private lateinit var cameraExecutor: ExecutorService
    private var tflite: Interpreter? = null

    private var modelInputWidth = 192
    private var modelInputHeight = 192

    private val kneeAngleHistory = ArrayDeque<Double>()
    private val maxHistorySize = 5

    private val CAMERA_PERMISSION_REQUEST = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoseFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnClosePose.setOnClickListener { finish() }

        // ✅ 카메라 권한 확인
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }

        // ✅ MoveNet Thunder 모델 불러오기
        try {
            val modelName = "movenet_singlepose_thunder.tflite"
            val modelBytes = assets.open(modelName).readBytes()

            // 버퍼 크기 체크
            if (modelBytes.isEmpty()) {
                throw IllegalStateException("모델 파일이 비어 있습니다.")
            }

            val buffer = ByteBuffer.allocateDirect(modelBytes.size)
            buffer.order(ByteOrder.nativeOrder())
            buffer.put(modelBytes)
            buffer.rewind()

            tflite = Interpreter(buffer)

            val inputShape = tflite!!.getInputTensor(0).shape() // [1,h,w,3]
            modelInputHeight = inputShape[1]
            modelInputWidth = inputShape[2]

            val outputShape = tflite!!.getOutputTensor(0).shape() // [1,1,17,3]
            Log.i("AI_MODEL", "✅ 모델 로드 성공: $modelName (크기=${modelBytes.size} bytes)")
            Log.i("AI_MODEL", "입력 shape=${inputShape.contentToString()}")
            Log.i("AI_MODEL", "출력 shape=${outputShape.contentToString()}")

        } catch (e: Exception) {
            Log.e("AI_MODEL", "❌ 모델 로딩 실패: ${e.message}")
        }

        // ✅ 단일 스레드 Executor
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 📌 CameraX 초기화
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        try {
                            analyzeImage(imageProxy)
                        } catch (e: Exception) {
                            Log.e("AI_ANALYZE", "❌ 분석 중 오류", e)
                        } finally {
                            imageProxy.close()
                        }
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("CameraX", "카메라 바인딩 실패", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // 📌 이미지 분석
    private fun analyzeImage(imageProxy: ImageProxy) {
        val interpreter = tflite ?: return

        val bitmap = imageProxy.toBitmap() ?: return
        val inputBuffer = preprocessImage(bitmap)

        val outputShape = interpreter.getOutputTensor(0).shape() // [1,1,17,3]
        val output = Array(outputShape[0]) {
            Array(outputShape[1]) {
                Array(outputShape[2]) {
                    FloatArray(outputShape[3])
                }
            }
        }

        synchronized(interpreter) {
            try {
                interpreter.run(inputBuffer, output)
            } catch (e: Exception) {
                Log.e("AI_INTERPRETER", "❌ 추론 실패", e)
                return
            }
        }

        val keypoints = output[0][0]

        val screenPoints = keypoints.map { kp ->
            PointF(kp[0] * binding.cameraPreview.width, kp[1] * binding.cameraPreview.height)
        }
        runOnUiThread {
            binding.overlayView.setKeypoints(screenPoints, keypoints.map { it[2] })
        }

        giveFeedback(keypoints, modelInputWidth, modelInputHeight)
    }

    // 📌 이미지 전처리 ([0,1] 정규화)
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, modelInputWidth, modelInputHeight, true)
        val inputBuffer =
            ByteBuffer.allocateDirect(1 * modelInputWidth * modelInputHeight * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())

        for (y in 0 until modelInputHeight) {
            for (x in 0 until modelInputWidth) {
                val pixel = scaled.getPixel(x, y)
                inputBuffer.putFloat((pixel shr 16 and 0xFF) / 255f)
                inputBuffer.putFloat((pixel shr 8 and 0xFF) / 255f)
                inputBuffer.putFloat((pixel and 0xFF) / 255f)
            }
        }

        // 버퍼 크기 확인
        if (inputBuffer.capacity() != 1 * modelInputWidth * modelInputHeight * 3 * 4) {
            Log.w("AI_BUFFER", "⚠️ 입력 버퍼 크기 불일치: ${inputBuffer.capacity()}")
        }

        return inputBuffer
    }

    // 📌 피드백 로직
    private fun giveFeedback(keypoints: Array<FloatArray>, imageWidth: Int, imageHeight: Int) {
        val leftHip = keypoints[11]
        val leftKnee = keypoints[13]
        val leftAnkle = keypoints[15]
        val rightHip = keypoints[12]
        val rightKnee = keypoints[14]
        val rightAnkle = keypoints[16]

        val threshold = 0.05f
        Log.i("AI_SCORE", "왼쪽: Hip=${leftHip[2]}, Knee=${leftKnee[2]}, Ankle=${leftAnkle[2]}")
        Log.i("AI_SCORE", "오른쪽: Hip=${rightHip[2]}, Knee=${rightKnee[2]}, Ankle=${rightAnkle[2]}")

        val leftValid = leftHip[2] > threshold && leftKnee[2] > threshold && leftAnkle[2] > threshold
        val rightValid = rightHip[2] > threshold && rightKnee[2] > threshold && rightAnkle[2] > threshold

        val (hip, knee, ankle, side) = when {
            leftValid -> Quadruple(
                floatArrayOf(leftHip[0] * imageWidth, leftHip[1] * imageHeight),
                floatArrayOf(leftKnee[0] * imageWidth, leftKnee[1] * imageHeight),
                floatArrayOf(leftAnkle[0] * imageWidth, leftAnkle[1] * imageHeight),
                "왼쪽"
            )
            rightValid -> Quadruple(
                floatArrayOf(rightHip[0] * imageWidth, rightHip[1] * imageHeight),
                floatArrayOf(rightKnee[0] * imageWidth, rightKnee[1] * imageHeight),
                floatArrayOf(rightAnkle[0] * imageWidth, rightAnkle[1] * imageHeight),
                "오른쪽"
            )
            else -> {
                runOnUiThread { binding.feedbackText.text = "사람을 인식하지 못했습니다." }
                return
            }
        }

        val kneeAngle = calculateAngle(hip, knee, ankle)
        kneeAngleHistory.addLast(kneeAngle)
        if (kneeAngleHistory.size > maxHistorySize) kneeAngleHistory.removeFirst()
        val avgKneeAngle = kneeAngleHistory.average()

        Log.i("AI_FEEDBACK", "(${side} 다리) 무릎 각도(평균) = $avgKneeAngle")

        val feedback = when {
            avgKneeAngle < 60 -> "[$side] 무릎을 너무 깊게 굽혔습니다!"
            avgKneeAngle in 60.0..100.0 -> "[$side] 좋은 자세입니다 👍"
            else -> "[$side] 무릎을 더 굽히세요!"
        }

        runOnUiThread { binding.feedbackText.text = feedback }
    }

    private fun calculateAngle(a: FloatArray, b: FloatArray, c: FloatArray): Double {
        val ab = doubleArrayOf((a[0] - b[0]).toDouble(), (a[1] - b[1]).toDouble())
        val cb = doubleArrayOf((c[0] - b[0]).toDouble(), (c[1] - b[1]).toDouble())

        val dot = ab[0] * cb[0] + ab[1] * cb[1]
        val abLen = sqrt(ab[0].pow(2) + ab[1].pow(2))
        val cbLen = sqrt(cb[0].pow(2) + cb[1].pow(2))

        return Math.toDegrees(acos(dot / (abLen * cbLen)))
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(this).get()
            cameraProvider.unbindAll()
        } catch (e: Exception) {
            Log.e("CameraX", "unbindAll 실패: ${e.message}")
        }
        try {
            cameraExecutor.shutdown()
            tflite?.close()
            tflite = null
            Log.i("AI_MODEL", "🛑 Interpreter와 Executor 안전 종료 완료")
        } catch (e: Exception) {
            Log.e("AI_MODEL", "Interpreter 종료 오류", e)
        }
    }
}

// -------------------------------
// 확장 함수: ImageProxy → Bitmap 변환
// -------------------------------
fun ImageProxy.toBitmap(): Bitmap? {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

// -------------------------------
// Quadruple 자료형 정의
// -------------------------------
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
