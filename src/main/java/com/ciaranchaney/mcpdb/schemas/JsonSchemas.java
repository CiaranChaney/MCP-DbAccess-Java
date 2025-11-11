package com.ciaranchaney.mcpdb.schemas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonSchemas {

    private static final ObjectMapper M = new ObjectMapper();


    public static JsonNode emailLookup() {
        String s = """
        {"type":"object","properties":{
        "email":{"type":"string","format":"email"}
        },"required":["email"]}
        """;
        try { return M.readTree(s); } catch (Exception e) { throw new RuntimeException(e); }
    }


    public static JsonNode exportOrders() {
        String s = """
        {"type":"object","properties":{
        "from":{"type":"string","format":"date"},
        "to":{"type":"string","format":"date"},
        "limit":{"type":"integer","maximum":10000,"default":1000}
        },"required":["from","to"]}
        """;
        try { return M.readTree(s); } catch (Exception e) { throw new RuntimeException(e); }
    }
}
