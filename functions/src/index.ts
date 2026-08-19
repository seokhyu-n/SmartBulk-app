import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";
import Anthropic from "@anthropic-ai/sdk";

admin.initializeApp();

const anthropicApiKey = defineSecret("ANTHROPIC_API_KEY");

interface DietRecommendation {
  breakfast: string;
  lunch: string;
  dinner: string;
  snack: string;
  note: string;
}

const DIET_SCHEMA = {
  type: "object",
  properties: {
    breakfast: { type: "string", description: "아침 식단 한 줄 설명" },
    lunch: { type: "string", description: "점심 식단 한 줄 설명" },
    dinner: { type: "string", description: "저녁 식단 한 줄 설명" },
    snack: { type: "string", description: "간식/보충 식단 한 줄 설명 (없으면 빈 문자열)" },
    note: { type: "string", description: "오늘 식단에 대한 한 줄 팁이나 격려 메시지" },
  },
  required: ["breakfast", "lunch", "dinner", "snack", "note"],
  additionalProperties: false,
};

/**
 * 로그인한 사용자의 프로필 + 그날 운동 부위(dailySplit)를 바탕으로
 * Claude에게 하루 식단을 추천받는다. 같은 uid+date 조합은 하루 한 번만
 * 실제 API를 호출하고, 이후에는 Realtime Database에 저장된 결과를 그대로 돌려준다.
 */
export const getDietRecommendation = onCall(
  { secrets: [anthropicApiKey], region: "us-central1" },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "로그인이 필요합니다.");
    }

    const date = String(request.data?.date ?? "").trim();
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
      throw new HttpsError("invalid-argument", "date는 YYYY-MM-DD 형식이어야 합니다.");
    }

    const db = admin.database();
    const cacheRef = db.ref(`users/${uid}/dietRecommendations/${date}`);

    const cached = await cacheRef.get();
    if (cached.exists()) {
      return cached.val() as DietRecommendation;
    }

    const userSnapshot = await db.ref(`users/${uid}`).get();
    if (!userSnapshot.exists()) {
      throw new HttpsError("failed-precondition", "사용자 프로필을 찾을 수 없습니다.");
    }
    const user = userSnapshot.val() as {
      name?: string;
      age?: number;
      height?: number;
      weight?: number;
      goal?: string;
      dailySplit?: Record<string, string>;
    };

    const category = user.dailySplit?.[date] ?? "휴식";

    const client = new Anthropic({ apiKey: anthropicApiKey.value() });

    const userPrompt = [
      `사용자 정보: 나이 ${user.age ?? "미상"}세, 키 ${user.height ?? "미상"}cm, 몸무게 ${user.weight ?? "미상"}kg, 운동 목표 "${user.goal ?? "근육 증가"}".`,
      `오늘(${date})의 운동 부위: "${category}".`,
      "이 사람의 목표와 오늘 운동 부위에 맞춰 한국인이 실제로 구하기 쉬운 재료로 아침/점심/저녁 식단을 각각 한 줄로 추천해줘.",
      "간식은 필요하면 한 줄로, 필요 없으면 빈 문자열로 남겨줘.",
      "note에는 오늘 식단과 관련된 짧은 팁이나 응원 한마디를 담아줘.",
      "과장된 의학적 주장은 하지 말고, 실용적이고 간결하게 작성해줘.",
    ].join("\n");

    let response;
    try {
      response = await client.messages.create({
        model: "claude-opus-5",
        max_tokens: 1024,
        thinking: { type: "disabled" },
        output_config: {
          effort: "low",
          format: { type: "json_schema", schema: DIET_SCHEMA },
        },
        messages: [{ role: "user", content: userPrompt }],
      });
    } catch (error) {
      logger.error("Anthropic API 호출 실패", error);
      throw new HttpsError("internal", "식단 추천을 불러오지 못했습니다.");
    }

    if (response.stop_reason === "refusal") {
      throw new HttpsError("internal", "식단 추천을 생성할 수 없습니다.");
    }

    const textBlock = response.content.find((block) => block.type === "text");
    if (!textBlock || textBlock.type !== "text") {
      throw new HttpsError("internal", "식단 추천 응답이 비어 있습니다.");
    }

    let recommendation: DietRecommendation;
    try {
      recommendation = JSON.parse(textBlock.text) as DietRecommendation;
    } catch (error) {
      logger.error("식단 추천 JSON 파싱 실패", { text: textBlock.text, error });
      throw new HttpsError("internal", "식단 추천 응답을 해석하지 못했습니다.");
    }

    await cacheRef.set(recommendation);

    return recommendation;
  }
);
