# Academic Text Summarization Assistant

> A controlled case study on how the **Adapter design pattern** affects the portability and maintainability of web applications integrated with Generative AI providers.

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/build-Maven-blue)](https://maven.apache.org/)
[![Status](https://img.shields.io/badge/status-research%20prototype-yellow)]()

---

## Overview

This repository hosts the software artifact developed for an undergraduate thesis (*Trabalho de Conclusão de Curso*) in Software Engineering. Rather than being a production product, it is a **deliberately small, controlled prototype** built to answer a specific architectural research question:

> **To what extent does a decoupled architecture, based on the Adapter design pattern, reduce the impact of changes related to Generative AI providers on a web application?**

To investigate this, the exact same application — an *Academic Text Summarization Assistant* — is implemented **twice**, in two architecturally distinct versions living on separate Git branches:

| Version | Branch | Description |
|---|---|---|
| **Coupled** | `versao-acoplada` | `ResumoService` calls the AI provider's HTTP API directly (via `RestTemplate`), building and parsing the provider-specific JSON itself. No abstraction layer exists between business logic and the provider. |
| **Decoupled** | `versao-desacoplada` | The application depends only on an internal abstraction (`Target` interface). A concrete `Adapter` translates that interface into the specific format required by whichever provider is plugged in. |

Both versions expose identical functionality to the end user. The only thing that differs is *how* they are built internally — which is precisely the point.

Three controlled **change scenarios** (provider replacement, addition of a second provider, and a breaking change in the provider's communication format) are then applied to both versions, and objective software metrics (files touched, lines of code changed, tests affected, direct coupling points) are collected and compared. If you're a recruiter skimming this: this project is less about "building a summarizer" and more about **producing empirical evidence for an architectural decision**, using the same rigor you'd expect from a systems design write-up.

---

## Why this matters

Applications that integrate Generative AI today are often wired directly to one provider's API. That's fine — until the provider changes its pricing, deprecates a model, alters its request/response schema, or simply becomes unavailable, and suddenly every consumer of that API across the codebase needs to be touched. This project treats that risk as a first-class engineering concern and measures, empirically, whether a well-known structural pattern actually mitigates it in this specific, fast-moving domain.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.1 (Spring Web MVC, `RestTemplate` for provider calls) |
| Build tool | Maven (via the bundled Maven Wrapper) |
| Testing | JUnit 5 |
| Frontend | A single static `index.html` with inline CSS and JavaScript (no framework — kept intentionally minimal so the study's focus stays on the backend architecture) |
| AI Providers | [Google Gemini API](https://ai.google.dev/) · [Groq API](https://console.groq.com/) (OpenAI-compatible) |
| Version control strategy | Git, with divergent long-lived branches representing each architectural variant |

---

## Project Structure

The Maven project lives in `prototype/`. Packages shared by every branch:

```
prototype/src/main/java/com/adapter/prototype/
 ├── controller/
 │     └── ResumoController.java       # REST entry point (POST /api/resumo)
 ├── service/
 │     └── ResumoService.java          # Business rules (text validation)
 ├── dto/
 │     ├── ResumoRequest.java          # Inbound payload
 │     ├── ResumoResponse.java         # Outbound payload
 │     └── ErroResponse.java           # Standardized error payload
 ├── exception/
 │     ├── TextoInvalidoException.java
 │     └── GlobalExceptionHandler.java # @RestControllerAdvice
 └── PrototypeApplication.java

prototype/src/main/resources/
 ├── static/
 │     └── index.html                  # UI (inline CSS + JS)
 └── application.properties
```

Each architectural branch adds the provider integration in a different place:

```
versao-acoplada (Coupled)
 ├── service/ResumoService.java        # also builds the Gemini request and parses the response
 ├── client/                           # GeminiRequest, GeminiResponse (JSON DTOs)
 ├── config/                           # GeminiConfig (RestTemplate), GeminiProperties
 └── exception/ProvedorIndisponivelException.java

versao-desacoplada (Decoupled)
 ├── service/ResumoService.java        # depends only on AiSummarizerClient
 └── provider/
       ├── AiSummarizerClient.java     # Target interface
       ├── ProvedorIndisponivelException.java
       └── gemini/                     # GeminiAdapter, GeminiConfig, GeminiProperties + package-private JSON DTOs
```

---

## Branching Strategy

This is not a conventional feature-branch workflow — the branches encode the experiment itself:

```
main  (baseline: functional UI, zero AI integration — the common starting point)
 │
 ├── versao-acoplada        (Version A — direct, tightly coupled Gemini integration)
 │     └── each change scenario applied on the baseline, tagged, then reverted
 │
 └── versao-desacoplada     (Version B — Adapter-based, decoupled Gemini integration)
       └── same change scenarios applied on the baseline, tagged, then reverted
```

Both branches rest at their baseline (Gemini only); the state of every scenario is preserved in a tag (see [Experiment tags](#experiment-tags)).

`versao-acoplada` and `versao-desacoplada` never merge into each other — they are two parallel, independently evolving implementations of the same functional requirements, which is what makes their `git diff` output a valid unit of comparison.

---

## Getting Started

### Prerequisites

- Java 17+
- No Maven install needed — the project ships with the Maven Wrapper (`mvnw` / `mvnw.cmd`)
- A free API key from [Google AI Studio](https://aistudio.google.com/) (Gemini) — required on both branches
- A free API key from [Groq Console](https://console.groq.com/keys) — only needed to run the scenario 1 and 2 tags

### Environment Variables

```bash
export GEMINI_API_KEY="your-gemini-key-here"
export GROQ_API_KEY="your-groq-key-here"
```

(On Windows PowerShell, use `setx GEMINI_API_KEY "your-key"` instead, then restart your terminal.)

### Running locally

```bash
git clone https://github.com/CmuriloDev/TCC-Prototype.git
cd TCC-Prototype

# Choose which architectural version to run:
git checkout versao-acoplada      # or: versao-desacoplada, or any tag (e.g. cenario-2-desacoplada)

cd prototype
./mvnw spring-boot:run            # Windows: mvnw.cmd spring-boot:run
```

Then open the app in your browser: **http://localhost:3000** on `versao-acoplada` (it sets `server.port=3000`) or **http://localhost:8080** on `versao-desacoplada`.

---

## API Reference

### `POST /api/resumo`

Summarizes a piece of academic text using the configured AI provider.

**Request body**

```json
{
  "texto": "Paste an academic text between 50 and 5000 characters here."
}
```

**Success response — `200 OK`**

```json
{
  "resumo": "A concise, AI-generated summary of the submitted text."
}
```

**Validation error — `400 Bad Request`**

```json
{
  "mensagem": "O texto deve ter no mínimo 50 caracteres."
}
```

**Provider failure — `502 Bad Gateway`**

```json
{
  "mensagem": "O serviço de IA está indisponível no momento. Tente novamente em instantes."
}
```

| Scenario | Status | Trigger |
|---|---|---|
| Empty text | `400` | `texto` is null or blank |
| Text too short | `400` | fewer than 50 characters |
| Text too long | `400` | more than 5000 characters |
| Provider timeout / network failure | `502` | AI provider unreachable |
| Provider rate limit | `502` | HTTP 429 from the provider — message: `Limite de requisições ao provedor de IA excedido.` |
| Provider error response | `502` | any other non-2xx response from the AI provider |
| Empty or unusable provider answer | `502` | no generated text in the response — message: `Não foi possível gerar o resumo. Tente novamente.` |

> **Scenario 2 only** (`cenario-2-*` tags): the request accepts an optional `"provedor"` field (`"gemini"` or `"groq"`, case-insensitive; Gemini when absent or blank). Any other value returns `400` with `Provedor inválido. Valores aceitos: gemini, groq.`

---

## Design Patterns & Architectural Decisions

The decoupled version (`versao-desacoplada`) implements the classic **GoF Adapter pattern**:

| GoF Role | Concrete element in this project |
|---|---|
| **Target** | The internal interface `AiSummarizerClient`, exposing the provider-agnostic method `gerarResumo(String texto)` |
| **Adaptee** | The specific AI provider's API (Gemini's `contents`/`parts` schema, or Groq's OpenAI-compatible `messages` schema) |
| **Adapter** | A concrete implementation (`GeminiAdapter`, `GroqAdapter`) that translates the `Target` call into the Adaptee's specific request/response format |
| **Client** | `ResumoService`, which depends exclusively on the `Target` interface and has zero knowledge of which provider is behind it (in scenario 2 it receives every `Adapter` as a `Map<String, AiSummarizerClient>` keyed by bean name and picks one per request) |

In the coupled version, `ResumoService` talks to the provider's API directly — there's no interface in between, by design, since this version exists specifically to serve as the "before" state in the comparison.

---

## Experimental Protocol (Change Scenarios)

To make the coupled-vs-decoupled comparison objective rather than anecdotal, three controlled change scenarios are applied identically to both architectural versions:

1. **Provider replacement** — fully migrating from one AI provider to another (e.g. Gemini → Groq).
2. **New provider addition** — introducing a second provider alongside the existing one, without removing it.
3. **Communication format change** — simulating a breaking change in the current provider's request/response schema.

For each scenario, on each version, the following metrics are collected via Git (`git diff --numstat` from the baseline to the scenario tag, both with `--no-renames` and with rename detection):

- Number of files modified, added or removed
- Number of lines added/removed
- Both of the above split into groups: **P** (provider-specific code — `provider/gemini|groq/` or `Gemini*`/`Groq*` classes), **T** (automated tests) and **O** (everything else, separated into code and configuration)
- Number of direct coupling points to the provider: case-insensitive occurrences of `gemini`/`groq` in `src/main` Java files outside group P, counted by script on both versions

Full methodology, measurement scripts, raw results, validity threats and analysis are documented in the [TCC repository](https://github.com/CmuriloDev/TCC-Document) (`METRICAS.md` and `prototipo/`), and in the published article once available.

### Experiment tags

Each scenario is applied on top of its version's baseline, tagged, and then reverted, so every scenario is measured independently with `git diff <baseline> <tag>`. Both branches are kept at their baseline; the scenario states live only in the tags below.

| Tag | Scenario | Version | Commit |
|---|---|---|---|
| `baseline-acoplada` | Baseline (Gemini only) | Coupled | `295d660` |
| `cenario-1-acoplada` | 1 — Provider replacement (Gemini → Groq) | Coupled | `bc0cf1c` |
| `cenario-2-acoplada` | 2 — New provider addition (Gemini + Groq) | Coupled | `cf372e9` |
| `cenario-3-acoplada` | 3 — Communication format change (simulated Gemini schema) | Coupled | `a648c39` |
| `baseline-desacoplada` | Baseline (Gemini only, behind the Adapter) | Decoupled | `497ed71` |
| `cenario-1-desacoplada` | 1 — Provider replacement (Gemini → Groq) | Decoupled | `86dc06d` |
| `cenario-2-desacoplada` | 2 — New provider addition (Gemini + Groq) | Decoupled | `d784cc8` |
| `cenario-3-desacoplada` | 3 — Communication format change (simulated Gemini schema) | Decoupled | `ea76236` |

---

## Project Status

- [x] Baseline application (UI + validation, no AI integration)
- [x] Coupled version — direct Gemini integration
- [x] Coupled version — error handling for provider failures
- [x] Coupled version — change scenarios applied & metrics collected
- [x] Decoupled version — Adapter-based implementation
- [x] Decoupled version — change scenarios applied & metrics collected
- [x] Comparative results consolidated

The experimental phase of the prototype is complete: both branches are back at their baselines and every scenario is preserved in its tag. Analysis and writing continue in the thesis repository.

---

## Academic Context

This project is the practical component of a Bachelor's thesis in Software Engineering, submitted as a scientific article (case-study methodology) at iCEV — Instituto de Ensino Superior (Teresina, PI, Brazil). It is not intended as a production-ready application, and functional scope was intentionally kept minimal so that the architectural comparison remains the focus of the work.

---

## License

This project is released for academic and portfolio purposes. Feel free to explore the code and adapt the approach — attribution is appreciated.

---

## Author

**Carlos Murilo**
Software Engineering student · Backend & Full-stack Developer
[GitHub](https://github.com/CmuriloDev) · [LinkedIn](https://www.linkedin.com/in/carlos-murilo-dev/)
