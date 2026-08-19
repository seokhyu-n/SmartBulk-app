package com.example.smartbulk

import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 관절 세 점이 이루는 각도(도)를 계산한다. b가 꼭짓점.
 * a, b, c는 (x, y) 형태의 정규화 좌표.
 */
fun jointAngle(a: FloatArray, b: FloatArray, c: FloatArray): Double {
    val abX = (a[0] - b[0]).toDouble(); val abY = (a[1] - b[1]).toDouble()
    val cbX = (c[0] - b[0]).toDouble(); val cbY = (c[1] - b[1]).toDouble()

    val dot = abX * cbX + abY * cbY
    val abLen = sqrt(abX.pow(2) + abY.pow(2))
    val cbLen = sqrt(cbX.pow(2) + cbY.pow(2))

    // 부동소수점 오차로 인해 [-1,1]을 살짝 벗어나면 acos가 NaN을 반환하므로 clamp
    val cos = (dot / (abLen * cbLen)).coerceIn(-1.0, 1.0)
    return Math.toDegrees(acos(cos))
}

/** MoveNet keypoint([y, x, score]) → (x, y) 좌표만 추출 */
fun xy(kp: FloatArray) = floatArrayOf(kp[1], kp[0])
