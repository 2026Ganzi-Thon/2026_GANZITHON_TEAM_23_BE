package com.ganzithon.pulse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 테스트끼리 같은 컨텍스트·DB를 공유하므로, 테스트마다 다른 loginId를 써서 실행 순서에 의존하지 않는다. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PulseFlowTest.TypedParamController.class)
class PulseFlowTest {
    /** 타입 불일치 처리 확인용. 실제 API에는 아직 숫자 파라미터를 받는 엔드포인트가 없어서 테스트에서만 둔다 */
    @RestController
    @RequestMapping("/api/v1/test-only")
    static class TypedParamController {
        @GetMapping("/typed")
        String typed(@RequestParam int n) { return "ok"; }
    }

    private static final String SPIKE = "{\"bpm\":108,\"baselineBpm\":74,\"steps\":null,\"measuredAt\":\"2026-11-15T14:20:00.000Z\"}";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private String call(String url, String body, String token, int status) throws Exception {
        var req = post(url).contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) req.header("Authorization", "Bearer " + token);
        return mvc.perform(req).andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
    }

    private String signupBody(String loginId, String email, String password) {
        return "{\"name\":\" 홍길동 \",\"nickname\":\"길동\",\"email\":\"" + email
                + "\",\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}";
    }

    /** 가입하고 토큰을 돌려준다 */
    private String signupToken(String loginId) throws Exception {
        String body = call("/api/v1/auth/signup", signupBody(loginId, "hello@example.com", "password123"), null, 201);
        return om.readTree(body).at("/data/accessToken").asText();
    }

    private JsonNode spike(String token) throws Exception {
        return om.readTree(call("/api/v1/heart-rate/spikes", SPIKE, token, 200));
    }

    // ---------- 인증 ----------

    @Test
    void signupAndLogin() throws Exception {
        String signup = signupBody("auth01", "hello@example.com", "password123");
        JsonNode s = om.readTree(call("/api/v1/auth/signup", signup, null, 201));
        assertEquals("홍길동", s.at("/data/user/name").asText());
        assertFalse(s.at("/data/accessToken").asText().isEmpty());

        // 중복 아이디, 규칙 위반, 로그인 실패
        JsonNode dup = om.readTree(call("/api/v1/auth/signup", signup, null, 409));
        assertEquals("AUTH-002", dup.get("code").asText());
        JsonNode bad = om.readTree(call("/api/v1/auth/signup",
                "{\"name\":\"a\",\"nickname\":\"b\",\"email\":\"hello@\",\"loginId\":\"x\",\"password\":\"1\"}", null, 400));
        assertEquals("COMMON-001", bad.get("code").asText());
        assertTrue(bad.get("errors").size() >= 3);
        call("/api/v1/auth/login", "{\"loginId\":\"auth01\",\"password\":\"wrongwrong\"}", null, 401);
        call("/api/v1/auth/login", "{\"loginId\":\"auth01\",\"password\":\"password123\"}", null, 200);
    }

    @Test
    void passwordOver72BytesIsRejected() throws Exception {
        // 영문 73바이트
        JsonNode ascii = om.readTree(call("/api/v1/auth/signup",
                signupBody("long01", "hello@example.com", "a".repeat(73)), null, 400));
        assertEquals("COMMON-001", ascii.get("code").asText());

        // 한글 30자 = 90바이트 (글자 수로는 72 이하라 서비스의 바이트 검사가 막아야 한다)
        JsonNode korean = om.readTree(call("/api/v1/auth/signup",
                signupBody("long02", "hello@example.com", "가".repeat(30)), null, 400));
        assertEquals("COMMON-001", korean.get("code").asText());

        // 경계: 정확히 72바이트는 가입·로그인 된다
        String max = "b".repeat(72);
        call("/api/v1/auth/signup", signupBody("long03", "hello@example.com", max), null, 201);
        call("/api/v1/auth/login", "{\"loginId\":\"long03\",\"password\":\"" + max + "\"}", null, 200);
    }

    @Test
    void loginWithLongPasswordIsUnauthorizedNot500() throws Exception {
        signupToken("long04");
        JsonNode res = om.readTree(call("/api/v1/auth/login",
                "{\"loginId\":\"long04\",\"password\":\"" + "a".repeat(100) + "\"}", null, 401));
        assertEquals("AUTH-001", res.get("code").asText());
        // 없는 아이디도 마찬가지
        JsonNode none = om.readTree(call("/api/v1/auth/login",
                "{\"loginId\":\"nobody\",\"password\":\"" + "가".repeat(30) + "\"}", null, 401));
        assertEquals("AUTH-001", none.get("code").asText());
    }

    @Test
    void emailOver255CharsIsBadRequest() throws Exception {
        String email = "a".repeat(250) + "@x.com"; // 256자
        JsonNode res = om.readTree(call("/api/v1/auth/signup", signupBody("mail01", email, "password123"), null, 400));
        assertEquals("COMMON-001", res.get("code").asText());
        assertTrue(res.get("errors").toString().contains("email"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/records")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-003"));
    }

    // ---------- 심박·알림 ----------

    @Test
    void spikeDecidesWhetherToAlert() throws Exception {
        String token = signupToken("spike01");
        String small = "{\"bpm\":90,\"baselineBpm\":74,\"steps\":0,\"measuredAt\":\"2026-11-15T14:20:00.000Z\"}";
        assertFalse(om.readTree(call("/api/v1/heart-rate/spikes", small, token, 200)).at("/data/shouldAlert").asBoolean());
        String walking = "{\"bpm\":120,\"baselineBpm\":74,\"steps\":40,\"measuredAt\":\"2026-11-15T14:20:00.000Z\"}";
        assertFalse(om.readTree(call("/api/v1/heart-rate/spikes", walking, token, 200)).at("/data/shouldAlert").asBoolean());

        JsonNode a = spike(token);
        assertTrue(a.at("/data/shouldAlert").asBoolean());
        String alertId = a.at("/data/alertId").asText();
        assertTrue(alertId.matches("alert_\\d{8}_\\d{4}"), alertId);
    }

    @Test
    void dismissRaisesThresholdOnce() throws Exception {
        String token = signupToken("dismiss01");
        String alertId = spike(token).at("/data/alertId").asText();

        // 괜찮아요 → 기준 +5 (34 차이는 25→30이라 아직 알림, 한 번 더 거절하면 35라 알림 없음)
        call("/api/v1/alerts/" + alertId + "/dismiss", "", token, 200);
        call("/api/v1/alerts/" + alertId + "/dismiss", "", token, 200); // 멱등: 기준값 한 번만 상승
        call("/api/v1/alerts/alert_00000000_9999/dismiss", "", token, 404);
        JsonNode a2 = spike(token);
        assertTrue(a2.at("/data/shouldAlert").asBoolean());
        call("/api/v1/alerts/" + a2.at("/data/alertId").asText() + "/dismiss", "", token, 200);
        assertFalse(spike(token).at("/data/shouldAlert").asBoolean());
    }

    // ---------- 기록 ----------

    @Test
    void saveAndListRecords() throws Exception {
        String token = signupToken("record01");
        String alertId = spike(token).at("/data/alertId").asText();

        call("/api/v1/records", "{\"pictureId\":null,\"alertId\":null,\"beforeScore\":null,\"afterScore\":3}", token, 201);
        call("/api/v1/records", "{\"pictureId\":\"pic_park_01\",\"alertId\":\"" + alertId + "\",\"beforeScore\":4,\"afterScore\":2}", token, 201);
        call("/api/v1/records", "{\"afterScore\":6}", token, 400);
        mvc.perform(get("/api/v1/records").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].afterScore").value(2))
                .andExpect(jsonPath("$.data[0].beforeScore").value(4))
                .andExpect(jsonPath("$.data[1].beforeScore").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdAt")
                        .value(org.hamcrest.Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00")));
    }

    // ---------- 공통 ----------

    @Test
    void corsPreflightIsAllowed() throws Exception {
        mvc.perform(options("/api/v1/records")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void typeMismatchIsBadRequestNot500() throws Exception {
        String token = signupToken("typed01");
        mvc.perform(get("/api/v1/test-only/typed").param("n", "abc").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-001"))
                .andExpect(jsonPath("$.errors[0].field").value("n"));
        mvc.perform(get("/api/v1/test-only/typed").param("n", "1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
