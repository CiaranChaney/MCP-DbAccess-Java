package com.ciaranchaney.mcpdb.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

public record JsonRpcResponse(
        String jsonrpc,
        String id,
        @JsonInclude(JsonInclude.Include.NON_NULL) Object result,
        @JsonInclude(JsonInclude.Include.NON_NULL) Object error
) {
    public static JsonRpcResponse ok(String id, Object result){
        return new JsonRpcResponse("2.0", id, result, null);
    }
    public static JsonRpcResponse err(String id, int code, String message){
        return new JsonRpcResponse("2.0", id, null, Map.of("code", code, "message", message));
    }
}
