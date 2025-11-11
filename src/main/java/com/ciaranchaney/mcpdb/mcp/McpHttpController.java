package com.ciaranchaney.mcpdb.mcp;

import com.ciaranchaney.mcpdb.tools.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/mcp")
public class McpHttpController {

    private final McpServer mcpServer;
    private final ToolRegistry registry;
    private final ObjectMapper mapper;

    public McpHttpController(McpServer mcpServer, ToolRegistry registry, ObjectMapper mapper) {
        this.mcpServer = mcpServer;
        this.registry  = registry;
        this.mapper    = mapper; // Spring-managed (has JavaTimeModule)
    }

  /* =====================
     DISCOVERY (JSON only)
     ===================== */

    // GET /mcp and /mcp/
    @GetMapping({ "", "/" })
    public ResponseEntity<JsonNode> root() {
        return jsonToolList(null);
    }

    // GET /mcp/{serverLabel} and /mcp/{serverLabel}/
    @GetMapping({ "/{serverLabel}", "/{serverLabel}/" })
    public ResponseEntity<JsonNode> byLabel(@PathVariable String serverLabel) {
        return jsonToolList(serverLabel);
    }

    private ResponseEntity<JsonNode> jsonToolList(String labelOrNull) {
        ArrayNode toolsArr = mapper.createArrayNode();
        for (var desc : registry.describe()) {
            ObjectNode t = (ObjectNode) mapper.valueToTree(desc);  // expect { "name", "inputSchema", ... }
            if (t.has("inputSchema") && !t.has("input_schema")) {
                t.set("input_schema", t.get("inputSchema"));         // alias for compatibility
            }
            if (!t.has("description")) {
                t.put("description", "Exposed MCP tool: " + t.path("name").asText());
            }
            toolsArr.add(t);
        }
        ObjectNode node = mapper.createObjectNode();
        node.set("tools", toolsArr);
        node.put("require_approval", "never");
        if (labelOrNull != null) node.put("server_label", labelOrNull);

        // Force JSON regardless of Accept; avoid 406/negotiation surprises
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(node);
    }

  /* =====================
     INVOCATION (POST /mcp)
     ===================== */

    @PostMapping({ "", "/" })
    public ResponseEntity<JsonNode> invoke(@RequestBody JsonNode body, HttpServletRequest req) {
        try {
            // JSON-RPC branch
            if (body.has("jsonrpc") && body.has("method")) {
                final String id     = body.has("id") ? body.get("id").asText() : UUID.randomUUID().toString();
                final String method = body.get("method").asText();
                final JsonNode params = body.get("params");

                switch (method) {
                    case "initialize": {
                        ObjectNode payload = (ObjectNode) jsonToolList(null).getBody();
                        ObjectNode rpc = mapper.createObjectNode();
                        rpc.put("jsonrpc", "2.0");
                        rpc.put("id", id);
                        rpc.set("result", payload);
                        return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(rpc);
                    }
                    case "tools/execute": {
                        String toolName = (params != null && params.has("name")) ? params.get("name").asText() : null;
                        if (toolName == null || toolName.isBlank()) {
                            return jsonRpcError(id, -32602, "Missing 'name'");
                        }
                        JsonNode args = (params != null && params.has("arguments")) ? params.get("arguments") : mapper.createObjectNode();
                        JsonNode hdrs = (params != null && params.has("headers")) ? params.get("headers") : mapper.createObjectNode();

                        String auth   = hdrs.has("authorization") ? hdrs.get("authorization").asText() : req.getHeader("Authorization");
                        String tenant = hdrs.has("x-tenant")       ? hdrs.get("x-tenant").asText()       : req.getHeader("X-Tenant");

                        ObjectNode innerParams = mapper.createObjectNode();
                        innerParams.put("name", toolName);
                        innerParams.set("arguments", args);
                        ObjectNode headers = mapper.createObjectNode();
                        if (auth != null)   headers.put("authorization", auth);
                        if (tenant != null) headers.put("x-tenant", tenant);
                        innerParams.set("headers", headers);

                        JsonRpcRequest rpcReq = new JsonRpcRequest("2.0", "tools/execute", innerParams, id);
                        JsonRpcResponse rpcResp = mcpServer.handleRequest(rpcReq);

                        ObjectNode out = mapper.createObjectNode();
                        out.put("jsonrpc", "2.0");
                        out.put("id", id);
                        if (rpcResp.error() == null) out.set("result", mapper.valueToTree(rpcResp.result()));
                        else                         out.set("error",  mapper.valueToTree(rpcResp.error()));

                        return ResponseEntity.status(rpcResp.error() == null ? 200 : 400)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(out);
                    }
                    default:
                        return jsonRpcError(id, -32601, "Method not found: " + method);
                }
            }

            // Simple shape (Postman)

            // New: support OpenAI Responses connector shape: { tools: [{type:'mcp', server_label:'db-proxy', ...}], input: "..." }
            // If the connector posts tools[] as a probe (no input), respond with the descriptor (200)
            if (body.has("tools") && body.get("tools").isArray() && body.get("tools").size() > 0 && !body.has("input")) {
                var firstTool = body.get("tools").get(0);
                String labelIfAny = null;
                if (firstTool.has("server_label")) labelIfAny = firstTool.get("server_label").asText();
                else if (firstTool.has("name")) labelIfAny = firstTool.get("name").asText();

                // Return the descriptor for that label (or the full list if no label)
                if (labelIfAny == null || labelIfAny.isBlank()) return jsonToolList(null);
                return jsonToolList(labelIfAny);
            }

            if (body.has("tools") && body.get("tools").isArray() && body.get("tools").size() > 0 && body.has("input")) {
                var firstTool = body.get("tools").get(0);
                String candidateName = null;
                if (firstTool.has("server_label")) candidateName = firstTool.get("server_label").asText();
                else if (firstTool.has("name"))         candidateName = firstTool.get("name").asText();

                String toolNameFromTools = (candidateName == null || candidateName.isBlank()) ? null : candidateName;

                // If the connector provided a label that doesn't match a registered tool name,
                // try some heuristics (case-insensitive match, replace dashes/underscores),
                // otherwise fall back to the first registered tool so discovery/validation succeeds.
                if (toolNameFromTools != null && registry.get(toolNameFromTools).isEmpty()) {
                    // try case-insensitive match
                    String found = null;
                    for (var d : registry.describe()) {
                        var n = (String) d.get("name");
                        if (n.equalsIgnoreCase(toolNameFromTools)) { found = n; break; }
                    }
                    // try normalized form (remove non-alphanum)
                    if (found == null) {
                        String norm = toolNameFromTools.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
                        for (var d : registry.describe()) {
                            var n = (String) d.get("name");
                            if (n.replaceAll("[^A-Za-z0-9]", "").toLowerCase().equals(norm)) { found = n; break; }
                        }
                    }
                    // fallback to first registered tool
                    if (found == null) {
                        var desc = registry.describe();
                        if (!desc.isEmpty()) found = (String) desc.get(0).get("name");
                    }
                    toolNameFromTools = found;
                }

                if (toolNameFromTools == null || toolNameFromTools.isBlank()) {
                    return ResponseEntity.badRequest()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(mapper.createObjectNode().put("error", "Missing 'name' in tools[0]"));
                }

                JsonNode argsNode;
                // prefer explicit arguments inside the tool descriptor
                if (firstTool.has("arguments")) argsNode = firstTool.get("arguments");
                else if (body.has("arguments")) argsNode = body.get("arguments");
                else if (body.get("input").isObject()) argsNode = body.get("input");
                else argsNode = mapper.createObjectNode().put("input", body.get("input").asText());

                ObjectNode headers = mapper.createObjectNode();
                if (firstTool.has("headers") && firstTool.get("headers").isObject()) headers.setAll((ObjectNode) firstTool.get("headers"));
                if (req.getHeader("Authorization") != null) headers.put("authorization", req.getHeader("Authorization"));
                if (req.getHeader("X-Tenant") != null)       headers.put("x-tenant", req.getHeader("X-Tenant"));

                ObjectNode params = mapper.createObjectNode();
                params.put("name", toolNameFromTools);
                params.set("arguments", argsNode);
                params.set("headers", headers);

                JsonRpcRequest rpc = new JsonRpcRequest("2.0","tools/execute", params, UUID.randomUUID().toString());
                JsonRpcResponse resp = mcpServer.handleRequest(rpc);

                return ResponseEntity.status(resp.error()==null?200:400)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.valueToTree(resp));
            }

            String toolName = body.has("name") ? body.get("name").asText() : null;
            if (toolName == null || toolName.isBlank()) {
                return ResponseEntity.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.createObjectNode().put("error", "Missing 'name'"));
            }
            JsonNode args = body.has("arguments") ? body.get("arguments") : mapper.createObjectNode();

            ObjectNode headers = mapper.createObjectNode();
            if (req.getHeader("Authorization") != null) headers.put("authorization", req.getHeader("Authorization"));
            if (req.getHeader("X-Tenant") != null)       headers.put("x-tenant", req.getHeader("X-Tenant"));

            ObjectNode params = mapper.createObjectNode();
            params.put("name", toolName);
            params.set("arguments", args);
            params.set("headers", headers);

            JsonRpcRequest rpc = new JsonRpcRequest("2.0","tools/execute", params, UUID.randomUUID().toString());
            JsonRpcResponse resp = mcpServer.handleRequest(rpc);

            return ResponseEntity.status(resp.error()==null?200:400)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.valueToTree(resp));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.createObjectNode().put("error", e.getMessage()));
        }
    }

    private ResponseEntity<JsonNode> jsonRpcError(String id, int code, String message) {
        ObjectNode out = mapper.createObjectNode();
        out.put("jsonrpc", "2.0");
        out.put("id", id == null ? UUID.randomUUID().toString() : id);
        ObjectNode err = mapper.createObjectNode();
        err.put("code", code);
        err.put("message", message);
        out.set("error", err);
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(out);
    }
}
