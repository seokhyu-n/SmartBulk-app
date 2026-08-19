package com.example.smartbulk

import android.content.pm.PackageManager
import android.graphics.PointF
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 카메라 프리뷰를 보여주고, 프레임마다 MoveNetPoseEstimator로 관절을 추론한 뒤
 * 전달받은 운동 종류에 맞는 ExerciseAnalyzer로 피드백/반복 횟수를 계산해 화면에 표시한다.
 * 화면 자체는 카메라 배관 + 결과 표시만 담당하고, 실제 판정 로직은 헬퍼 클래스들에 위임한다.
 */
class PoseFeedbackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPoseFeedbackBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var poseEstimator: MoveNetPoseEstimator
    private lateinit var analyzer: ExerciseAnalyzer

    private var practiceMode = false
    private var practiceCompleted = false

    private val CAMERA_PERMISSION_REQUEST = 2001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoseFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val exerciseName = intent.getStringExtra(EXTRA_EXERCISE_NAME)
        binding.exerciseNameText.text = exerciseName ?: "운동"
        analyzer = analyzerFor(exerciseName)
        practiceMode = intent.getBooleanExtra(EXTRA_PRACTICE_MODE, false)

        binding.btnClosePose.setOnClickListener { finish() }

        poseEstimator = MoveNetPoseEstimator(this)
        if (!poseEstimator.isReady) {
            Toast.makeText(this, "자세 인식 모델을 불러오지 못했습니다.", Toast.LENGTH_LONG).show()
        }

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
                            Log.e("AI_ANALYZE", "분석 중 오류", e)
                        } finally {
                            imageProxy.close()
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("CameraX", "카메라 바인딩 실패", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeImage(imageProxy: ImageProxy) {
        val frame = poseEstimator.estimate(imageProxy) ?: return

        val points = frame.keypoints.map { PointF(it[1], it[0]) } // MoveNet [y,x,score] -> (x,y)
        val scores = frame.keypoints.map { it[2] }

        runOnUiThread {
            binding.overlayView.setKeypoints(
                points, scores,
                frame.frameWidth, frame.frameHeight,
                frame.cropOffsetX, frame.cropOffsetY, frame.cropSize
            )
        }

        val result = analyzer.analyze(frame.keypoints)

        if (practiceMode && !practiceCompleted && result.repCount >= PRACTICE_TARGET_REPS) {
            practiceCompleted = true
        }

        runOnUiThread {
            if (practiceCompleted) {
                binding.feedbackText.text = "완벽합니다! 운동으로 넘어가셔도 될 것 같아요"
                binding.feedbackText.setTextColor(colorForTone(FeedbackTone.GOOD))
                binding.repCountText.text = "$PRACTICE_TARGET_REPS"
            } else {
                binding.feedbackText.text = result.message
                binding.feedbackText.setTextColor(colorForTone(result.tone))
                binding.repCountText.text = "${result.repCount}"
            }
        }
    }

    private fun colorForTone(tone: FeedbackTone): Int = ContextCompat.getColor(
        this,
        when (tone) {
            FeedbackTone.GOOD -> R.color.success
            FeedbackTone.WARNING -> R.color.warning
            FeedbackTone.NEUTRAL -> R.color.text_primary
        }
    )

    override fun onDestroy() {
        super.onDestroy()
        try {
            ProcessCameraProvider.getInstance(this).get().unbindAll()
        } catch (e: Exception) {
            Log.e("CameraX", "unbindAll 실패: ${e.message}")
        }
        cameraExecutor.shutdown()
        poseEstimator.close()
    }

    companion object {
        const val EXTRA_EXERCISE_NAME = "exerciseName"
        const val EXTRA_PRACTICE_MODE = "practiceMode"
        private const val PRACTICE_TARGET_REPS = 3
    }
}
