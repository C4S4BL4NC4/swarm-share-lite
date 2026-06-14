Collaboration Guidelines

Thank you for contributing to swarm-share-lite.

The primary goal of this project is not only to build a distributed file-sharing system, but also to serve as a reference implementation of modern Java 25, Test-Driven Development (TDD), and clean software architecture.

---

Core Principles

1. Test First
2. Modern Java 25
3. Readable Code Over Clever Code
4. Document Important Decisions
5. Maintain Architectural Boundaries

If a contribution violates these principles, it will likely be rejected regardless of whether it works.

---

Test-Driven Development (Required)

All new features and bug fixes should follow TDD:

1. Write a failing test.
2. Implement the minimum code necessary to pass.
3. Refactor while keeping tests green.

Expected workflow:

RED → GREEN → REFACTOR

Contributors should submit tests alongside production code.

Examples:

- New protocol message → tests first
- New storage behavior → tests first
- Bug fix → reproduce bug with a failing test before fixing

Pull requests without adequate tests may be rejected.

---

Modern Java 25 Standards

This project intentionally embraces modern Java.

Prefer:

- Records
- Sealed interfaces/classes
- Virtual Threads
- Pattern Matching
- Switch Expressions
- Immutable data structures
- Structured concurrency when appropriate
- "Optional" where it improves clarity

Avoid:

- Legacy Java patterns when modern alternatives exist
- Unnecessary inheritance
- Mutable shared state
- Excessive framework usage
- Premature optimization

Code should feel like modern Java, not Java 8.

---

Documentation Requirements

Public APIs must be documented.

Example:

/**
 * Retrieves a chunk from local storage.
 *
 * @param chunkIndex index of the requested chunk
 * @return chunk data
 * @throws ChunkNotFoundException if the chunk is unavailable
 */
Chunk readChunk(int chunkIndex);

Document:

- Public classes
- Public methods
- Interfaces
- Complex algorithms
- Protocol behaviors
- Non-obvious design decisions

Do not document the obvious.

Bad:

// Increment counter
counter++;

Good:

// CAS loop prevents duplicate scheduling when multiple
// virtual threads compete for the same chunk.

---

Commenting Guidelines

Comments should explain:

- Why something exists
- Design trade-offs
- Concurrency assumptions
- Protocol guarantees
- Performance considerations

Comments should not explain what the code already clearly states.

Prefer self-documenting code.

---

Architectural Rules

Domain code must remain independent of infrastructure.

Allowed dependency direction:

CLI
 ↓
Transfer
 ↓
Networking / Storage
 ↓
Operating System

Core domain models must not depend on:

- TCP
- FileChannel
- JSON libraries
- CLI frameworks
- External infrastructure

Infrastructure depends on the domain.

Never the reverse.

---

Pull Request Expectations

Before opening a PR:

- All tests pass
- New functionality includes tests
- Public APIs are documented
- Code follows Java 25 conventions
- No unrelated changes included
- Commit history is reasonably clean

Small focused PRs are preferred over large PRs.

---

Code Style

Prefer:

- Small methods
- Clear names
- Immutable objects
- Constructor injection
- Explicit domain terminology

Avoid:

- God classes
- Utility dumping grounds
- Deep inheritance hierarchies
- Excessive abstraction
- Over-engineering

---

Project Goal

The objective is to create a codebase that is:

- Educational
- Maintainable
- Extensible
- Idiomatic Modern Java

Future contributors should be able to learn distributed systems and modern Java simply by reading the code.

When in doubt:

Choose readability, tests, and simplicity.