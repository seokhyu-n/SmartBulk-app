package com.example.smartbulk

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var keypoints: List<PointF> = emptyList()
    private var scores: List<Float> = emptyList()

    private val paintCircle = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
        strokeWidth = 8f
    }

    private val paintLine = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
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

    fun setKeypoints(points: List<PointF>, scores: List<Float>) {
        keypoints = points
        this.scores = scores
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (keypoints.isEmpty()) return

        // 점 그리기
        keypoints.forEachIndexed { i, point ->
            if (scores.getOrNull(i) ?: 0f > 0.05f) {
                canvas.drawCircle(point.x, point.y, 10f, paintCircle)
            }
        }

        // 선 그리기
        for ((start, end) in skeleton) {
            val p1 = keypoints.getOrNull(start)
            val p2 = keypoints.getOrNull(end)
            if (p1 != null && p2 != null &&
                (scores.getOrNull(start) ?: 0f) > 0.05f &&
                (scores.getOrNull(end) ?: 0f) > 0.05f
            ) {
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paintLine)
            }
        }
    }
}
