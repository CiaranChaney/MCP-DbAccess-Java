package com.ciaranchaney.mcpdb.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class AuditLogger {
    private static final ObjectMapper M = new ObjectMapper();


    public void record(Instant ts, String principal, String tenant, String tool, JsonNode args, Object result){
        try {
            var log = Map.of(
                    "ts", ts.toString(),
                    "principal", principal,
                    "tenant", tenant,
                    "tool", tool,
                    "args", args,
                    "result_meta", Map.of("class", result == null ? "null" : result.getClass().getSimpleName())
            );
            System.err.println(M.writeValueAsString(log));
        } catch (Exception ignored) {}
    }
}
