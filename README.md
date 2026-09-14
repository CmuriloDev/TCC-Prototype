# Academic Text Summarization Assistant

> A controlled case study on how the **Adapter design pattern** affects the portability and maintainability of web applications integrated with Generative AI providers.

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/build-Maven-blue)](https://maven.apache.org/)
[![Status](https://img.shields.io/badge/status-research%20prototype-yellow)]()

---

## Overview

This repository hosts the software artifact developed for an undergraduate thesis (*Trabalho de Conclusão de Curso*) in Software Engineering. Rather than being a production product, it is a **deliberately small, controlled prototype** built to answer a specific architectural research question:

> **To what extent does a decoupled architecture, based on the Adapter design pattern, reduce the impact of changes related to Generative AI providers on a web application?**

To investigate this, the exact same application — an *Academic Text Summarization Assistant* — is implemented **twice**, in two architecturally distinct versions living on separate Git branches:

| Version | Branch | Description |
|---|---|---|
| **Coupled** | `versao-acoplada` | The application talks directly to a specific AI provider's SDK/HTTP API. No abstraction layer exists between business logic and the provider. |
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
| Framework | Spring Boot (Spring Web) |
| Build tool | Maven |
| Frontend | Plain HTML, CSS, and JavaScript (no framework — kept intentionally minimal so the study's focus stays on the backend architecture) |
| AI Providers | [Google Gemini API](https://ai.google.dev/) · [Groq API](https://console.groq.com/) (OpenAI-compatible) |
| Version control strategy | Git, with divergent long-lived branches representing each architectural variant |

---

## Project Structure

```
src/main/java/com/seuprojeto/resumoacademico/
 ├── controller/
 │     └── ResumoController.java       # REST entry point (POST /api/resumo)
 ├── service/
 │     └── ResumoService.java          # Business rules + AI provider integration
 ├── dto/
 │     ├── ResumoRequest.java          # Inbound payload
 │     ├── ResumoResponse.java         # Outbound payload
 │     └── ErroResponse.java           # Standardized error payload
 ├── exception/
 │     ├── TextoInvalidoException.java
 │     ├── ProvedorIndisponivelException.java
 │     └── GlobalExceptionHandler.java # @RestControllerAdvice
 ├── config/
 │     └── GeminiConfig.java / GroqConfig.java   # RestTemplate + provider properties
 └── ResumoAcademicoApplication.java

src/main/resources/
 ├── static/
 │     ├── index.html
 │     ├── style.css
 │     └── script.js
 └── application.properties
```

> **Note:** the package layout above reflects the `main`/coupled baseline. The `versao-desacoplada` branch introduces an additional `provider/` (or `adapter/`) package containing the `Target` interface, the concrete `Adapter` implementations, and the `Adaptee`-facing clients — see that branch for the decoupled structure.

---

## Branching Strategy

This is not a conventional feature-branch workflow — the branches encode the experiment itself:

```
main  (baseline: functional UI, zero AI integration — the common starting point)
 │
 ├── versao-acoplada        (Version A — direct, tightly coupled AI integration)
 │     └── change scenarios applied and reverted one at a time, metrics collected
 │
 └── versao-desacoplada     (Version B — Adapter-based, decoupled AI integration)
       └── same change scenarios applied and reverted one at a time, metrics collected
```

`versao-acoplada` and `versao-desacoplada` never merge into each other — they are two parallel, independently evolving implementations of the same functional requirements, which is what makes their `git diff` output a valid unit of comparison.

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- A free API key from [Google AI Studio](https://aistudio.google.com/) (Gemini)
- A free API key from [Groq Console](https://console.groq.com/keys)

### Environment Variables

```bash
export GEMINI_API_KEY="your-gemini-key-here"
export GROQ_API_KEY="your-groq-key-here"
```

(On Windows PowerShell, use `setx GEMINI_API_KEY "your-key"` instead, then restart your terminal.)

### Running locally

```bash
git clone https://github.com/your-username/your-repo-name.git
cd your-repo-name

# Choose which architectural version to run:
git checkout versao-acoplada      # or: git checkout versao-desacoplada

mvn spring-boot:run
```

Then open **http://localhost:8080** in your browser.

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
| Provider rate limit / error response | `502` | non-2xx response from the AI provider |

---

## Design Patterns & Architectural Decisions

The decoupled version (`versao-desacoplada`) implements the classic **GoF Adapter pattern**:

| GoF Role | Concrete element in this project |
|---|---|
| **Target** | An internal interface (e.g. `AiSummarizerClient`) exposing a provider-agnostic method such as `gerarResumo(String texto)` |
| **Adaptee** | The specific AI provider's API (Gemini's `contents`/`parts` schema, or Groq's OpenAI-compatible `messages` schema) |
| **Adapter** | A concrete implementation (`GeminiAdapter`, `GroqAdapter`) that translates the `Target` call into the Adaptee's specific request/response format |
| **Client** | `ResumoService`, which depends exclusively on the `Target` interface and has zero knowledge of which provider is behind it |

In the coupled version, `ResumoService` talks to the provider's API directly — there's no interface in between, by design, since this version exists specifically to serve as the "before" state in the comparison.

---

## Experimental Protocol (Change Scenarios)

To make the coupled-vs-decoupled comparison objective rather than anecdotal, three controlled change scenarios are applied identically to both architectural versions:

1. **Provider replacement** — fully migrating from one AI provider to another (e.g. Gemini → Groq).
2. **New provider addition** — introducing a second provider alongside the existing one, without removing it.
3. **Communication format change** — simulating a breaking change in the current provider's request/response schema.

For each scenario, on each version, the following metrics are collected via Git:

- Number of files modified (`git diff --stat`)
- Number of lines added/removed
- Number of direct coupling points to the provider (manual count, coupled version only)
- Number of automated tests affected

Full methodology, validity threats, and results are documented in the accompanying thesis (see `/docs` in the [main TCC repository], or the published article once available).

---

## Project Status

- [x] Baseline application (UI + validation, no AI integration)
- [x] Coupled version — direct Gemini integration
- [x] Coupled version — error handling for provider failures
- [ ] Coupled version — change scenarios applied & metrics collected
- [ ] Decoupled version — Adapter-based implementation
- [ ] Decoupled version — change scenarios applied & metrics collected
- [ ] Comparative results consolidated

This is a research artifact under active development as part of an ongoing thesis; the checklist above is kept up to date as each experimental stage is completed.

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
[GitHub](#) · [LinkedIn](#)
