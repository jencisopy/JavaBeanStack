# AGENTS.md

This repository is a Maven multi-module Java framework.
This file gives coding agents the minimum repo-specific guidance needed to work safely here.

## Scope

- Applies to the whole repository rooted at `JavaBeanStack`.
- No `.cursor/rules/`, `.cursorrules`, or `.github/copilot-instructions.md` files were found during this scan.
- No prior `AGENTS.md` existed at the repository root when this file was created.

## Repository Layout

- Root parent module: `pom.xml` (`org.javabeanstack:jbs-parent`).
- Java release configured in Maven: `11` via `maven.compiler.release`.
- Modules declared in the root reactor:
- `interfaces`
- `core`
- `business`
- `commons`
- `web`
- `aws`
- Packaging is primarily `jar` modules.

## Tooling Summary

- Build tool: Maven.
- Test framework: JUnit 5 (`junit-jupiter`).
- Logging: Log4j 2.
- Persistence/API stack used in code: Java EE / Jakarta-adjacent APIs such as EJB, JPA, JAX-RS, servlet APIs.
- Coverage profile exists: `sonar-coverage` with JaCoCo.
- There is no dedicated lint plugin configured in the scanned POMs.
- There is no formatter plugin configured in the scanned POMs.
- There is no Checkstyle, Spotless, PMD, or Error Prone configuration in the scanned POMs.

## Commands

Run commands from the repository root unless noted otherwise.

### Build

- Full reactor compile/package: `mvn clean package`
- Full reactor install to local Maven repo: `mvn clean install`
- Build without tests: `mvn -DskipTests clean package`
- Build one module and upstream dependencies: `mvn -pl commons -am clean package`
- Build one module by directory selector: `mvn -pl web -am clean package`

### Test

- Run all tests: `mvn test`
- Run all tests in one module: `mvn -pl commons test`
- Run one test class: `mvn -pl commons -Dtest=StringsTest test`
- Run one test method: `mvn -pl web -Dtest=ExcelUtilTest#testOpenWorkbook_File test`
- Run multiple specific tests: `mvn -pl commons -Dtest=StringsTest,FnTest test`
- Run a module and required dependencies: `mvn -pl business -am test`
- Run coverage profile: `mvn -Psonar-coverage test`
- If Maven complains about no matching tests, add `-DfailIfNoTests=false`.

### Verified examples

- `mvn -pl commons -Dtest=StringsTest test` completed successfully during this scan.
- `mvn -pl web -Dtest=ExcelUtilTest#testOpenWorkbook_File test` completed successfully during this scan.

### Lint / Format

- No standalone lint command is configured in Maven right now.
- No standalone format command is configured in Maven right now.
- For validation, prefer `mvn test` or `mvn clean package` over inventing lint steps.

## Working Conventions

- Prefer targeted module commands over full-reactor runs when editing a single module.
- Use `-pl <module> -am` when a module depends on upstream modules in the same reactor.
- Keep changes scoped; this repo has multiple modules with distinct responsibilities.
- Do not "upgrade" Java, Maven plugins, or third-party dependencies unless the task explicitly requires it.
- The root README still mentions JDK 8, but the active root POM compiles with Java 11; trust the POM.

## Code Style

### General formatting

- Follow the surrounding file exactly; this codebase has a strong legacy style.
- Use 4 spaces for indentation.
- Keep opening braces on the same line as class and method declarations.
- Use blank lines to separate logical sections, fields, and major method steps.
- Preserve existing block comment headers in files that already have them.
- When creating a new Java file, copy the license/header style from nearby files in the same module.

### Imports

- Put the `package` declaration first, then imports, then the class declaration.
- Prefer non-static imports for normal types.
- Static imports are common for assertions and a few utility helpers.
- In new code, prefer explicit static imports over wildcard static imports unless the surrounding file already uses a wildcard.
- Keep imports grouped in a stable order that matches the surrounding file rather than reformatting the entire block.
- Do not reorder imports in untouched files just for style.

### Naming

- Packages are lowercase under `org.javabeanstack.*`.
- Classes use PascalCase.
- Interfaces consistently use the `I` prefix, for example `IDataRow`, `ILogManager`, `IWebResource`.
- Methods and fields use camelCase.
- Constants use uppercase snake case, for example `DEFAULT_SCHEMA_PROPERTY`.
- Loggers are usually named `LOGGER` and declared `private static final`.
- Test classes use the production class name plus `Test`.

### Types and APIs

- Prefer explicit Java types; do not introduce `var`.
- Use interface types in signatures where the codebase already does so (`List`, `Map`, repo-specific interfaces).
- Use concrete implementations only at construction sites, for example `new ArrayList<>()`, `new HashMap<>()`.
- Match the existing public API style even when it is older Java style.
- Wrapper types such as `Boolean` appear in public APIs; only replace with primitives when null is impossible and the surrounding API uses primitives.
- Generics are used broadly; keep type parameters explicit when it improves compatibility with existing APIs.

### Class structure

- Common order is: constants/loggers, injected fields, constructors, public API, protected helpers, private helpers.
- EJB/JPA/JAX-RS annotations are used heavily; preserve annotation placement and ordering from nearby code.
- Abstract base classes are common in `business` and `web`; prefer extending existing framework abstractions instead of bypassing them.

### Error handling

- Follow the layer's existing behavior instead of forcing a new exception policy.
- Service/DAO code often declares `throws Exception` on public methods; do not narrow signatures casually.
- When catching exceptions in framework code, log them consistently.
- Existing patterns include `ErrorManager.showError(ex, LOGGER, logMngr, null)` and `LOGGER.error(ex.getMessage())`.
- Avoid swallowing exceptions silently.
- Return `null`, `false`, or an empty collection only when that matches the existing method contract in that class.
- Use domain-specific exceptions where the module already does so, for example `TokenError`, `CompanyError`, `SessionError`.
- Validate null or empty inputs early when the surrounding code does that.

### Logging

- Use Log4j 2: `private static final Logger LOGGER = LogManager.getLogger(MyClass.class);`
- Prefer concise, contextual messages.
- Reuse existing logging helpers where available instead of inventing parallel logging flows.
- Avoid noisy info/debug logging unless the class already logs at that level.

### Testing

- Tests use JUnit 5 annotations from `org.junit.jupiter.api`.
- Assertions are typically imported statically from `org.junit.jupiter.api.Assertions`.
- Some legacy tests print to stdout; do not add more console output unless it helps debug an unstable test.
- Keep new tests near the module they exercise under `src/test/java`.
- For focused verification, run a single test class or method before broader reactor runs.

### Data and framework patterns

- `commons` contains low-level utility helpers.
- `core` contains configuration, resource, logging, XML, and framework support code.
- `business` contains data access, services, security, reporting, and business abstractions.
- `web` contains JSF, REST, filters, converters, and web-specific helpers.
- `interfaces` contains shared contracts used across modules.
- `aws` contains AWS-specific integrations.
- Keep dependencies flowing in the same direction as the current module graph; do not create circular module coupling.

## Agent Advice

- Before changing code, inspect the target module's nearby files and mimic local conventions.
- Favor minimal, compatibility-oriented edits over large refactors.
- If you add a new command to this file later, verify it against the current Maven reactor first.
- If a task mentions linting, explain that the repo currently has no dedicated lint target and use Maven build/test validation instead.
