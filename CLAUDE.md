# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JavaBeanStack is a Maven multi-module Java EE framework (LGPL v3.0) providing reusable infrastructure for data access, security, error handling, and JSF/REST web tiers. It is a **library/framework**, not a standalone application.

## Build Commands

Run from the repository root unless noted otherwise.

```bash
# Full build
mvn clean package

# Install to local Maven repo
mvn clean install

# Skip tests
mvn -DskipTests clean package

# Build one module and its upstream dependencies
mvn -pl commons -am clean package
```

## Test Commands

```bash
# All tests
mvn test

# All tests in one module
mvn -pl commons test

# Single test class
mvn -pl commons -Dtest=StringsTest test

# Single test method
mvn -pl web -Dtest=ExcelUtilTest#testOpenWorkbook_File test

# Multiple test classes
mvn -pl commons -Dtest=StringsTest,FnTest test

# With coverage (JaCoCo)
mvn -Psonar-coverage test
```

If Maven reports no matching tests, add `-DfailIfNoTests=false`.

There is no dedicated lint or format command — use `mvn test` or `mvn clean package` for validation.

## Module Architecture

Dependency flow (each level depends on those above it):

```
interfaces  (contracts only, no implementations)
    ↓
commons     (string, file, crypto, date utilities)
    ↓
core        (error handling, logging, XML, config, resources)
    ↓
business    (JPA/EJB data access, services, security, business logic)
    ↓
web         (JSF controllers, REST resources, Excel/JasperReports, filters)

aws         (AWS S3 integration — standalone, not part of main chain)
```

Do not introduce imports that reverse this dependency direction.

## Key Abstractions

### Data Layer (`business` module)
- **`DataRow`** — base entity class; all application entities extend this. Tracks CRUD action state (`INSERT=1`, `UPDATE=2`, `DELETE=3`), field changes, and validation errors.
- **`AbstractDAO`** — generic JPA-backed DAO with query building, entity validation, and error handling.
- **`AbstractDataLink`** — wraps DAO and manages persistence unit context, user session, and company/schema context. Implements `IDataLink`.
- **`AbstractDataService`** — extends DAO with business validation (unique keys, foreign key checks, field-level validation).

### Security (`interfaces` + `business` modules)
- **`IUserSession`** — authenticated user context (login, company, roles, permissions).
- **`ISecManager`** — EJB-based authentication, password management, OAuth.
- **`JwtManager`** / **`DigestAuth`** — JWT and HTTP Digest authentication support.

### Error Handling (`core` module)
- **`IErrorReg`** / **`ErrorReg`** — unified error representation across layers.
- **`ErrorManager`** — static utility for error logging: `ErrorManager.showError(ex, LOGGER, logMngr, null)`.

### Web Tier (`web` module)
- **`AbstractDataController`** — base JSF managed bean for CRUD screens; manages lazy loading, caching, and error display.
- **`UserEnvironment`** — request-scoped user context and permission checks.
- **`LazyDataRows`** — efficient pagination for large result sets in JSF DataTables.
- REST exceptions: `TokenError`, `JpaNoExist`, `OptionUnavailable`.

## Code Style

- **Indentation:** 4 spaces.
- **Interfaces:** `I` prefix convention (e.g., `IDataRow`, `ILogManager`).
- **Classes:** PascalCase; **methods/fields:** camelCase; **constants:** `UPPER_SNAKE_CASE`.
- **Loggers:** `private static final Logger LOGGER = LogManager.getLogger(MyClass.class);`
- Do not use `var`; use explicit types throughout.
- Prefer interface types in signatures (`List`, `Map`, `IDataRow`); use concrete types only at construction (`new ArrayList<>()`).
- Class member order: constants/loggers → injected fields → constructors → public API → protected helpers → private helpers.
- EJB/JPA/JAX-RS annotations are pervasive — preserve annotation placement from surrounding code.
- Public methods in service/DAO code commonly declare `throws Exception`; do not narrow these signatures.

## Working Conventions

- Prefer targeted module commands (`-pl <module> -am`) over full-reactor runs.
- The root README mentions JDK 8, but the active POM compiles with Java 11 — trust the POM.
- Extend existing abstract base classes rather than bypassing them.
- Before changing code, read nearby files in the same module and match local conventions exactly.
- Favor minimal, compatibility-oriented edits over refactors.
