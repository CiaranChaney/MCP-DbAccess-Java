package com.ciaranchaney.mcpdb.tools;

import com.ciaranchaney.mcpdb.db.Repositories;
import com.ciaranchaney.mcpdb.schemas.JsonSchemas;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class ExportOrdersTool implements Tool {

    private final Repositories repo;
    private final ToolRegistry registry;
    @Value("${mcp.maxRows:10000}") int maxRows;




    public ExportOrdersTool(Repositories repo, ToolRegistry registry){
        this.repo = repo; this.registry = registry;
    }


    @PostConstruct
    public void init(){ registry.register(this); }


    @Override public String name() { return "exportOrders"; }
    @Override public com.fasterxml.jackson.databind.JsonNode inputSchema() { return JsonSchemas.exportOrders(); }


    @Override
    public Object execute(Context ctx, JsonNode input) {
        LocalDate from = LocalDate.parse(input.get("from").asText());
        LocalDate to = LocalDate.parse(input.get("to").asText());
        int limit = input.has("limit") ? Math.min(input.get("limit").asInt(), maxRows) : Math.min(1000, maxRows);
        List<Map<String,Object>> rows = repo.exportOrders(from, to, limit);
        boolean truncated = rows.size() >= limit;
        return Map.of("rows", rows, "truncated", truncated);
    }
}
