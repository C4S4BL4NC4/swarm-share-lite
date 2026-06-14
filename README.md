# swarm-share-lite

[![Java CI with Gradle](https://github.com/deadboyccc/swarm-share-lite/actions/workflows/gradle.yml/badge.svg)](https://github.com/deadboyccc/swarm-share-lite/actions/workflows/gradle.yml)
[![Dependabot Updates](https://github.com/deadboyccc/swarm-share-lite/actions/workflows/dependabot/dependabot-updates/badge.svg)](https://github.com/deadboyccc/swarm-share-lite/actions/workflows/dependabot/dependabot-updates)
[![Automatic Dependency Submission](https://github.com/deadboyccc/swarm-share-lite/actions/workflows/dependency-graph/auto-submission/badge.svg)](https://github.com/deadboyccc/swarm-share-lite/actions/workflows/dependency-graph/auto-submission)

> P2P chunk-based file distribution with logarithmic peer scaling.

Traditional file distribution bottlenecks at the source. `swarm-share-lite` turns every node that receives a chunk into a server — throughput grows as the swarm expands.

---

## The Problem

Distributing a 5 GB Linux ISO to 14 machines on a LAN with a single source means the seeder transfers ~70 GB total. The more peers you add, the worse it gets.

**Traditional approach** — source does all the work:
```
Seeder → Peer 1
Seeder → Peer 2
...
Seeder → Peer 14
```

**Swarm approach** — every completed peer becomes a source:
```
Round 1:  2 peers
Round 2:  4 peers
Round 3:  8 peers
Round 4: 16 peers
```

Each peer that finishes a chunk immediately begins serving it. Seeder load stays constant; distribution time scales logarithmically.

---

## Features

- **Chunk-based distribution** — files split into fixed-size chunks with SHA-256 verification per chunk
- **Parallel downloads** — missing chunks fetched concurrently from any peer that owns them
- **Resume support** — interrupted transfers restart from the last verified chunk; no separate metadata database needed
- **Peer promotion** — a chunk becomes serveable the moment it is verified and written
- **Virtual-thread concurrency** — built on Project Loom (Java 25); massive concurrency with natural blocking I/O and minimal overhead
- **Random-access writes** — chunks written directly to their file offset via `FileChannel`
- **Clean layered architecture** — domain layer has zero infrastructure dependencies

---

## Quick Start

**Requirements:** Java 25+, Gradle 8+, Linux (Ubuntu / Fedora)

### Seed a file

```bash
./gradlew cli:run --args="seed --file ubuntu-25.04.iso --port 7070"
```

```
Listening on port 7070
Manifest hash: e3b0c44298fc1c...
Total chunks: 5120
```

### Download from peers

```bash
./gradlew cli:run --args="leech \
  --manifest manifest.json \
  --peer 192.168.1.10:7070 \
  --peer 192.168.1.11:7070 \
  --output ubuntu-25.04.iso"
```

```
Downloaded 2048 / 5120 chunks... [████░░░░░░] 40%
Downloaded 5120 / 5120 chunks... [██████████] 100%
Transfer complete. Verifying full file hash...
✓ File verified.
```

Interrupted downloads resume automatically on restart.

---

## How It Works

### 1 — Manifest generation

The seeder splits the file into fixed-size chunks and produces a manifest:

```json
{
  "fileHash": "e3b0c44298fc1c...",
  "fileName": "ubuntu-25.04.iso",
  "totalSize": 5368709120,
  "chunkSize": 1048576
}
```

The manifest describes file metadata, chunk boundaries, and SHA-256 checksums for every chunk.

### 2 — Peer discovery

Peers exchange chunk availability using a compact `BitSet`. A set bit at position `i` means the peer owns chunk `i`.

### 3 — Parallel download

Missing chunks are scheduled concurrently. Each chunk is requested → downloaded → hash-verified → written to its exact file offset.

### 4 — Resume support

On restart, existing chunks are rehashed. Valid chunks are marked complete and skipped; only missing chunks continue downloading.

### 5 — Peer promotion

The instant a chunk is verified and written, the local peer can begin serving it to others — no coordination required.

---

## Architecture

```
CLI
 │
 ▼
Transfer Manager
 ├── StorageProvider
 └── PeerConnector
      │
      ▼
   TCP Network
```

| Module | Responsibility |
|---|---|
| `core` | Domain model and business rules |
| `manifest` | Manifest generation and serialization |
| `storage` | File storage and checksum validation |
| `networking` | TCP communication and protocol framing |
| `transfer` | Download orchestration and scheduling |
| `cli` | Command-line interface |

**Design principle:** the domain layer never depends on infrastructure. Swapping TCP → TLS or local disk → S3 requires no changes to orchestration logic.

---

## Protocol

| Code | Description |
|---|---|
| `0x01` | Request piece map |
| `0x02` | Piece map response |
| `0x03` | Request chunk |
| `0x04` | Chunk response |

Binary framing, big-endian integers, length-prefixed fields.

### Chunk lifecycle

```
MISSING → SCHEDULED → IN_FLIGHT → VERIFYING → VERIFIED → WRITTEN
                                                  │
                                         (hash mismatch / timeout)
                                                  │
                                               MISSING
```

---

## Testing

```bash
./gradlew test
```

**Unit tests** cover manifest generation, checksum verification, chunk state tracking, and transfer orchestration.

**Integration tests** run end-to-end transfer scenarios verifying chunk exchange, file reconstruction, and hash correctness.

---

## Why Virtual Threads?

Traditional thread-per-connection designs become expensive at scale — each OS thread carries significant memory overhead and context-switch cost.

Virtual threads (Project Loom, stable in Java 21+) allow:

- Massive concurrency without a thread pool
- Natural blocking I/O — no callbacks or reactive chains
- Minimal per-thread memory overhead
- Code that reads sequentially and stays debuggable

`swarm-share-lite` intentionally embraces this model to stay readable while remaining highly concurrent.

---

## Build

```bash
git clone <repo>
cd swarm-share-lite
./gradlew build
./gradlew test
```

---

## Non-Goals (v1)

- No DHT-based peer discovery
- No encryption / TLS
- No GUI
- No persistent peer database

---

**Java 25 · Virtual Threads · Project Loom · MIT License**
