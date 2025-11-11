package com.ciaranchaney.mcpdb.tools;

import com.fasterxml.jackson.databind.JsonNode;

public interface Tool {
    String name();
    JsonNode inputSchema();
    Object execute(Context ctx, JsonNode input) throws Exception;


    record Context(String principal, String tenant) {}
}
