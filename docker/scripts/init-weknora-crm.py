#!/usr/bin/env python3
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request


API_URL = os.getenv("WEKNORA_API_URL", "http://app:8080/api/v1").rstrip("/")


class ApiError(Exception):
    def __init__(self, message, payload=None):
        super().__init__(message)
        self.payload = payload or {}


def log(message):
    print(f"[weknora-init] {message}", flush=True)


def http_json(method, path, payload=None, token=None, retries=20):
    url = f"{API_URL}{path}"
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"

    last_error = None
    for attempt in range(1, retries + 1):
        request = urllib.request.Request(url, data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=15) as response:
                body = response.read().decode("utf-8")
                return json.loads(body) if body else {}
        except urllib.error.HTTPError as exc:
            body = exc.read().decode("utf-8", errors="replace")
            try:
                parsed = json.loads(body)
            except json.JSONDecodeError:
                parsed = {"message": body}
            raise ApiError(parsed.get("message") or parsed.get("error", {}).get("message") or body, parsed)
        except Exception as exc:
            last_error = exc
            if attempt < retries:
                time.sleep(2)
                continue
            raise ApiError(str(last_error))


def login(email, password):
    response = http_json("POST", "/auth/login", {"email": email, "password": password}, retries=3)
    if not response.get("success"):
        raise ApiError(response.get("message", "login failed"), response)
    return response


def ensure_account():
    username = os.getenv("WEKNORA_INIT_USERNAME", "admin")
    email = os.getenv("WEKNORA_INIT_EMAIL", "").strip()
    password = os.getenv("WEKNORA_INIT_PASSWORD", "").strip()

    if not email or not password:
        raise ApiError("WEKNORA_INIT_EMAIL and WEKNORA_INIT_PASSWORD must be configured")

    try:
        return login(email, password)
    except ApiError:
        log(f"creating WeKnora account {email}")
        response = http_json(
            "POST",
            "/auth/register",
            {"username": username, "email": email, "password": password},
            retries=3,
        )
        if not response.get("success"):
            raise ApiError(response.get("message", "registration failed"), response)
        return login(email, password)


def list_models(token):
    response = http_json("GET", "/models", token=token)
    if response.get("success") and isinstance(response.get("data"), list):
        return response["data"]
    return []


def ensure_model(token, model_type, prefix, default_provider):
    name = os.getenv(f"{prefix}_NAME", "").strip()
    base_url = os.getenv(f"{prefix}_BASE_URL", "").strip()
    api_key = os.getenv(f"{prefix}_API_KEY", "").strip()
    provider = os.getenv(f"{prefix}_PROVIDER", default_provider).strip()

    missing = [
        key for key, value in (
            (f"{prefix}_NAME", name),
            (f"{prefix}_BASE_URL", base_url),
            (f"{prefix}_API_KEY", api_key),
        )
        if not value
    ]
    if missing:
        raise ApiError(f"{model_type} model config is incomplete; missing: {', '.join(missing)}")

    for model in list_models(token):
        if model.get("type") == model_type and model.get("name") == name:
            return model.get("id")

    parameters = {
        "base_url": base_url,
        "api_key": api_key,
        "provider": provider,
    }
    if model_type == "Embedding":
        dimension_value = os.getenv(f"{prefix}_DIMENSION", "").strip()
        if not dimension_value:
            raise ApiError(f"Embedding model config is incomplete; missing: {prefix}_DIMENSION")
        try:
            dimension = int(dimension_value)
        except ValueError:
            raise ApiError(f"Embedding model dimension must be an integer: {prefix}_DIMENSION")
        parameters["embedding_parameters"] = {
            "dimension": dimension,
            "truncate_prompt_tokens": 0,
        }

    payload = {
        "name": name,
        "type": model_type,
        "source": "remote",
        "description": "Created by AI CRM deployment initialization",
        "parameters": parameters,
    }
    response = http_json("POST", "/models", payload, token=token)
    if not response.get("success") or not response.get("data", {}).get("id"):
        raise ApiError(response.get("message", f"failed to create {model_type} model"), response)
    log(f"created {model_type} model {name}")
    return response["data"]["id"]


def list_knowledge_bases(token):
    response = http_json("GET", "/knowledge-bases", token=token)
    if response.get("success") and isinstance(response.get("data"), list):
        return response["data"]
    return []


def ensure_knowledge_base(token, llm_model_id, embedding_model_id):
    name = os.getenv("WEKNORA_INIT_KB_NAME", "CRM Default Knowledge Base").strip()
    for kb in list_knowledge_bases(token):
        if kb.get("name") == name:
            return kb.get("id")

    payload = {
        "name": name,
        "description": "Default knowledge base for AI CRM documents",
        "type": "document",
        "embedding_model_id": embedding_model_id,
        "summary_model_id": llm_model_id,
        "chunking_config": {
            "chunk_size": 512,
            "chunk_overlap": 100,
            "separators": ["\n\n", "\n", "。", "！", "？", ";", "；"],
        },
        "vlm_config": {"enabled": False, "model_id": ""},
    }
    response = http_json("POST", "/knowledge-bases", payload, token=token)
    if not response.get("success") or not response.get("data", {}).get("id"):
        raise ApiError(response.get("message", "failed to create knowledge base"), response)
    log(f"created knowledge base {name}")
    return response["data"]["id"]


def sql_quote(value):
    return "'" + str(value).replace("'", "''") + "'"


def upsert_crm_config(api_key, knowledge_base_id, enabled):
    db_host = os.getenv("CRM_DB_HOST", "postgres")
    db_port = os.getenv("CRM_DB_PORT", "5432")
    db_user = os.getenv("DB_USER", "postgres")
    db_password = os.getenv("DB_PASSWORD", "")
    db_name = os.getenv("CRM_DB_NAME", "wk_ai_crm")
    base_url = os.getenv("CRM_WEKNORA_BASE_URL", "http://app:8080/api/v1")

    values = [
        (101, "weknora_enabled", "true" if enabled else "false", "WeKnora enabled"),
        (102, "weknora_base_url", base_url, "WeKnora API base URL"),
        (103, "weknora_api_key", api_key or "", "WeKnora tenant API key"),
        (104, "weknora_knowledge_base_id", knowledge_base_id or "", "Default WeKnora knowledge base ID"),
        (105, "weknora_match_count", os.getenv("WEKNORA_MATCH_COUNT", "5"), "Search max result count"),
        (106, "weknora_vector_threshold", os.getenv("WEKNORA_VECTOR_THRESHOLD", "0.5"), "Vector similarity threshold"),
        (107, "weknora_auto_rag_enabled", os.getenv("WEKNORA_AUTO_RAG_ENABLED", "true"), "Enable automatic RAG"),
    ]

    rows = []
    for config_id, key, value, description in values:
        rows.append(
            f"({config_id}, {sql_quote(key)}, {sql_quote(value)}, 'weknora', "
            f"{sql_quote(description)}, NOW(), NOW())"
        )

    sql = """
INSERT INTO crm_system_config
  (config_id, config_key, config_value, config_type, description, create_time, update_time)
VALUES
  {rows}
ON CONFLICT (config_key) DO UPDATE SET
  config_value = EXCLUDED.config_value,
  config_type = EXCLUDED.config_type,
  description = EXCLUDED.description,
  update_time = NOW();
""".format(rows=",\n  ".join(rows))

    env = os.environ.copy()
    env["PGPASSWORD"] = db_password
    subprocess.run(
        ["psql", "-h", db_host, "-p", db_port, "-U", db_user, "-d", db_name, "-v", "ON_ERROR_STOP=1"],
        input=sql,
        text=True,
        check=True,
        env=env,
    )
    state = "enabled" if enabled else "disabled"
    log(f"CRM WeKnora config updated ({state})")


def main():
    if os.getenv("WEKNORA_INIT_ENABLED", "false").lower() not in ("1", "true", "yes", "on"):
        log("disabled by WEKNORA_INIT_ENABLED")
        return

    session = ensure_account()
    token = session["token"]
    tenant = session.get("tenant") or {}
    api_key = tenant.get("api_key", "")
    log(f"WeKnora tenant ready: {tenant.get('id')}")

    llm_id = ensure_model(token, "KnowledgeQA", "INIT_LLM_MODEL", "aliyun")
    embedding_id = ensure_model(token, "Embedding", "INIT_EMBEDDING_MODEL", "aliyun")

    kb_id = ensure_knowledge_base(token, llm_id, embedding_id)
    upsert_crm_config(api_key, kb_id, enabled=True)
    log(f"RAG is ready; knowledge base id: {kb_id}")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        log(f"failed: {exc}")
        if isinstance(exc, ApiError) and exc.payload:
            log(json.dumps(exc.payload, ensure_ascii=False))
        sys.exit(1)
