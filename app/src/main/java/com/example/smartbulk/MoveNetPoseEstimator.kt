package com.example.smartbulk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 한 프레임의 MoveNet 추론 결과.
 * keypoints: 17개 관절, 각 원소 = [y, x, score] (정사각형 크롭 기준 0..1 정규화, MoveNet 출력 규격 그대로)
 * frameWidth/frameHeight: 회전 보정까지 끝난 원본 카메라 프레임 크기
 * cropOffsetX/cropOffsetY/cropSize: frame 안에서 정사각형으로 크롭한 영역 (오버레이 좌표 변환에 필요)
 */
class PoseFrame(
    val keypoints: Array<FloatArray>,
    val frameWidth: Int,
    val frameHeight: Int,
    val cropOffsetX: Int,
    val cropOffsetY: Int,
    val cropSize: Int
)

/**
 * MoveNet Single Pose 모델을 로드하고, 카메라 프레임(ImageProxy)을 받아 키포인트를 추론한다.
 * 회전 보정, 정사각형 크롭(비율 왜곡 방지), NV21 변환까지 이 클래스 안에서 전부 처리하므로
 * 호출부(Activity)는 estimate()만 부르면 된다.
 */
class MoveNetPoseEstimator(
    context: Context,
    modelAssetName: String = "movenet_singlepose_thunder.tflite"
) {
    private var interpreter: Interpreter? = null
    private var inputWidth = 192
    private var inputHeight = 192

    val isReady: Boolean get() = interpreter != null

    init {
        try {
            val modelBytes = context.assets.open(modelAssetName).readBytes()
            if (modelBytes.isEmpty()) throw IllegalStateException("모델 파일이 비어 있습니다.")

            val buffer = ByteBuffer.allocateDirect(modelBytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(modelBytes)
                rewind()
            }

            val loaded = Interpreter(buffer)
            val inputTensor = loaded.getInputTensor(0)
            val inputShape = inputTensor.shape() // [1, h, w, 3]
            inputHeight = inputShape[1]
            inputWidth = inputShape[2]
            interpreter = loaded

            val outputShape = loaded.getOutputTensor(0).shape()
            Log.i(
                TAG,
                "모델 로드 성공: $modelAssetName (${modelBytes.size} bytes), " +
                    "입력=${inputShape.contentToString()} dtype=${inputTensor.dataType()}, " +
                    "출력=${outputShape.contentToString()}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "모델 로딩 실패: ${e.message}", e)
        }
    }

    /** 카메라 프레임 하나를 추론한다. 모델 미준비/추론 실패 시 null. */
    fun estimate(imageProxy: ImageProxy): PoseFrame? {
        val interpreter = interpreter ?: return null

        val rawBitmap = imageProxy.toBitmap() ?: return null
        // 센서 회전 보정: 안 하면 세로로 촬영 시 모델에 옆으로 누운 사람이 들어감
        val rotated = rotateBitmap(rawBitmap, imageProxy.imageInfo.rotationDegrees)

        // 세로로 긴 카메라 프레임을 억지로 정사각형에 눌러 찌그러뜨리지 않도록
        // 가운데 기준 정사각형 크롭 후 리사이즈
        val cropSize = minOf(rotated.width, rotated.height)
        val cropOffsetX = (rotated.width - cropSize) / 2
        val cropOffsetY = (rotated.height - cropSize) / 2
        val squareBitmap = Bitmap.createBitmap(rotated, cropOffsetX, cropOffsetY, cropSize, cropSize)

        val outputShape = interpreter.getOutputTensor(0).shape() // [1, 1, 17, 3]
        val output = Array(outputShape[0]) {
            Array(outputShape[1]) { Array(outputShape[2]) { FloatArray(outputShape[3]) } }
        }

        try {
            interpreter.run(toInputBuffer(squareBitmap), output)
        } catch (e: Exception) {
            Log.e(TAG, "추론 실패", e)
            return null
        }

        return PoseFrame(
            keypoints = output[0][0],
            frameWidth = rotated.width,
            frameHeight = rotated.height,
            cropOffsetX = cropOffsetX,
            cropOffsetY = cropOffsetY,
            cropSize = cropSize
        )
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // MoveNet은 0~1 정규화가 아니라 원본 0~255 픽셀 값을 float로 그대로 기대함
    // (TF Hub 공식 예제도 tf.cast(image, uint8) 그대로 넣지, /255 정규화를 하지 않음)
    private fun toInputBuffer(bitmap: Bitmap): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        val buffer = ByteBuffer.allocateDirect(1 * inputWidth * inputHeight * 3 * 4)
        buffer.order(ByteOrder.nativeOrder())

        for (y in 0 until inputHeight) {
            for (x in 0 until inputWidth) {
                val pixel = scaled.getPixel(x, y)
                buffer.putFloat((pixel shr 16 and 0xFF).toFloat())
                buffer.putFloat((pixel shr 8 and 0xFF).toFloat())
                buffer.putFloat((pixel and 0xFF).toFloat())
            }
        }

        // 커서를 처음으로 되돌려야 Interpreter.run()이 방금 채운 데이터를 읽는다
        // (안 하면 커서가 끝에 있어 빈 버퍼를 넘기는 것과 같음)
        buffer.rewind()
        return buffer
    }

    companion object {
        private const val TAG = "MoveNetPoseEstimator"
    }
}

// -------------------------------
// 확장 함수: ImageProxy(YUV_420_888) → Bitmap 변환
// -------------------------------
fun ImageProxy.toBitmap(): Bitmap? {
    if (format != ImageFormat.YUV_420_888) return null

    val yPlane = planes[0]
    val uPlane = planes[1]
    val vPlane = planes[2]

    val ySize = yPlane.buffer.remaining()
    val chromaWidth = width / 2
    val chromaHeight = height / 2
    val nv21 = ByteArray(ySize + chromaWidth * chromaHeight * 2)
    yPlane.buffer.get(nv21, 0, ySize)

    // U/V 플레인은 기기별로 rowStride/pixelStride가 다를 수 있어
    // 단순히 버퍼를 이어붙이면 안 되고, 각 픽셀 위치를 stride 기준으로 직접 읽어야 함
    val uBytes = ByteArray(uPlane.buffer.remaining()).also { uPlane.buffer.get(it) }
    val vBytes = ByteArray(vPlane.buffer.remaining()).also { vPlane.buffer.get(it) }

    var offset = ySize
    for (row in 0 until chromaHeight) {
        for (col in 0 until chromaWidth) {
            val vIndex = row * vPlane.rowStride + col * vPlane.pixelStride
            val uIndex = row * uPlane.rowStride + col * uPlane.pixelStride
            nv21[offset++] = vBytes[vIndex]
            nv21[offset++] = uBytes[uIndex]
        }
    }

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}
