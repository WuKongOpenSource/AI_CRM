#!/usr/bin/env node

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

type HttpMethod = "GET" | "POST";

interface BackendResult<T = unknown> {
  code?: number;
  msg?: string;
  data?: T;
  [key: string]: unknown;
}

const DEFAULT_BASE_URL = "http://127.0.0.1:8088";
const DEFAULT_TOKEN_HEADER = "Manager-Token";

const baseUrl = normalizeBaseUrl(process.env.AICRM_BASE_URL ?? DEFAULT_BASE_URL);
const token = process.env.AICRM_TOKEN;
const tokenHeader = process.env.AICRM_TOKEN_HEADER ?? DEFAULT_TOKEN_HEADER;

const server = new McpServer({
  name: "ai-crm-mcp-server",
  version: "0.1.0"
});

const longIdInput = z.union([
  z.string().regex(/^\d+$/, "ID must contain only digits."),
  z.number().int().positive()
]);

const pageInput = {
  page: z.number().int().positive().default(1).describe("Page number, starting from 1."),
  limit: z.number().int().positive().max(100).default(15).describe("Page size, capped at 100 by AI_CRM.")
};

server.registerTool(
  "global_search",
  {
    title: "Global CRM Search",
    description: "Search across CRM modules such as customers, contacts, tasks, schedules, and knowledge.",
    inputSchema: {
      keyword: z.string().min(1).describe("Search keyword."),
      entityType: z.enum(["customer", "contact", "task", "schedule", "knowledge"]).optional()
        .describe("Optional entity type filter."),
      ...pageInput
    }
  },
  async ({ keyword, entityType, page, limit }) => {
    const data = await post("/search/global", { keyword, entityType, page, limit });
    return jsonToolResult(data);
  }
);

server.registerTool(
  "list_customers",
  {
    title: "List Customers",
    description: "Query a paginated customer list with common filters.",
    inputSchema: {
      keyword: z.string().optional().describe("Keyword matched against customer data."),
      stage: z.string().optional().describe("Customer stage."),
      level: z.string().optional().describe("Customer level."),
      ownerId: longIdInput.optional().describe("Owner user ID."),
      industry: z.string().optional().describe("Industry filter."),
      tag: z.string().optional().describe("Customer tag filter."),
      source: z.string().optional().describe("Customer source filter."),
      sortBy: z.enum(["createTime", "quotation", "lastContactTime", "nextFollowTime", "contactCount"]).optional(),
      sortOrder: z.enum(["asc", "desc"]).optional(),
      ...pageInput
    }
  },
  async (args) => {
    const data = await post("/customer/queryPageList", pruneEmpty(args));
    return jsonToolResult(data);
  }
);

server.registerTool(
  "get_customer_detail",
  {
    title: "Get Customer Detail",
    description: "Fetch full customer detail by customer ID.",
    inputSchema: {
      customerId: longIdInput.describe("Customer ID.")
    }
  },
  async ({ customerId }) => {
    const data = await get(`/customer/detail/${customerId}`);
    return jsonToolResult(data);
  }
);

server.registerTool(
  "list_customer_contacts",
  {
    title: "List Customer Contacts",
    description: "List contacts that belong to a customer.",
    inputSchema: {
      customerId: longIdInput.describe("Customer ID.")
    }
  },
  async ({ customerId }) => {
    const data = await post("/contact/queryByCustomer", undefined, { customerId });
    return jsonToolResult(data);
  }
);

server.registerTool(
  "list_customer_followups",
  {
    title: "List Customer Follow-Ups",
    description: "List follow-up records for a customer.",
    inputSchema: {
      customerId: longIdInput.describe("Customer ID.")
    }
  },
  async ({ customerId }) => {
    const data = await post("/followup/queryByCustomer", undefined, { customerId });
    return jsonToolResult(data);
  }
);

server.registerTool(
  "list_my_tasks",
  {
    title: "List My Tasks",
    description: "List current user's tasks by filter.",
    inputSchema: {
      filter: z.enum(["all", "today", "thisWeek", "overdue"]).default("all")
    }
  },
  async ({ filter }) => {
    const data = await get("/task/myTasks", { filter });
    return jsonToolResult(data);
  }
);

server.registerTool(
  "list_my_schedules",
  {
    title: "List My Schedules",
    description: "List current user's schedules by filter.",
    inputSchema: {
      filter: z.enum(["all", "today", "thisWeek"]).default("all")
    }
  },
  async ({ filter }) => {
    const data = await get("/schedule/mySchedules", { filter });
    return jsonToolResult(data);
  }
);

server.registerTool(
  "search_knowledge",
  {
    title: "Search Knowledge Base",
    description: "Run AI_CRM knowledge AI search and return the answer plus referenced documents.",
    inputSchema: {
      keyword: z.string().min(1).describe("Search keyword or question."),
      type: z.string().optional().describe("Optional knowledge type filter."),
      limit: z.number().int().positive().max(20).default(5).describe("Maximum number of referenced documents.")
    }
  },
  async (args) => {
    const data = await post("/knowledge/ai-search", pruneEmpty(args));
    return jsonToolResult(data);
  }
);

server.registerTool(
  "get_knowledge_detail",
  {
    title: "Get Knowledge Detail",
    description: "Fetch knowledge document metadata by ID.",
    inputSchema: {
      knowledgeId: longIdInput.describe("Knowledge document ID.")
    }
  },
  async ({ knowledgeId }) => {
    const data = await get(`/knowledge/detail/${knowledgeId}`);
    return jsonToolResult(data);
  }
);

async function get(path: string, query?: Record<string, unknown>) {
  return request("GET", path, undefined, query);
}

async function post(path: string, body?: unknown, query?: Record<string, unknown>) {
  return request("POST", path, body, query);
}

async function request(method: HttpMethod, path: string, body?: unknown, query?: Record<string, unknown>) {
  const url = new URL(`${baseUrl}${path}`);
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== null && value !== "") {
      url.searchParams.set(key, String(value));
    }
  }

  const headers: Record<string, string> = {
    Accept: "application/json"
  };
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  if (token) {
    headers[tokenHeader] = token;
  }

  const response = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  });

  const text = await response.text();
  const parsed = parseJson(text);

  if (!response.ok) {
    throw new Error(`AI_CRM backend returned HTTP ${response.status}: ${formatErrorBody(parsed, text)}`);
  }

  if (isBackendError(parsed)) {
    throw new Error(`AI_CRM backend returned code ${parsed.code}: ${parsed.msg ?? "unknown error"}`);
  }

  return parsed;
}

function jsonToolResult(data: unknown) {
  return {
    content: [
      {
        type: "text" as const,
        text: JSON.stringify(data, null, 2)
      }
    ]
  };
}

function normalizeBaseUrl(value: string) {
  return value.replace(/\/+$/, "");
}

function parseJson(text: string) {
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return text;
  }
}

function formatErrorBody(parsed: unknown, raw: string) {
  if (typeof parsed === "string") {
    return parsed;
  }
  if (parsed === null || parsed === undefined) {
    return raw;
  }
  return JSON.stringify(parsed);
}

function isBackendError(value: unknown): value is BackendResult {
  if (!value || typeof value !== "object") {
    return false;
  }
  const result = value as BackendResult;
  return typeof result.code === "number" && result.code !== 0;
}

function pruneEmpty<T extends Record<string, unknown>>(value: T) {
  return Object.fromEntries(
    Object.entries(value).filter(([, entry]) => entry !== undefined && entry !== null && entry !== "")
  );
}

const transport = new StdioServerTransport();
await server.connect(transport);
