package com.example.smartbulk.util

import com.example.smartbulk.model.DietRecommendation
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import java.time.LocalDate

/**
 * Cloud Function(getDietRecommendation)을 호출해 오늘의 AI 식단 추천을 가져온다.
 * Anthropic API 키는 Cloud Functions 쪽에만 있고 클라이언트에는 절대 내려오지 않는다.
 * 같은 날짜에 대해서는 서버(Realtime Database)에 캐시된 결과를 재사용하므로
 * 앱을 여러 번 열어도 API가 반복 호출되지 않는다.
 */
object DietRecommender {

    fun fetchTodayRecommendation(
        date: LocalDate = LocalDate.now(),
        onSuccess: (DietRecommendation) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val data = hashMapOf("date" to date.toString())

        FirebaseFunctions.getInstance()
            .getHttpsCallable("getDietRecommendation")
            .call(data)
            .addOnSuccessListener { result ->
                @Suppress("UNCHECKED_CAST")
                val map = result.data as? Map<String, Any?>
                if (map == null) {
                    onFailure(IllegalStateException("empty diet recommendation response"))
                    return@addOnSuccessListener
                }
                val recommendation = DietRecommendation(
                    breakfast = map["breakfast"] as? String ?: "",
                    lunch = map["lunch"] as? String ?: "",
                    dinner = map["dinner"] as? String ?: "",
                    snack = map["snack"] as? String ?: "",
                    note = map["note"] as? String ?: ""
                )
                onSuccess(recommendation)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun friendlyErrorMessage(exception: Exception): String {
        val code = (exception as? FirebaseFunctionsException)?.code
        return when (code) {
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> "로그인이 필요합니다."
            else -> "식단 추천을 불러오지 못했습니다."
        }
    }
}
