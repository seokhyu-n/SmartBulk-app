package com.example.smartbulk

// MoveNet(COCO) 키포인트 인덱스
private const val LEFT_HIP = 11
private const val LEFT_KNEE = 13
private const val LEFT_ANKLE = 15
private const val RIGHT_HIP = 12
private const val RIGHT_KNEE = 14
private const val RIGHT_ANKLE = 16

private enum class SquatState { UP, DOWN }

/**
 * MoveNet 키포인트를 받아 스쿼트 자세 피드백 문구와 반복 횟수를 계산한다.
 * 안드로이드/카메라에 의존하지 않는 순수 로직이라 단위 테스트가 쉽고,
 * 다른 운동을 추가할 때는 [ExerciseAnalyzer]를 구현하는 이런 형태의 클래스를 하나 더 만들면 된다.
 */
class SquatAnalyzer(
    private val confidenceThreshold: Float = 0.2f,
    private val deepAngle: Double = 60.0,
    private val squatAngle: Double = 100.0,
    private val standingAngle: Double = 160.0,
    historySize: Int = 5
) : ExerciseAnalyzer {

    private val maxHistory = historySize
    private val angleHistory = ArrayDeque<Double>()
    private var state = SquatState.UP

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

        val leftHip = kp[LEFT_HIP]; val leftKnee = kp[LEFT_KNEE]; val leftAnkle = kp[LEFT_ANKLE]
        val rightHip = kp[RIGHT_HIP]; val rightKnee = kp[RIGHT_KNEE]; val rightAnkle = kp[RIGHT_ANKLE]

        return when {
            visible(leftHip, leftKnee, leftAnkle) ->
                Leg(xy(leftHip), xy(leftKnee), xy(leftAnkle), "왼쪽")
            visible(rightHip, rightKnee, rightAnkle) ->
                Leg(xy(rightHip), xy(rightKnee), xy(rightAnkle), "오른쪽")
            else -> null
        }
    }

    private fun updateRepState(avgAngle: Double) {
        // 선 자세(STANDING) <-> 앉은 자세(SQUAT)를 한 번 왕복해야 1회 인정.
        // 60~160도 사이 애매구간은 카운트 판정에서 제외해 흔들림에 의한 중복 카운트를 막는다.
        when {
            avgAngle >= standingAngle -> {
                if (state == SquatState.DOWN) repCount++
                state = SquatState.UP
            }
            avgAngle <= squatAngle -> state = SquatState.DOWN
        }
    }

    private fun feedbackFor(avgAngle: Double, side: String): Pair<String, FeedbackTone> = when {
        avgAngle < deepAngle -> "[$side] 무릎을 너무 깊게 굽혔습니다!" to FeedbackTone.WARNING
        avgAngle <= squatAngle -> "[$side] 좋은 자세입니다 👍" to FeedbackTone.GOOD
        avgAngle < standingAngle -> "[$side] 조금 더 내려가세요" to FeedbackTone.NEUTRAL
        else -> "[$side] 준비 자세입니다. 스쿼트를 시작하세요" to FeedbackTone.NEUTRAL
    }
}
