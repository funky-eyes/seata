package org.apache.seata.core.rpc.netty.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParameterParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testConvertParamMapWithSingleValue() throws JsonProcessingException {
        Map<String, List<String>> paramMap = new HashMap<>();
        paramMap.put("key1", Collections.singletonList("value1"));
        paramMap.put("key2", Collections.singletonList("value2"));

        ObjectNode result = ParameterParser.convertParamMap(paramMap);

        assertEquals("value1", result.get("key1").asText());
        assertEquals("value2", result.get("key2").asText());
    }

    @Test
    void testConvertParamMapWithMultipleValues() throws JsonProcessingException {
        Map<String, List<String>> paramMap = new HashMap<>();
        paramMap.put("key", Arrays.asList("value1", "value2", "value3"));

        ObjectNode result = ParameterParser.convertParamMap(paramMap);

        JsonNode arrayNode = result.get("key");
        assertTrue(arrayNode.isArray());
        assertEquals(3, arrayNode.size());
        assertEquals("value1", arrayNode.get(0).asText());
        assertEquals("value2", arrayNode.get(1).asText());
        assertEquals("value3", arrayNode.get(2).asText());
    }

    @Test
    void testConvertParamMapWithEmptyList() throws JsonProcessingException {
        Map<String, List<String>> paramMap = new HashMap<>();
        paramMap.put("emptyKey", Collections.emptyList());

        ObjectNode result = ParameterParser.convertParamMap(paramMap);

        assertNull(result.get("emptyKey"));
    }

    @Test
    void testGetArgValuesWithRequestBody() throws Exception {
        Method method = TestClass.class.getMethod("objectMethod", Object.class);

        ParamMetaData paramMetaData = new ParamMetaData();
        paramMetaData.setParamConvertType(ParamMetaData.ParamConvertType.REQUEST_BODY);

        ObjectNode paramMap = objectMapper.createObjectNode();
        ObjectNode bodyNode = paramMap.putObject("body");
        bodyNode.put("field1", "value1");
        bodyNode.put("field2", "value2");

        Object[] args = ParameterParser.getArgValues(
                new ParamMetaData[]{paramMetaData},
                method,
                paramMap
        );

        assertEquals(1, args.length);
        assertNotNull(args[0]);
    }

    // 测试辅助类
    class TestClass {
        public void objectMethod(Object obj) {
        }
    }
}