package com.ciaranchaney.mcpdb.mcp;

public record JsonRpcRequest(String jsonrpc, String method, Object params, String id) {}
