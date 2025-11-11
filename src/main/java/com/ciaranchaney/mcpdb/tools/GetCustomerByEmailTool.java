package com.ciaranchaney.mcpdb.tools;

import com.ciaranchaney.mcpdb.db.Repositories;
import com.ciaranchaney.mcpdb.schemas.JsonSchemas;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetCustomerByEmailTool implements Tool {

    private final Repositories repo;
    private final ToolRegistry registry;

    @Autowired
    ObjectMapper mapper;


    public GetCustomerByEmailTool(Repositories repo, ToolRegistry registry){
        this.repo = repo; this.registry = registry;
    }


    @PostConstruct
    public void init(){ registry.register(this); }


    @Override public String name() { return "getCustomerByEmail"; }
    @Override public JsonNode inputSchema() { return JsonSchemas.emailLookup(); }


    @Override
    public Object execute(Context ctx, JsonNode input) throws Exception {
        String email = input.get("email").asText();
        return repo.getCustomerByEmail(email);
    }
}
