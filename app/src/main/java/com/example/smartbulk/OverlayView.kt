package com.example.smartbulk

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // keypoints: 정사각형으로 크롭된 프레임 기준 정규화 좌표 (x, y 각각 0..1)
    private var keypoints: List<PointF> = emptyList()
    private var scores: List<Float> = emptyList()

    // 정규화 좌표 -> 실제 화면 좌표 변환에 필요한 정보
    private var frameWidth = 0
    private var frameHeight = 0
    private var cropOffsetX = 0
    private var cropOffsetY = 0
    private var cropSize = 0

    private val paintCircle = Paint().apply {
        color = ContextCompat.getColor(context, R.color.accent)
        style = Paint.Style.FILL
        strokeWidth = 8f
    }

    private val paintLine = Paint().apply {
        color = ContextCompat.getColor(context, R.color.text_primary)
        style = Paint.Style.STROKE
        strokeWidth = 5f
        alpha = 200
    }

    // COCO keypoint 연결 규칙 (일부만 예시: 관절 연결)
    private val skeleton = listOf(
        Pair(5, 7), Pair(7, 9),   // 왼쪽 팔
        Pair(6, 8), Pair(8, 10),  // 오른쪽 팔
        Pair(11, 13), Pair(13, 15), // 왼쪽 다리
        Pair(12, 14), Pair(14, 16), // 오른쪽 다리
        Pair(5, 6), Pair(11, 12), // 어깨, 엉덩이 연결
        Pair(5, 11), Pair(6, 12)  // 몸통 대각선
    )

    /**
     * @param points 정사각형 크롭 기준 정규화 좌표 (x, y 각각 0..1)
     * @param frameWidth/frameHeight 회전 보정 후 원본 카메라 프레임 크기
     * @param cropOffsetX/cropOffsetY/cropSize 원본 프레임에서 정사각형으로 크롭한 영역
     */
    fun setKeypoints(
        points: List<PointF>,
        scores: List<Float>,
        frameWidth: Int,
        frameHeight: Int,
        cropOffsetX: Int,
        cropOffsetY: Int,
        cropSize: Int
    ) {
        this.keypoints = points
        this.scores = scores
        this.frameWidth = frameWidth
        this.frameHeight = frameHeight
        this.cropOffsetX = cropOffsetX
        this.cropOffsetY = cropOffsetY
        this.cropSize = cropSize
        invalidate()
    }

    // PreviewView 기본 스케일(FILL_CENTER: 중앙 기준으로 꽉 채우고 넘치는 부분은 잘림)과
    // 동일한 방식으로 정규화 좌표를 뷰 좌표로 변환한다.
    private fun toScreenPoint(nx: Float, ny: Float): PointF {
        if (frameWidth == 0 || frameHeight == 0) return PointF(0f, 0f)

        val fx = cropOffsetX + nx * cropSize
        val fy = cropOffsetY + ny * cropSize

        val scale = maxOf(width.toFloat() / frameWidth, height.toFloat() / frameHeight)
        val dx = (width - frameWidth * scale) / 2f
        val dy = (height - frameHeight * scale) / 2f

        return PointF(fx * scale + dx, fy * scale + dy)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (keypoints.isEmpty() || frameWidth == 0 || frameHeight == 0) return

        val screenPoints = keypoints.map { toScreenPoint(it.x, it.y) }

        screenPoints.forEachIndexed { i, point ->
            if ((scores.getOrNull(i) ?: 0f) > MIN_DRAW_SCORE) {
                canvas.drawCircle(point.x, point.y, 10f, paintCircle)
            }
        }

        for ((start, end) in skeleton) {
            val p1 = screenPoints.getOrNull(start)
            val p2 = screenPoints.getOrNull(end)
            if (p1 != null && p2 != null &&
                (scores.getOrNull(start) ?: 0f) > MIN_DRAW_SCORE &&
                (scores.getOrNull(end) ?: 0f) > MIN_DRAW_SCORE
            ) {
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paintLine)
            }
        }
    }

    companion object {
        private const val MIN_DRAW_SCORE = 0.2f
    }
}
