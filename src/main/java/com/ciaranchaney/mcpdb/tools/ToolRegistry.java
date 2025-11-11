package com.ciaranchaney.mcpdb.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();


    public void register(Tool tool){ tools.put(tool.name(), tool); }


    public List<Map<String,Object>> describe(){
        List<Map<String,Object>> arr = new ArrayList<>();
        for (Tool t : tools.values()) {
            arr.add(Map.of(
                    "name", t.name(),
                    "inputSchema", t.inputSchema()
            ));
        }
        return arr;
    }


    public Optional<Tool> get(String name){ return Optional.ofNullable(tools.get(name)); }
}
