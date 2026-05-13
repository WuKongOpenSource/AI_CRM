# AI_CRM MCP Server

This directory contains a standalone MCP server for the main AI_CRM `backend`.
It intentionally does not call or expose anything from `sync_data`.

## First-phase tools

- `global_search`
- `list_customers`
- `get_customer_detail`
- `list_customer_contacts`
- `list_customer_followups`
- `list_my_tasks`
- `list_my_schedules`
- `search_knowledge`
- `get_knowledge_detail`

All tools call existing backend HTTP APIs and rely on the backend for
authentication, tenant context, permissions, and data visibility.

## Configuration

Set these environment variables in the MCP client configuration:

```powershell
$env:AICRM_BASE_URL = "http://127.0.0.1:8088"
$env:AICRM_TOKEN = "Bearer your-login-token"
$env:AICRM_TOKEN_HEADER = "Manager-Token"
```

`AICRM_BASE_URL` defaults to `http://127.0.0.1:8088`.
`AICRM_TOKEN_HEADER` defaults to `Manager-Token`.

The backend accepts either a raw token or a `Bearer ` token value in the
configured token header.

## Local run

```powershell
cd D:\workspace\AI_CRM\mcp_server
npm install
npm run build
npm start
```

For MCP clients that run a command directly:

```json
{
  "mcpServers": {
    "ai-crm": {
      "command": "node",
      "args": ["D:/workspace/AI_CRM/mcp_server/dist/index.js"],
      "env": {
        "AICRM_BASE_URL": "http://127.0.0.1:8088",
        "AICRM_TOKEN": "Bearer your-login-token",
        "AICRM_TOKEN_HEADER": "Manager-Token"
      }
    }
  }
}
```
