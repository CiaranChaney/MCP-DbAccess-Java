# Java MCP DB Proxy — Proof of Concept

This project implements a **Model Context Protocol (MCP)** server in **Java (Spring Boot)** to provide **controlled, auditable, policy‑enforced database access** for AI agents and copilots.

It is intended to act as a **safe data access boundary** between AI systems and internal databases — preventing:

* credential exposure,
* unrestricted SQL access,
* accidental data leaks,
* policy or compliance violations.

Instead of exposing raw SQL, the MCP server exposes **limited, intentional, domain-level tools**, defined and governed centrally.

---

## 🧭 Architecture Overview

```
AI Agent / Copilot / ChatGPT Desktop
          │  (MCP JSON-RPC)
          ▼
   Java MCP Server (this project)
   ├─ Tool Registry (list of allowed actions)
   ├─ Input Validation (JSON Schema)
   ├─ AuthN (Bearer/JWT placeholder)
   ├─ AuthZ (OPA / Rego Policy checks)
   ├─ Auditing (structured logs)
   └─ Safe DB Access (Prepared SQL)
          │
          ▼
       PostgreSQL / Other DBs
```

### Key Properties

| Feature                                    | Purpose                                    |
| ------------------------------------------ | ------------------------------------------ |
| **No DB credentials exposed to agents**    | DB secrets stay server-side only           |
| **Policy-as-code (OPA)**                   | Central control of who can run what        |
| **Tool-based access model**                | Only approved, parameter-validated queries |
| **Audit logging**                          | Every tool call and result is recorded     |
| **Works with any AI model supporting MCP** | ChatGPT Desktop, local agents, custom LLMs |

---

## 🚀 Quick Start (Developer Onboarding)

### 1) Prerequisites

| Requirement             | Version                  |
| ----------------------- | ------------------------ |
| Java                    | 21+                      |
| Gradle                  | Bundled wrapper included |
| PostgreSQL              | Any recent version       |
| OPA (Open Policy Agent) | `v0.65+`                 |

---

### 2) Configure Database

Ensure a database exists and export credentials:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/app
export DB_USER=app
export DB_PASSWORD=app
```

### 3) Start OPA with the provided policy

```bash
opa run --server --addr :8181 ./policy.rego
```

You should see:

```
Server started on :8181
```

---

### 4) Run the MCP Server

```bash
./gradlew bootRun
```

This process now listens via **STDIN/STDOUT** for MCP JSON-RPC requests.

---

### 5) Test a Tool Call Manually

```bash
echo '{
  "jsonrpc":"2.0","id":"1","method":"tools/execute",
  "params":{
    "name":"getCustomerByEmail",
    "arguments":{"email":"alice@example.com"},
    "headers":{
      "authorization":"Bearer local-test",
      "x-tenant":"team-a"
    }
  }
}' | ./gradlew run
```

Expected output:

```json
{"jsonrpc":"2.0","id":"1","result":{"output":{"id":123,"email":"alice@example.com","name":"Alice"}}}
```

---

## 🧩 Integrating with ChatGPT Desktop

Add to your `~/.config/chatgpt/settings.jsonc` (Mac/Linux) or `%AppData%` equivalent:

```jsonc
{
  "mcpServers": {
    "company-db-proxy": {
      "command": "java",
      "args": ["-jar", "/absolute/path/java-mcp-db-proxy.jar"],
      "env": {
        "DB_URL": "jdbc:postgresql://localhost:5432/app",
        "DB_USER": "app",
        "DB_PASSWORD": "app",
        "OPA_URL": "http://localhost:8181"
      }
    }
  }
}
```

Restart ChatGPT Desktop → The tools will auto‑register.

---

## 🛡️ Security & Governance Model

| Layer            | Responsibility                                       |
| ---------------- | ---------------------------------------------------- |
| Input Schemas    | Enforce required arguments & type safety             |
| AuthN            | Confirms identity (JWT or bearer token)              |
| AuthZ (OPA)      | Approves or rejects the tool call based on rules     |
| SQL Access Layer | Parameterized queries + row caps                     |
| Auditing         | Logs principal, tool, arguments, and result metadata |

OPA allows centralized enforcement of:

* team / role permissions
* maximum date ranges
* max row limits
* field or column masking

---

## 🔧 Adding New Tools

1. Create a class implementing `Tool`.
2. Define an input JSON schema.
3. Implement safe SQL or service logic.
4. Register the tool in the `ToolRegistry` (automatic via `@PostConstruct`).
5. Update OPA policy to allow/deny the new tool.

This keeps access **explicit and reviewable**.

---

## ✅ Roadmap / Next Enhancements

* JWT validation via JWKS
* Tenant-aware schema routing or Postgres RLS
* WebSocket MCP transport for remote hosting
* Column-level masking rules in OPA
* Tool versioning & deprecation framework

---

## 📜 License

Internal / Private — for organizational use.

---

## Contact

Platform / Security Engineering Team

---

This POC is designed to be a **secure, reviewable foundation** for providing AI systems with **controlled operational access to internal data**, without exposing the underlying databases or violating governance controls.
