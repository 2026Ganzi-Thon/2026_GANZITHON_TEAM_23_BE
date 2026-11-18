package com.ganzithon.pulse.global.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

/** JSON 정수 리터럴만 받는다. 2.5 같은 소수나 "3" 같은 문자열은 읽기 오류(400)가 된다. 필요한 필드에만 붙여 쓴다. */
public class StrictIntegerDeserializer extends StdDeserializer<Integer> {
    public StrictIntegerDeserializer() {
        super(Integer.class);
    }

    @Override
    public Integer deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() != JsonToken.VALUE_NUMBER_INT) {
            return (Integer) ctxt.handleUnexpectedToken(Integer.class, p);
        }
        return p.getIntValue();
    }
}
