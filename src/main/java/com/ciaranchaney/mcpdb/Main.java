package com.ciaranchaney.mcpdb;

import com.ciaranchaney.mcpdb.mcp.McpServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(Main.class, args);
        McpServer server = ctx.getBean(McpServer.class);
        server.run();
    }
}
