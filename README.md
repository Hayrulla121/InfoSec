# Качественная оценка рисков ИТ и ИБ

Web platform replacing the Excel workbook used for qualitative IT & IS risk
assessment (ISO/IEC 27005 style): asset registry, DREAD threat scoring, risk
register with mitigation controls, 5×5 risk matrix, and a management dashboard.

**Status: all seven phases complete. 138 backend tests green.**

## Interface language

A **RU / O‘Z** switch sits in the sidebar (and on the login card). The choice is
saved in `localStorage`, applied instantly without a reload, and sent to the
backend as `Accept-Language` so validation and business messages come back in
the same language.

What the switch does and does not touch:

| | Language |
|---|---|
| Interface chrome — menu, buttons, table headers, forms, dialogs, errors | Follows the switch |
| Risk / threat / criticality levels, treatment methods, statuses | Follows the switch (mapped for display) |
| **Data people typed** — asset names, threat descriptions, control names, comments, dictionary values | Shown exactly as entered |

That last row is deliberate. The database stores those in Russian because the
Excel export has to match the source workbook whatever language the screen is
in; rewriting them for display would make the exported file and the UI disagree.

**Adding a language.** Copy `frontend/src/i18n/uz.ts`, translate the values, and
register it in `I18nContext`. `Dictionary` is derived from `ru.ts`, so a missing
or misspelled key is a **compile error** — an untranslated screen cannot ship.
On the backend, add `messages_<lang>.properties` beside the existing two.

## Demo data

**Загрузить демо-данные** in the sidebar (admin only) seeds 3 information
systems, 4 assets, 6 threats, 8 controls and 6 linked risks taken from the
source workbook — enough to fill the dashboard, matrix and every registry.

It goes through the normal services rather than inserting rows, so codes are
generated, DREAD scores are summed, and every risk is classified by the real
engine. It **refuses with a 409 when any registry already holds data**: demo
records mixed into a live register cannot be told apart from real ones
afterwards.

## Excel export

The **Экспорт в Excel** button in the sidebar downloads a real `.xlsx` that
reproduces the source workbook: the same eight sheets in the same order, the
same 75-column layout on Реестр рисков, and **live formulas** — VLOOKUP,
SUM, the DREAD helper columns, the reduction chains and the a×t classification
all recalculate when Excel opens the file.

The layout comes across too, not just the values — merged headings, the
columns the source keeps hidden (`Класс защищенности`, `Индикаторы риска`,
`Описание контроля`), the frozen header rows, the column filters, and the
colour coding on the risk and threat ladders, in the source's own shades.

Two deliberate differences from the source:

| Source workbook | Export |
|---|---|
| Ranges hard-coded — lookups (`$A$2:$R$196`), filters (`A1:F30` on a 542-row sheet), highlight ranges (`I2:I482` on 931 rows) | All sized to the actual row count, so they cannot be outgrown or silently under-cover the data |
| Control IDs derived with a dynamic-array `FILTER` | Written as values — the database already holds the authoritative links, and a literal cannot produce `#SPILL!` |

The workbook is a **report, not a second system of record.** It recalculates
independently, so a DREAD score edited in Excel will disagree with the platform.
The database stays authoritative.

Excel's layout caps a risk at 7 implemented + 5 planned controls; the platform
does not. If a risk exceeds that, the export returns an `X-Export-Warnings`
header and the UI shows which risks were affected rather than truncating
silently.

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5.16, Spring Web / Data JPA / Security / AOP, Flyway, Bean Validation, JJWT |
| Database | H2 file mode (dev, PostgreSQL compatibility) → PostgreSQL (prod) |
| Frontend | React 19 + TypeScript, Vite, React Router, Axios |

## Prerequisites

- **JDK 21.** The system default here is JDK 17, so builds must point at 21:
  ```bash
  export JAVA_HOME=/opt/homebrew/opt/openjdk@21
  ```
  In IntelliJ: *File → Project Structure → Project SDK → 21*.
- **Node 20+.**
- Maven is **not** required — the repo ships the Maven Wrapper (`./mvnw`).

## Running

Two terminals.

**Backend** (http://localhost:8080):
```bash
cd backend && JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./mvnw spring-boot:run
```

**Frontend** (http://localhost:5173):
```bash
cd frontend && npm run dev
```

**Tests:**
```bash
cd backend && JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./mvnw test
```

Dev login: **`admin`** / **`p@MZ7!q7vfYMGH478^B#`**. H2 SQL console at
http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/riskdb`, user
`sa`, blank password).

> The seeded password is set by `V6__admin_password.sql`. It is a *development*
> default: it ships in this repository, so anyone who can read the code can read
> the password. Change it from **Пользователи / Foydalanuvchilar** in the
> interface before the system holds real data.

## Screens

| Route | Excel sheet it replaces |
|---|---|
| `/` | — (new: dashboard with gauges) |
| `/threat-model` | Ma'lumot — Tahdidlar modeli |
| `/assets` | Реестр ключевых ИА |
| `/threats` | Реестр угроз |
| `/risk-matrix` | Матрица рисков |
| `/risks` | Реестр рисков |
| `/controls` | Риск-контроль |
| `/dictionaries` | Техническая страница |
| `/admin/users` | — (new: users & permissions) |

## The calculation engine

All Excel formulas live in **one class**, `RiskCalculationService`, with 57
golden unit tests citing the source columns.

| Rule | Excel source | Implementation |
|---|---|---|
| DREAD clamp 0–5 | cols L–P | `clampCriterion` |
| Sum 0–25 | col Q | `totalScore` |
| Rating thresholds `<6→1 <11→2 <16→3 <21→4 else 5` | col R | `ratingFromScore` |
| Asset rating lookup | col H VLOOKUP | `DictionaryService.numericValueOf` |
| Control chain `s = s − s×pct` | cols AQ–AW, BC–BG | `applyReductions` |
| a×t classification | cols AI / BU / BW | `classify` |
| Matrix counts | COUNTIFS(AF, BV) | `RiskRepository.matrixCounts` |

Each risk stores **three** computed stages:

- **inherent** — raw threat rating, no controls *(new; Excel had no such column)*
- **current** — after IMPLEMENTED controls → Excel's «Уровень риска»
- **residual** — after IMPLEMENTED + PLANNED → Excel's «Остаточный риск»

`RiskRecalculationService` refreshes these whenever a threat's scores, an
asset's criticality, a control's percentage, or a risk-control link changes —
always inside the same transaction, so no reader ever sees stale numbers.

## Security model

| | ADMIN | USER |
|---|---|---|
| Log in | ✔ | ✔ |
| Manage users / permissions | ✔ | ✘ (403) |
| CRUD on modules | ✔ always | only where granted |

- Every endpoint except `POST /api/auth/login` requires a valid JWT.
- Write endpoints carry `@RequireModulePermission(module, action)`, enforced by
  an aspect before the method body runs. ADMIN bypasses the grid.
- New users default to READ on all seven modules, no write access anywhere.
- The frontend hides forbidden buttons, but that is **UX only** — the backend is
  the real guard.
- Deactivating a user immediately invalidates their existing tokens.

**Production checklist:** override the signing key via environment variable —
`export APP_JWT_SECRET=$(openssl rand -base64 48)`. The value in
`application.yml` is a dev default only.

Change the `admin` password too. The seeded one is long and random rather than
the word `admin`, which stops opportunistic guessing, but it is committed to
this repository and therefore is not a secret. Set a real one through the
Пользователи screen, which re-hashes with the same BCrypt encoder. If you would
rather rotate it in the database, generate a hash with
`htpasswd -bnBC 10 "" 'new-password'` and update `users.password_hash` — never
by editing a migration that has already run.

## Improvements over the workbook

| Excel limitation | Platform |
|---|---|
| Max 7 implemented + 5 planned controls (fixed columns R–AD) | Unlimited, via `risk_controls` link table |
| Fixed VLOOKUP ranges (300 assets / 196 threats / 454 controls) | No limits |
| Hidden helper columns L–R, AF–BW | One tested service class |
| Single shared file, no concurrency control | Multi-user, JWT auth, audit columns |
| No access control | Role + per-module CRUD permissions |
| No inherent-risk baseline | Inherent / current / residual all tracked |
| TEXTJOIN control-name columns | Control chips + rebuilt in CSV export |

## Layout

```
backend/
  src/main/java/uz/infosec/risk/
    config/      security, CORS, JWT properties, JPA auditing, i18n validation
    domain/      entities + enums (AppModule, Action, Role, RiskLevel, ...)
    repository/  Spring Data JPA interfaces
    security/    JWT filter, permission aspect, UserDetails adapter
    service/     business rules, calculation engine, CSV export
    web/         REST controllers + DTOs
    error/       ApiError contract + @RestControllerAdvice
  src/main/resources/db/migration/   V1..V5 — the schema's single source of truth
frontend/
  src/api/         axios instance + typed endpoint clients
  src/auth/        AuthContext, ProtectedRoute
  src/layout/      AppLayout (left nav + outlet)
  src/components/  DataTable, Modal, Gauge, ExportButton
  src/pages/       one per module
```

## Rules of the codebase

- **Flyway owns the schema.** `ddl-auto=validate` — Hibernate never creates or
  alters a table. Schema changes go in a *new* `V*.sql`; never edit a migration
  that has already run.
- **Entities never leave the service layer.** Controllers speak DTOs, so a
  password hash can never be serialised and a schema change cannot silently
  alter the API contract.
- **Computed fields are server-owned.** Request DTOs have no `rating` or
  `totalScore` field, so a client cannot dictate them.
- **All risk arithmetic lives in `RiskCalculationService`**, unit-tested against
  values taken from the original workbook.

## Progress

- [x] **Phase 0** — skeletons, H2 + Flyway, CORS
- [x] **Phase 1** — JWT auth, users CRUD, per-module permission grid
- [x] **Phase 2** — dictionaries + DREAD reference page
- [x] **Phase 3** — Assets, Threats, Controls registries
- [x] **Phase 4** — Risks engine, control linking, recalculation triggers
- [x] **Phase 5** — risk matrix heat map + dashboard gauges
- [x] **Phase 6** — CSV export, search, Russian validation messages
- [ ] Phase 7 (later) — swap H2 → PostgreSQL, Docker compose, backups
# InfoSec
