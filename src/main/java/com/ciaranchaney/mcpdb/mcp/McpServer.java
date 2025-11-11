package com.ciaranchaney.mcpdb.mcp;

import com.ciaranchaney.mcpdb.tools.Tool;
import com.ciaranchaney.mcpdb.tools.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ciaranchaney.mcpdb.audit.AuditLogger;
import com.ciaranchaney.mcpdb.policy.OpaClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class McpServer {

    private final ToolRegistry registry;
    private final OpaClient opa;
    private final AuditLogger audit;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${mcp.maxRows:10000}")
    int maxRows;
    @Value("${mcp.timeoutMs:5000}")
    int timeoutMs;
    @Value("${mcp.requireAuth:true}")
    boolean requireAuth;

    public McpServer(ToolRegistry registry, OpaClient opa, AuditLogger audit) {
        this.registry = registry;
        this.opa = opa;
        this.audit = audit;
    }

    public void run(){
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8))) {


            String line;
            while ((line = in.readLine()) != null) {
                JsonRpcRequest req = mapper.readValue(line, JsonRpcRequest.class);
                JsonRpcResponse resp = handle(req);
                out.write(mapper.writeValueAsString(resp));
                out.write("\n");
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Expose a public handler for HTTP callers
    public JsonRpcResponse handleRequest(JsonRpcRequest req) {
        return handle(req);
    }

    private JsonRpcResponse handle(JsonRpcRequest req){
        String id = req.id();
        try {
            switch (req.method()) {
                case "initialize":
                    Map<String,Object> init = new HashMap<>();
                    init.put("tools", registry.describe());
                    return JsonRpcResponse.ok(id, init);


                case "tools/execute":
                    JsonNode p = mapper.valueToTree(req.params());

                    // Defensive checks to give helpful errors instead of NPE
                    if (p == null) {
                        return JsonRpcResponse.err(id, -32602, "Missing params");
                    }
                    JsonNode nameNode = p.get("name");
                    if (nameNode == null || nameNode.isNull() || nameNode.asText().isBlank()) {
                        return JsonRpcResponse.err(id, -32602, "Missing 'name'");
                    }

                    String toolName = nameNode.asText();
                    JsonNode args = p.has("arguments") ? p.get("arguments") : mapper.createObjectNode();


                    String principal = safeHeader(p, "authorization");
                    String tenant = safeHeader(p, "x-tenant");
                    Tool.Context ctx = new Tool.Context(principal, tenant);


                    var tool = registry.get(toolName).orElseThrow(() -> new IllegalArgumentException("Unknown tool: " + toolName));


                    if (!opa.isAllowed(principal, tenant, toolName, args)) {
                        return JsonRpcResponse.err(id, 403, "Forbidden by policy");
                    }


                    Object result = tool.execute(ctx, args);
                    audit.record(Instant.now(), principal, tenant, toolName, args, result);
                    return JsonRpcResponse.ok(id, Map.of("output", result));


                default:
                    return JsonRpcResponse.err(id, -32601, "Method not found");
            }
        } catch (Exception ex){
            return JsonRpcResponse.err(id, -32000, ex.getMessage());
        }
    }

    private String safeHeader(JsonNode p, String name){
        JsonNode headers = p.get("headers");
        if (headers != null && headers.has(name)) return headers.get(name).asText();
        return null;
    }


}
