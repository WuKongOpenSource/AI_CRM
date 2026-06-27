# WukongMarket — kickoff prompt for the new conversation

**How to use:** create the new repo (e.g. `wukong-market/`), **copy `WUKONG_MARKET_CONTRACT.md` into it** (e.g. `wukong-market/docs/INTEGRATION_CONTRACT.md`), open a new Claude Code conversation **with the working directory set to that new repo**, and paste the prompt below.

---

## Paste this into the new conversation

> I'm building **WukongMarket** — the centralized, closed-source SaaS control plane (marketplace) for the plugin platform of an open-source AI CRM. Stack: **Java 21 / Spring Boot 3.x, PostgreSQL**, object storage for artifacts (S3/MinIO), separate repo from the CRM.
>
> The CRM instance side of the plugin platform already exists and is verified (OAuth client registry, Extension Gateway with run-as-actor data access, declarative-manifest apply/reverse, dynamic AI-app registry, event→signed-webhook delivery, lifecycle). WukongMarket must implement the **server side** of our integration contract so the CRM's Marketplace Client can consume it.
>
> **The authoritative spec is `docs/INTEGRATION_CONTRACT.md` in this repo — read it first and treat it as the source of truth.** Key points: WukongMarket owns catalog/versions/artifacts/**signing**/licensing/developer-portal and OAuth-client *provisioning*; the CRM instance owns OAuth token issuance + scope enforcement. Every plugin version is signed with our private key; instances pin the public key and verify before install. Manifest schema v1 must be byte-compatible with the contract. Versions are immutable.
>
> Please:
> 1. Read `docs/INTEGRATION_CONTRACT.md` and restate the contract surfaces + the authority split, flagging anything underspecified.
> 2. Propose the build plan as milestones (suggested in the contract §8): M0 scaffold → M1 registry + public catalog API → M2 signing + artifact distribution → M3 developer portal + manifest-v1 validation → M4 install bundle + OAuth client provisioning + licensing/heartbeat → M5 review/moderation + billing → M6 instance registration / API keys.
> 3. Recommend whether to start with a shared `wukong-plugin-contract` JAR (contract §9) holding the manifest model + DTOs + signature verifier, so the CRM and WukongMarket never drift.
> 4. Then scaffold M0 (Spring Boot service + Postgres + object storage + the contract module) and implement M1, verifying as you go.
>
> Constraints: implement exactly the v1 HTTP API + manifest schema + signing scheme in the contract (don't invent divergent shapes). Keep the signing private key in a secret store/KMS, never in the repo. Reject reserved AI-app codes (general/crm/product/project/knowledge/address_book/relation) at publish time.

---

## Notes on linking the two repos (recap)

- **They link via the versioned contract**, not a runtime code dependency. The bridge is `INTEGRATION_CONTRACT.md` (kept in both repos) and, ideally, the shared `wukong-plugin-contract` JAR (contract §9).
- **Memory does not carry across projects** (it's keyed to each repo's path), so the contract doc is the carrier of intent.
- When the contract changes: bump the version, update the doc in **both** repos (and the shared JAR), and re-verify both sides.
- The CRM still owes **P1-d** (Marketplace Client) to consume this — do that back in the CRM repo/conversation once WukongMarket exposes the v1 API (even a stub is enough to wire + test the instance side).
