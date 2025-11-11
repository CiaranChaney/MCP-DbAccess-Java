package com.ciaranchaney.mcpdb.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class OpaClient {

    private final WebClient http;
    private final String path;
    private final ObjectMapper M = new ObjectMapper();

    public OpaClient(
            WebClient.Builder builder,
            @Value("${opa.baseUrl}") String baseUrl,
            @Value("${opa.policyPath}") String policyPath
    ) {
        this.http = builder.baseUrl(baseUrl).build();
        this.path = policyPath;
    }

    public boolean isAllowed(String principal, String tenant, String tool, JsonNode args){
        try {
            JsonNode input = M.valueToTree(Map.of(
                    "principal", principal,
                    "tenant", tenant,
                    "tool", tool,
                    "args", args
            ));
            JsonNode resp = http.post()
                    .uri(path)
                    .bodyValue(Map.of("input", input))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .onErrorReturn(M.createObjectNode().put("result", false))
                    .block(); // fine for a quick POC; later you can make this async
            return resp != null && resp.path("result").asBoolean(false);
        } catch (Exception e){
            return true; // fail-open
        }
    }
}


