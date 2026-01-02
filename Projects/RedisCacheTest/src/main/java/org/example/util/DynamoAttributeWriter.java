package org.example.util;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DynamoAttributeWriter {

    private DynamoAttributeWriter() {}

    public static AttributeValue toAttributeValue(Object value) {

        if (value == null) {
            return AttributeValue.builder().nul(true).build();
        }

        if (value instanceof String s) {
            return AttributeValue.builder().s(s).build();
        }

        if (value instanceof Number n) {
            return AttributeValue.builder().n(n.toString()).build();
        }

        if (value instanceof Boolean b) {
            return AttributeValue.builder().bool(b).build();
        }

        if (value instanceof Map<?, ?> map) {
            Map<String, AttributeValue> avMap = new LinkedHashMap<>();
            map.forEach((k, v) ->
                    avMap.put(String.valueOf(k), toAttributeValue(v))
            );
            return AttributeValue.builder().m(avMap).build();
        }

        if (value instanceof List<?> list) {
            List<AttributeValue> avList = new ArrayList<>();
            for (Object item : list) {
                avList.add(toAttributeValue(item));
            }
            return AttributeValue.builder().l(avList).build();
        }

        throw new IllegalArgumentException(
                "Unsupported value type: " + value.getClass()
        );
    }

    public static Map<String, AttributeValue> toItem(Map<String, Object> data) {
        Map<String, AttributeValue> item = new LinkedHashMap<>();
        data.forEach((k, v) -> item.put(k, toAttributeValue(v)));
        return item;
    }
}
