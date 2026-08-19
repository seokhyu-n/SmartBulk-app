package com.example.smartbulk

// MoveNet(COCO) 키포인트 인덱스
private const val LG_LEFT_HIP = 11
private const val LG_LEFT_KNEE = 13
private const val LG_LEFT_ANKLE = 15
private const val LG_RIGHT_HIP = 12
private const val LG_RIGHT_KNEE = 14
private const val LG_RIGHT_ANKLE = 16

private enum class LungeState { UP, DOWN }

/**
 * 런지: 엉덩이-무릎-발목 각도로 판정한다. 스쿼트와 같은 방식이지만 목표 각도가 다르다
 * (런지는 앞무릎이 약 90도가 되는 지점이 목표).
 *
 * ⚠️ MVP 버전: 왼쪽/오른쪽 중 먼저 보이는 다리 하나만 본다. 실제로는 앞다리와 뒷다리가
 * 다르게 움직이므로, 두 다리를 함께 보는 방식으로 나중에 개선이 필요할 수 있다.
 * 각도 값도 임시 추정치라 실기 테스트로 튜닝이 필요하다.
 */
class LungeAnalyzer(
    private val confidenceThreshold: Float = 0.2f,
    private val deepAngle: Double = 60.0,
    private val lungeAngle: Double = 110.0,   // 이 이하: 앞무릎이 충분히 굽은 작업 자세 (목표 ~90도)
    private val standingAngle: Double = 160.0,
    historySize: Int = 5
) : ExerciseAnalyzer {

    private val maxHistory = historySize
    private val angleHistory = ArrayDeque<Double>()
    private var state = LungeState.UP

    var repCount = 0
        private set

    override fun analyze(keypoints: Array<FloatArray>): AnalyzerResult {
        val leg = pickVisibleLeg(keypoints)
            ?: return AnalyzerResult("사람을 인식하지 못했습니다.", repCount, FeedbackTone.NEUTRAL)

        val angle = jointAngle(leg.hip, leg.knee, leg.ankle)
        angleHistory.addLast(angle)
        if (angleHistory.size > maxHistory) angleHistory.removeFirst()
        val avgAngle = angleHistory.average()

        updateRepState(avgAngle)

        val (message, tone) = feedbackFor(avgAngle, leg.side)
        return AnalyzerResult(message, repCount, tone)
    }

    private data class Leg(val hip: FloatArray, val knee: FloatArray, val ankle: FloatArray, val side: String)

    private fun pickVisibleLeg(kp: Array<FloatArray>): Leg? {
        fun visible(a: FloatArray, b: FloatArray, c: FloatArray) =
            a[2] > confidenceThreshold && b[2] > confidenceThreshold && c[2] > confidenceThreshold

        val leftHip = kp[LG_LEFT_HIP]; val leftKnee = kp[LG_LEFT_KNEE]; val leftAnkle = kp[LG_LEFT_ANKLE]
        val rightHip = kp[LG_RIGHT_HIP]; val rightKnee = kp[LG_RIGHT_KNEE]; val rightAnkle = kp[LG_RIGHT_ANKLE]

        return when {
            visible(leftHip, leftKnee, leftAnkle) ->
                Leg(xy(leftHip), xy(leftKnee), xy(leftAnkle), "왼쪽")
            visible(rightHip, rightKnee, rightAnkle) ->
                Leg(xy(rightHip), xy(rightKnee), xy(rightAnkle), "오른쪽")
            else -> null
        }
    }

    private fun updateRepState(avgAngle: Double) {
        // 선 자세(UP) <-> 런지 자세(DOWN)를 한 번 왕복해야 1회 인정
        when {
            avgAngle >= standingAngle -> {
                if (state == LungeState.DOWN) repCount++
                state = LungeState.UP
            }
            avgAngle <= lungeAngle -> state = LungeState.DOWN
        }
    }

    private fun feedbackFor(avgAngle: Double, side: String): Pair<String, FeedbackTone> = when {
        avgAngle < deepAngle -> "[$side] 무릎이 너무 깊게 굽었습니다!" to FeedbackTone.WARNING
        avgAngle <= lungeAngle -> "[$side] 좋은 자세입니다 👍" to FeedbackTone.GOOD
        avgAngle < standingAngle -> "[$side] 조금 더 내려가세요" to FeedbackTone.NEUTRAL
        else -> "[$side] 준비 자세입니다" to FeedbackTone.NEUTRAL
    }
}
