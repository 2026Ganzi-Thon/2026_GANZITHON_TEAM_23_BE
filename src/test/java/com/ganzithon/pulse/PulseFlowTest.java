package com.ganzithon.pulse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganzithon.pulse.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 테스트끼리 같은 컨텍스트·DB를 공유하므로, 테스트마다 다른 loginId를 써서 실행 순서에 의존하지 않는다. */
@SpringBootTest
@AutoConfigureMockMvc
class PulseFlowTest {
    private static final String SPIKE = "{\"bpm\":108,\"baselineBpm\":74,\"steps\":null,\"measuredAt\":\"2026-11-15T14:20:00.000Z\"}";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserRepository userRepository;

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

        call("/api/v1/records", "{\"mode\":\"COLOR\",\"pictureId\":null,\"alertId\":null,\"beforeScore\":null,\"afterScore\":3}", token, 201);
        call("/api/v1/records", "{\"mode\":\"OBJECT\",\"pictureId\":\"pic_park_01\",\"alertId\":\"" + alertId + "\",\"beforeScore\":4,\"afterScore\":2}", token, 201);
        call("/api/v1/records", "{\"mode\":\"COLOR\",\"afterScore\":6}", token, 400);
        mvc.perform(get("/api/v1/records").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].afterScore").value(2))
                .andExpect(jsonPath("$.data[0].beforeScore").value(4))
                .andExpect(jsonPath("$.data[1].beforeScore").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdAt")
                        .value(org.hamcrest.Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00")));
    }

    private static final String LIST_AUTH = "Authorization";

    private Long saveRecord(String token, String mode) throws Exception {
        String body = call("/api/v1/records", "{\"mode\":\"" + mode + "\",\"afterScore\":3}", token, 201);
        return om.readTree(body).at("/data/recordId").asLong();
    }

    private JsonNode records(String token) throws Exception {
        return om.readTree(mvc.perform(get("/api/v1/records").header(LIST_AUTH, "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("data");
    }

    @Test
    void recordModeIsRequiredAndReturned() throws Exception {
        String token = signupToken("recmode01");

        JsonNode missing = om.readTree(call("/api/v1/records", "{\"afterScore\":3}", token, 400));
        assertEquals("COMMON-001", missing.get("code").asText());
        assertEquals("mode", missing.at("/errors/0/field").asText());
        assertEquals("모드가 필요해요.", missing.at("/errors/0/reason").asText());

        JsonNode undefined = om.readTree(call("/api/v1/records", "{\"mode\":\"REAL\",\"afterScore\":3}", token, 400));
        assertEquals("COMMON-001", undefined.get("code").asText());
        assertEquals(0, undefined.get("errors").size());

        JsonNode saved = om.readTree(call("/api/v1/records", "{\"mode\":\"OBJECT\",\"afterScore\":3}", token, 201));
        assertEquals("OBJECT", saved.at("/data/mode").asText());
        assertTrue(saved.get("data").has("nextAction"));
        assertTrue(saved.at("/data/nextAction").isNull());

        JsonNode first = records(token).get(0);
        assertEquals("OBJECT", first.get("mode").asText());
        assertTrue(first.has("nextAction"));
        assertTrue(first.get("nextAction").isNull());
    }

    @Test
    void nextActionIsRecordedAndOverwritten() throws Exception {
        String token = signupToken("nextact01");
        Long objectId = saveRecord(token, "OBJECT");
        saveRecord(token, "COLOR");

        JsonNode ok = om.readTree(call("/api/v1/records/" + objectId + "/next-action", "{\"action\":\"IMPROVED\"}", token, 200));
        assertEquals("처리되었습니다.", ok.get("message").asText());
        JsonNode list = records(token);
        assertEquals("COLOR", list.get(0).get("mode").asText());
        assertTrue(list.get(0).get("nextAction").isNull());
        assertEquals("OBJECT", list.get(1).get("mode").asText());
        assertEquals("IMPROVED", list.get(1).get("nextAction").asText());

        call("/api/v1/records/" + objectId + "/next-action", "{\"action\":\"RETRY\"}", token, 200);
        assertEquals("RETRY", records(token).get(1).get("nextAction").asText());
        call("/api/v1/records/" + objectId + "/next-action", "{\"action\":\"HELP\"}", token, 200);
        assertEquals("HELP", records(token).get(1).get("nextAction").asText());
    }

    @Test
    void nextActionRejectsMissingOrOthersRecord() throws Exception {
        String owner = signupToken("nextown01");
        String other = signupToken("nextown02");
        Long recordId = saveRecord(owner, "COLOR");

        JsonNode none = om.readTree(call("/api/v1/records/999999/next-action", "{\"action\":\"RETRY\"}", owner, 404));
        assertEquals("RECORD-001", none.get("code").asText());
        assertEquals("기록을 찾을 수 없어요.", none.get("message").asText());

        JsonNode notMine = om.readTree(call("/api/v1/records/" + recordId + "/next-action", "{\"action\":\"RETRY\"}", other, 404));
        assertEquals("RECORD-001", notMine.get("code").asText());
        assertTrue(records(owner).get(0).get("nextAction").isNull());
    }

    @Test
    void nextActionValidatesInput() throws Exception {
        String token = signupToken("nextval01");
        Long recordId = saveRecord(token, "COLOR");
        String url = "/api/v1/records/" + recordId + "/next-action";

        JsonNode undefined = om.readTree(call(url, "{\"action\":\"LATER\"}", token, 400));
        assertEquals("COMMON-001", undefined.get("code").asText());

        JsonNode empty = om.readTree(call(url, "{}", token, 400));
        assertEquals("COMMON-001", empty.get("code").asText());
        assertEquals("action", empty.at("/errors/0/field").asText());
        assertEquals("다음 행동이 필요해요.", empty.at("/errors/0/reason").asText());

        JsonNode badId = om.readTree(call("/api/v1/records/abc/next-action", "{\"action\":\"RETRY\"}", token, 400));
        assertEquals("COMMON-001", badId.get("code").asText());
        assertEquals("recordId", badId.at("/errors/0/field").asText());

        JsonNode noToken = om.readTree(call(url, "{\"action\":\"RETRY\"}", null, 401));
        assertEquals("AUTH-003", noToken.get("code").asText());
        assertTrue(records(token).get(0).get("nextAction").isNull());
    }

    @Test
    void modeAndActionRejectOrdinals() throws Exception {
        String token = signupToken("ordinal01");

        // 순번(숫자·숫자 문자열)은 이름이 아니므로 읽기 오류(400)다. 기존 잘못된 enum 값과 같은 응답 형식이다
        for (String mode : new String[] {"0", "1", "\"0\"", "\"1\"", "\"color\"", "true", "[]"}) {
            JsonNode res = om.readTree(call("/api/v1/records", "{\"mode\":" + mode + ",\"afterScore\":3}", token, 400));
            assertEquals("COMMON-001", res.get("code").asText(), mode);
            assertEquals(400, res.get("status").asInt(), mode);
            assertEquals(0, res.get("errors").size(), mode);
        }
        assertEquals(0, records(token).size());
        assertEquals("COLOR", om.readTree(call("/api/v1/records", "{\"mode\":\"COLOR\",\"afterScore\":3}", token, 201))
                .at("/data/mode").asText());

        Long recordId = records(token).get(0).get("recordId").asLong();
        String url = "/api/v1/records/" + recordId + "/next-action";
        for (String action : new String[] {"0", "1", "\"1\"", "\"2\"", "\"retry\"", "false"}) {
            JsonNode res = om.readTree(call(url, "{\"action\":" + action + "}", token, 400));
            assertEquals("COMMON-001", res.get("code").asText(), action);
            assertEquals(0, res.get("errors").size(), action);
            assertTrue(records(token).get(0).get("nextAction").isNull(), action);
        }
        // 이름 문자열은 그대로 받는다
        call(url, "{\"action\":\"RETRY\"}", token, 200);
        assertEquals("RETRY", records(token).get(0).get("nextAction").asText());
        // 순번으로 덮어쓰려 해도 기존 값이 유지된다
        call(url, "{\"action\":2}", token, 400);
        assertEquals("RETRY", records(token).get(0).get("nextAction").asText());
    }

    @Test
    void recordScoreMustBeIntegerNumber() throws Exception {
        String token = signupToken("recint01");
        // 소수와 문자열은 정수로 바꿔 받지 않고 400이다
        for (String score : new String[] {"2.5", "\"3\"", "3.0", "99999999999"}) {
            JsonNode after = om.readTree(call("/api/v1/records", "{\"mode\":\"COLOR\",\"afterScore\":" + score + "}", token, 400));
            assertEquals("COMMON-001", after.get("code").asText());
            JsonNode before = om.readTree(call("/api/v1/records",
                    "{\"mode\":\"COLOR\",\"beforeScore\":" + score + ",\"afterScore\":3}", token, 400));
            assertEquals("COMMON-001", before.get("code").asText());
        }
        assertEquals(0, records(token).size());
        call("/api/v1/records", "{\"mode\":\"COLOR\",\"beforeScore\":4,\"afterScore\":3}", token, 201);
    }

    @Test
    void recordIdsOver255CharsAreBadRequestNot500() throws Exception {
        String token = signupToken("reclen01");
        String max = "p".repeat(255);
        String over = "p".repeat(256);

        call("/api/v1/records", "{\"mode\":\"COLOR\",\"pictureId\":\"" + max + "\",\"alertId\":\"" + max + "\",\"afterScore\":3}", token, 201);

        JsonNode picture = om.readTree(call("/api/v1/records",
                "{\"mode\":\"COLOR\",\"pictureId\":\"" + over + "\",\"afterScore\":3}", token, 400));
        assertEquals("COMMON-001", picture.get("code").asText());
        assertEquals("pictureId", picture.at("/errors/0/field").asText());
        JsonNode alert = om.readTree(call("/api/v1/records",
                "{\"mode\":\"COLOR\",\"alertId\":\"" + over + "\",\"afterScore\":3}", token, 400));
        assertEquals("COMMON-001", alert.get("code").asText());
        assertEquals("alertId", alert.at("/errors/0/field").asText());
    }

    // ---------- 마이페이지·보호자 ----------

    private static final String ME = "/api/v1/users/me";
    private static final String GUARDIAN = "/api/v1/users/me/guardian";

    /** PATCH·PUT·DELETE·GET을 같은 방식으로 부르고 응답 본문을 JSON으로 돌려준다 */
    private JsonNode send(MockHttpServletRequestBuilder req, String body, String token, int status) throws Exception {
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) req.header("Authorization", "Bearer " + token);
        return om.readTree(mvc.perform(req).andExpect(status().is(status)).andReturn().getResponse().getContentAsString());
    }

    private JsonNode me(String token) throws Exception {
        return send(get(ME), null, token, 200);
    }

    private static String fieldNames(JsonNode node) {
        java.util.List<String> names = new java.util.ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names.toString();
    }

    private static boolean hasErrorField(JsonNode res, String field) {
        for (JsonNode e : res.get("errors")) {
            if (field.equals(e.get("field").asText())) return true;
        }
        return false;
    }

    @Test
    void meReturnsProfileWithNullGuardian() throws Exception {
        String token = signupToken("me01");
        JsonNode res = me(token);
        assertTrue(res.get("success").asBoolean());
        JsonNode data = res.get("data");
        // 필드 이름과 순서는 명세 7.1 예시와 같다. 보호자가 없어도 guardian 키는 있다
        assertEquals("[userId, name, nickname, email, loginId, guardian]",
                fieldNames(data));
        assertEquals("홍길동", data.get("name").asText());
        assertEquals("길동", data.get("nickname").asText());
        assertEquals("hello@example.com", data.get("email").asText());
        assertEquals("me01", data.get("loginId").asText());
        assertTrue(data.has("guardian"));
        assertTrue(data.get("guardian").isNull());
    }

    @Test
    void patchMeChangesOnlySentFields() throws Exception {
        String token = signupToken("mepatch01");

        JsonNode nick = send(patch(ME), "{\"nickname\":\"새닉네임\"}", token, 200).get("data");
        assertEquals("새닉네임", nick.get("nickname").asText());
        assertEquals("홍길동", nick.get("name").asText());
        assertEquals("hello@example.com", nick.get("email").asText());
        assertTrue(nick.get("guardian").isNull());

        // 앞뒤 공백은 지우고 저장한다. null로 보낸 필드는 바꾸지 않는다
        JsonNode name = send(patch(ME), "{\"name\":\" 홍길순 \",\"nickname\":null}", token, 200).get("data");
        assertEquals("홍길순", name.get("name").asText());
        assertEquals("새닉네임", name.get("nickname").asText());
        assertEquals("홍길순", me(token).at("/data/name").asText());

        JsonNode email = send(patch(ME), "{\"email\":\"new@example.com\"}", token, 200).get("data");
        assertEquals("new@example.com", email.get("email").asText());
    }

    @Test
    void patchMeIgnoresLoginIdAndPassword() throws Exception {
        String token = signupToken("mepatch02");
        JsonNode res = send(patch(ME), "{\"nickname\":\"새닉\",\"loginId\":\"other01\",\"password\":\"newpassword1\"}",
                token, 200).get("data");
        assertEquals("새닉", res.get("nickname").asText());
        assertEquals("mepatch02", res.get("loginId").asText());
        assertEquals("mepatch02", me(token).at("/data/loginId").asText());
        call("/api/v1/auth/login", "{\"loginId\":\"mepatch02\",\"password\":\"password123\"}", null, 200);
    }

    @Test
    void patchMeRejectsEmptyRequest() throws Exception {
        String token = signupToken("mepatch03");
        for (String body : new String[] {"{}", "{\"loginId\":\"other01\"}", "{\"name\":null,\"nickname\":null,\"email\":null}"}) {
            JsonNode res = send(patch(ME), body, token, 400);
            assertEquals("COMMON-001", res.get("code").asText());
            assertEquals("바꿀 항목을 하나 이상 보내 주세요.", res.at("/errors/0/reason").asText());
        }
        assertEquals("길동", me(token).at("/data/nickname").asText());
    }

    @Test
    void patchMeValidatesFieldsAndChangesNothingOnFailure() throws Exception {
        String token = signupToken("mepatch04");

        JsonNode blank = send(patch(ME), "{\"nickname\":\"   \"}", token, 400);
        assertEquals("COMMON-001", blank.get("code").asText());
        assertTrue(hasErrorField(blank, "nickname"));

        // 일부 필드만 틀려도 맞는 필드까지 바뀌지 않는다
        JsonNode partial = send(patch(ME), "{\"name\":\"새이름\",\"email\":\"hello@\"}", token, 400);
        assertEquals("COMMON-001", partial.get("code").asText());
        assertTrue(hasErrorField(partial, "email"));
        assertEquals("홍길동", me(token).at("/data/name").asText());

        assertTrue(hasErrorField(send(patch(ME), "{\"name\":\"" + "가".repeat(21) + "\"}", token, 400), "name"));
        assertTrue(hasErrorField(send(patch(ME), "{\"nickname\":\"" + "가".repeat(11) + "\"}", token, 400), "nickname"));
        assertTrue(hasErrorField(send(patch(ME), "{\"email\":\"\"}", token, 400), "email"));
        assertTrue(hasErrorField(send(patch(ME), "{\"email\":\"" + "a".repeat(250) + "@x.com\"}", token, 400), "email"));
        assertEquals("홍길동", me(token).at("/data/name").asText());
    }

    @Test
    void mypageRequiresTokenAndExistingUser() throws Exception {
        for (MockHttpServletRequestBuilder req : new MockHttpServletRequestBuilder[] {
                get(ME), patch(ME), put(GUARDIAN), delete(GUARDIAN)}) {
            JsonNode res = send(req, null, null, 401);
            assertEquals("AUTH-003", res.get("code").asText());
        }

        // 토큰은 유효한데 사용자가 없는 경우 (서버 재시작으로 메모리 DB가 비워진 상황)
        String token = signupToken("megone01");
        userRepository.delete(userRepository.findByLoginId("megone01").orElseThrow());
        assertEquals("AUTH-003", send(get(ME), null, token, 401).get("code").asText());
        assertEquals("AUTH-003", send(patch(ME), "{\"nickname\":\"새닉\"}", token, 401).get("code").asText());
        assertEquals("AUTH-003", send(put(GUARDIAN), "{\"name\":\"엄마\",\"phone\":\"01012345678\"}", token, 401)
                .get("code").asText());
        assertEquals("AUTH-003", send(delete(GUARDIAN), null, token, 401).get("code").asText());
    }

    @Test
    void guardianIsRegisteredAndOverwritten() throws Exception {
        String token = signupToken("guard01");

        JsonNode saved = send(put(GUARDIAN), "{\"name\":\" 엄마 \",\"phone\":\"010-1234-5678\"}", token, 200);
        assertTrue(saved.get("success").asBoolean());
        assertEquals("엄마", saved.at("/data/name").asText());
        assertEquals("01012345678", saved.at("/data/phone").asText());
        assertEquals("[name, phone]", fieldNames(saved.get("data")));

        JsonNode guardian = me(token).at("/data/guardian");
        assertEquals("엄마", guardian.get("name").asText());
        assertEquals("01012345678", guardian.get("phone").asText());

        // 다시 등록하면 덮어쓴다 (보호자는 1명)
        send(put(GUARDIAN), "{\"name\":\"아빠\",\"phone\":\"01098765432\"}", token, 200);
        JsonNode second = me(token).at("/data/guardian");
        assertEquals("아빠", second.get("name").asText());
        assertEquals("01098765432", second.get("phone").asText());
        assertEquals(2, second.size());
    }

    @Test
    void guardianPhoneKeepsDigitsOnly() throws Exception {
        String token = signupToken("guard02");
        assertEquals("01012345678", send(put(GUARDIAN), "{\"name\":\"엄마\",\"phone\":\"010 1234 5678\"}", token, 200)
                .at("/data/phone").asText());
        assertEquals("0111234567", send(put(GUARDIAN), "{\"name\":\"엄마\",\"phone\":\"011-123-4567\"}", token, 200)
                .at("/data/phone").asText());

        for (String phone : new String[] {"02-123-4567", "+82 10-1234-5678", "010-123", "010-1234-56789", ""}) {
            JsonNode bad = send(put(GUARDIAN), "{\"name\":\"엄마\",\"phone\":\"" + phone + "\"}", token, 400);
            assertEquals("COMMON-001", bad.get("code").asText(), phone);
            assertTrue(hasErrorField(bad, "phone"), phone);
        }
        JsonNode missing = send(put(GUARDIAN), "{\"name\":\"엄마\"}", token, 400);
        assertTrue(hasErrorField(missing, "phone"));
        // 실패한 요청은 저장된 번호를 바꾸지 않는다
        assertEquals("0111234567", me(token).at("/data/guardian/phone").asText());
    }

    @Test
    void guardianNameIsValidatedAndFailureKeepsExisting() throws Exception {
        String token = signupToken("guard03");
        send(put(GUARDIAN), "{\"name\":\"엄마\",\"phone\":\"01012345678\"}", token, 200);

        JsonNode missing = send(put(GUARDIAN), "{\"phone\":\"01012345678\"}", token, 400);
        assertEquals("COMMON-001", missing.get("code").asText());
        assertTrue(hasErrorField(missing, "name"));
        assertTrue(hasErrorField(send(put(GUARDIAN), "{\"name\":\"   \",\"phone\":\"01012345678\"}", token, 400), "name"));
        JsonNode tooLong = send(put(GUARDIAN), "{\"name\":\"" + "가".repeat(21) + "\",\"phone\":\"01012345678\"}", token, 400);
        assertEquals("COMMON-001", tooLong.get("code").asText());
        assertTrue(hasErrorField(tooLong, "name"));
        send(put(GUARDIAN), "{\"name\":\"" + "가".repeat(20) + "\",\"phone\":\"01012345678\"}", token, 200);

        send(put(GUARDIAN), "{\"name\":\"엄마\",\"phone\":\"01012345678\"}", token, 200);
        send(put(GUARDIAN), "{\"name\":\"\",\"phone\":\"01012345678\"}", token, 400);
        assertEquals("엄마", me(token).at("/data/guardian/name").asText());
    }

    @Test
    void guardianDeleteIsIdempotent() throws Exception {
        String token = signupToken("guard04");
        send(put(GUARDIAN), "{\"name\":\"엄마\",\"phone\":\"01012345678\"}", token, 200);

        JsonNode deleted = send(delete(GUARDIAN), null, token, 200);
        assertTrue(deleted.get("success").asBoolean());
        assertEquals("처리되었습니다.", deleted.get("message").asText());
        assertTrue(me(token).at("/data").has("guardian"));
        assertTrue(me(token).at("/data/guardian").isNull());

        // 보호자가 없어도 200
        assertEquals("처리되었습니다.", send(delete(GUARDIAN), null, token, 200).get("message").asText());

        // 지운 뒤 다시 등록할 수 있다
        send(put(GUARDIAN), "{\"name\":\"아빠\",\"phone\":\"01098765432\"}", token, 200);
        assertEquals("아빠", me(token).at("/data/guardian/name").asText());
    }

    @Test
    void guardianIsPerUser() throws Exception {
        String a = signupToken("guard05");
        String b = signupToken("guard06");
        send(put(GUARDIAN), "{\"name\":\"엄마\",\"phone\":\"01012345678\"}", a, 200);
        assertTrue(me(b).at("/data/guardian").isNull());
        send(delete(GUARDIAN), null, b, 200);
        assertEquals("엄마", me(a).at("/data/guardian/name").asText());
    }

    @Test
    void corsPreflightAllowsPatchOnMe() throws Exception {
        mvc.perform(options(ME)
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("PATCH")));
    }

    // ---------- 숨은그림 ----------

    private static final String PICTURE_NEXT = "/api/v1/pictures/next";

    @Test
    void pictureNextReturnsObjectModePicture() throws Exception {
        String token = signupToken("pic01");
        String body = mvc.perform(get(PICTURE_NEXT).param("mode", "OBJECT").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("OBJECT"))
                .andReturn().getResponse().getContentAsString();
        JsonNode data = om.readTree(body).get("data");
        assertEquals("[pictureId, mode, imageUrl, aspectRatio, targets]", fieldNames(data));
        assertFalse(data.get("pictureId").asText().isEmpty());
        assertTrue(data.get("imageUrl").asText().startsWith("http://localhost/pictures/"));
        assertTrue(data.get("aspectRatio").asDouble() > 0);
        assertTrue(data.get("targets").size() >= 1);
        for (JsonNode t : data.get("targets")) {
            assertEquals("[targetId, label, emoji, x, y, radius]", fieldNames(t));
        }
    }

    @Test
    void pictureNextAlertIdIsOptionalAndNotChecked() throws Exception {
        String token = signupToken("pic02");
        mvc.perform(get(PICTURE_NEXT).param("mode", "OBJECT").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get(PICTURE_NEXT).param("mode", "OBJECT").param("alertId", "alert_00000000_9999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void pictureNextRequiresValidMode() throws Exception {
        String token = signupToken("pic03");
        mvc.perform(get(PICTURE_NEXT).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-001"))
                .andExpect(jsonPath("$.errors").isEmpty());
        for (String mode : new String[] {"PURPLE", "0", "1", "object"}) {
            mvc.perform(get(PICTURE_NEXT).param("mode", mode).header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON-001"))
                    .andExpect(jsonPath("$.errors[0].field").value("mode"));
        }
    }

    @Test
    void pictureNextRequiresToken() throws Exception {
        mvc.perform(get(PICTURE_NEXT).param("mode", "OBJECT"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH-003"));
    }

    @Test
    void pictureImagesAreServedWithoutToken() throws Exception {
        mvc.perform(get("/pictures/test-pixel.png")).andExpect(status().isOk());
        mvc.perform(get("/pictures/none.png"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON-001"));
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
}
