# swarm-share-lite

**Logarithmic P2P file distribution using Java Virtual Threads.**

Every receiver becomes a sender. Transfer time scales with peers, not file size.

## The Idea

Copy a 300 GB file across 14 machines:
- **Naive (2 sources):** One source uploads 7 times sequentially — slow.
- **swarm-share:** As soon as node B receives chunk 1, it serves chunk 1 while node A serves chunk 2 to node C. Parallel spread. **Logarithmic scaling.**

```
Round 1:  2 sources   → 4 nodes total
Round 2:  4 sources   → 8 nodes total
Round 3:  8 sources   → 16 nodes total
```

## How It Works

1. **Manifest** — Seeder splits file into fixed chunks (1 MB each), computes SHA-256 per chunk, publishes manifest.json to all peers.
2. **Discover** — Leechers ask peers: "which chunks do you hold?" (BitSet exchange).
3. **Download** — Each leecher downloads missing chunks in parallel from whoever has them, verifies checksum, writes to disk.
4. **Resume** — Restart? Scan disk, verify existing chunks, continue from where you left off.

## Why Java 25 & Virtual Threads?

10,000 concurrent downloads with ~1 MB stack overhead each is traditional thread suicide. Virtual threads: ~1 KB each, managed by the JVM, cheap to park on I/O waits. Write blocking code; Loom handles scheduling.

## Quick Start

### Seed (advertise a file)

```bash
./gradlew cli:run --args="seed --file ubuntu-25.04.iso --port 7070"
```

Writes `manifest.json` to stdout.

### Leech (download from peers)

```bash
./gradlew cli:run --args="leech \
  --manifest manifest.json \
  --peer 192.168.1.10:7070 \
  --peer 192.168.1.11:7070 \
  --output ubuntu-25.04.iso"
```

Downloads in parallel. Resume by re-running — already-downloaded chunks are skipped.

## Architecture

```
Domain (pure Java, no I/O)
    ↑ implements
Ports (StorageProvider, PeerConnector)
    ↑ implemented by
Infrastructure (FileChannelStorage, TcpPeerConnector)
```

- **core/** — Domain types, interfaces. No dependencies on storage or networking.
- **storage/** — FileChannel-based random-access chunk reads/writes.
- **networking/** — TCP binary framing, ServerSocket listener, async fetch.
- **transfer/** — Orchestrator. Coordinates downloads, retries, state tracking.
- **manifest/** — Build and serialize chunk metadata.
- **cli/** — Entry points (`seed`, `leech`).

## Testing

- **Unit tests** — Test doubles (InMemoryStorage, FakePeerConnector) for core logic.
- **Integration tests** — Two-node loopback test: start seeder, run leecher, verify byte-for-byte match.

```bash
./gradlew test
```

## Phased Roadmap

| Phase | Focus |
|-------|-------|
| **1** | Domain modeling, TDD |
| **2** | Random-access storage (FileChannel) |
| **3** | Binary TCP framing |
| **4** | Virtual thread concurrency + backpressure |
| **5** | Multi-peer orchestration, CLI |
| **6** | Failure recovery, retry policy |

## Status

**Phase 2** — FileChannelStorage written, tested. BitSet piece map sync in progress.

## Non-Goals (v1)

- No DHT / peer discovery — static peer list.
- No TLS.
- No GUI.

---

**Language:** Java 25 | **Platform:** Linux | **License:** MIT
