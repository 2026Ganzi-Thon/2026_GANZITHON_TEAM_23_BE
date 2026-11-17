package com.ganzithon.pulse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PulseFlowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private String call(String url, String body, String token, int status) throws Exception {
        var req = post(url).contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) req.header("Authorization", "Bearer " + token);
        return mvc.perform(req).andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
    }

    @Test
    void fullFlow() throws Exception {
        String signup = """
                {"name":" 홍길동 ","nickname":"길동","email":"hello@example.com","loginId":"pulse01","password":"password123"}""";
        JsonNode s = om.readTree(call("/api/v1/auth/signup", signup, null, 201));
        String token = s.at("/data/accessToken").asText();
        org.junit.jupiter.api.Assertions.assertEquals("홍길동", s.at("/data/user/name").asText());

        // 중복 아이디, 규칙 위반, 로그인 실패
        JsonNode dup = om.readTree(call("/api/v1/auth/signup", signup, null, 409));
        org.junit.jupiter.api.Assertions.assertEquals("AUTH-002", dup.get("code").asText());
        JsonNode bad = om.readTree(call("/api/v1/auth/signup",
                "{\"name\":\"a\",\"nickname\":\"b\",\"email\":\"hello@\",\"loginId\":\"x\",\"password\":\"1\"}", null, 400));
        org.junit.jupiter.api.Assertions.assertEquals("COMMON-001", bad.get("code").asText());
        org.junit.jupiter.api.Assertions.assertTrue(bad.get("errors").size() >= 3);
        call("/api/v1/auth/login", "{\"loginId\":\"pulse01\",\"password\":\"wrongwrong\"}", null, 401);
        call("/api/v1/auth/login", "{\"loginId\":\"pulse01\",\"password\":\"password123\"}", null, 200);

        // 인증 없음
        mvc.perform(get("/api/v1/records")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-003"));

        // 급상승: 기준 미달 / 걸음 있음 / 알림
        String small = "{\"bpm\":90,\"baselineBpm\":74,\"steps\":0,\"measuredAt\":\"2026-11-15T14:20:00.000Z\"}";
        org.junit.jupiter.api.Assertions.assertFalse(om.readTree(call("/api/v1/heart-rate/spikes", small, token, 200)).at("/data/shouldAlert").asBoolean());
        String walking = "{\"bpm\":120,\"baselineBpm\":74,\"steps\":40,\"measuredAt\":\"2026-11-15T14:20:00.000Z\"}";
        org.junit.jupiter.api.Assertions.assertFalse(om.readTree(call("/api/v1/heart-rate/spikes", walking, token, 200)).at("/data/shouldAlert").asBoolean());
        String spike = "{\"bpm\":108,\"baselineBpm\":74,\"steps\":null,\"measuredAt\":\"2026-11-15T14:20:00.000Z\"}";
        JsonNode a = om.readTree(call("/api/v1/heart-rate/spikes", spike, token, 200));
        org.junit.jupiter.api.Assertions.assertTrue(a.at("/data/shouldAlert").asBoolean());
        String alertId = a.at("/data/alertId").asText();
        org.junit.jupiter.api.Assertions.assertTrue(alertId.matches("alert_\\d{8}_\\d{4}"), alertId);

        // 괜찮아요 → 기준 +5 (34 차이는 25→30이라 아직 알림, 한 번 더 거절하면 35라 알림 없음)
        call("/api/v1/alerts/" + alertId + "/dismiss", "", token, 200);
        call("/api/v1/alerts/" + alertId + "/dismiss", "", token, 200); // 멱등: 기준값 한 번만 상승
        call("/api/v1/alerts/alert_00000000_9999/dismiss", "", token, 404);
        JsonNode a2 = om.readTree(call("/api/v1/heart-rate/spikes", spike, token, 200));
        org.junit.jupiter.api.Assertions.assertTrue(a2.at("/data/shouldAlert").asBoolean());
        call("/api/v1/alerts/" + a2.at("/data/alertId").asText() + "/dismiss", "", token, 200);
        org.junit.jupiter.api.Assertions.assertFalse(om.readTree(call("/api/v1/heart-rate/spikes", spike, token, 200)).at("/data/shouldAlert").asBoolean());

        // 기록
        call("/api/v1/records", "{\"pictureId\":null,\"alertId\":null,\"beforeScore\":null,\"afterScore\":3}", token, 201);
        call("/api/v1/records", "{\"pictureId\":\"pic_park_01\",\"alertId\":\"" + alertId + "\",\"beforeScore\":4,\"afterScore\":2}", token, 201);
        call("/api/v1/records", "{\"afterScore\":6}", token, 400);
        mvc.perform(get("/api/v1/records").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].afterScore").value(2))
                .andExpect(jsonPath("$.data[0].beforeScore").value(4))
                .andExpect(jsonPath("$.data[1].beforeScore").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdAt").value(org.hamcrest.Matchers.endsWith("+09:00")));
    }
}
