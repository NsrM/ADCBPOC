package org.example.util;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Component
public class DynamoAttributeParser {

    // The Problem with this parsing is
    // The ns, ss and m are initialized with DefaultAutoConstructList type by the SDK
    // and appears to be non null ... For my l() type case m() is becoming true and returning
    // null value

//    public static Object parse(AttributeValue av) {
//
//        if (av.s() != null) return av.s();
//        if (av.n() != null) return new BigDecimal(av.n()); // preserve number
//        if (av.bool() != null) return av.bool();
//        if (av.nul() != null && av.nul()) return null;
//
//        if (av.m() != null) {
//            Map<String, Object> map = new LinkedHashMap<>();
//            av.m().forEach((k, v) -> map.put(k, parse(v)));
//            return map;
//        }
//
//        if (av.l() != null) {
//            List<Object> list = new ArrayList<>();
//            av.l().forEach(v -> list.add(parse(v)));
//            return list;
//        }
//
//        if (av.ss() != null) return new ArrayList<>(av.ss());
//        if (av.ns() != null)
//            return av.ns().stream().map(BigDecimal::new).toList();
//
//        throw new IllegalArgumentException("Unsupported AttributeValue: " + av);
//    }



    /**
     * Converts a single AttributeValue into a Java Object.
     */
    public static Object parse(AttributeValue av) {

        // ---------- Scalar Types ----------

        // String
        if (av.s() != null) {
            return av.s();
        }

        // Number (use BigDecimal to avoid precision loss)
        if (av.n() != null) {
            return new BigDecimal(av.n());
        }

        // Boolean
        if (av.bool() != null) {
            return av.bool();
        }

        // NULL
        if (av.nul() != null && av.nul()) {
            return null;
        }

        // ---------- Map Type ----------
        // MUST use hasM() – never check null
        if (av.hasM()) {
            Map<String, Object> map = new LinkedHashMap<>();
            av.m().forEach((key, value) -> {
                map.put(key, parse(value));
            });
            return map;
        }

        // ---------- List Type ----------
        // MUST use hasL() – DefaultSdkAutoConstructList is never null
        if (av.hasL()) {
            List<Object> list = new ArrayList<>();
            for (AttributeValue value : av.l()) {
                list.add(parse(value));
            }
            return list;
        }

        // ---------- Set Types ----------

        // String Set
        if (av.hasSs()) {
            return new ArrayList<>(av.ss());
        }

        // Number Set
        if (av.hasNs()) {
            return av.ns()
                    .stream()
                    .map(BigDecimal::new)
                    .toList();
        }

        // Binary Set (rare, but supported)
        if (av.hasBs()) {
            return av.bs();
        }

        throw new IllegalArgumentException(
                "Unsupported AttributeValue type: " + av
        );
    }

    /**
     * Entry method: converts a single AttributeValue
     * into an equivalent Java Object.
     */
    public static Object parseByType(AttributeValue av) {

        if (av == null) {
            return null;
        }

        switch (av.type()) {

            case S:
                // String
                return av.s();

            case N:
                // Number (use BigDecimal to avoid precision loss)
                return new BigDecimal(av.n());

            case BOOL:
                return av.bool();

            case NUL:
                return null;

            case M:
                // Map<String, AttributeValue> → Map<String, Object>
                return parseMap(av.m());

            case L:
                // List<AttributeValue> → List<Object>
                return parseList(av.l());

            case SS:
                // String Set → List<String>
                return new ArrayList<>(av.ss());

            case NS:
                // Number Set → List<BigDecimal>
                return av.ns().stream()
                        .map(BigDecimal::new)
                        .toList();

            case BS:
                // Binary Set → List<byte[]>
                return av.bs().stream()
                        .map(b -> b.asByteArray())
                        .toList();

            case B:
                // Binary → byte[]
                return av.b().asByteArray();

            default:
                throw new IllegalArgumentException(
                        "Unsupported DynamoDB AttributeValue type: " + av.type()
                );
        }
    }

    /**
     * Parses a DynamoDB Map attribute.
     */
    private static Map<String, Object> parseMap(Map<String, AttributeValue> map) {

        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, AttributeValue> entry : map.entrySet()) {
            result.put(entry.getKey(), parse(entry.getValue()));
        }

        return result;
    }

    /**
     * Parses a DynamoDB List attribute.
     */
    private static List<Object> parseList(List<AttributeValue> list) {

        List<Object> result = new ArrayList<>();

        for (AttributeValue av : list) {
            result.add(parse(av));
        }

        return result;
    }

    /**
     * Converts DynamoDB item → Map<String, Object>
     */
    public static Map<String, Object> parseItem(
            Map<String, AttributeValue> item) {

        Map<String, Object> result = new LinkedHashMap<>();
        item.forEach((k, v) -> result.put(k, parse(v)));
        return result;
    }



}
