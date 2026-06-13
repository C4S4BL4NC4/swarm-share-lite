// storage/ChecksumVerifier.java
package io.swarmshare.storage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Verifies chunk integrity using SHA-256 hashing.
 * <p>
 * MessageDigest is NOT thread-safe — never share an instance across threads.
 * Options:
 * 1. Create per call (simple, slight GC pressure — fine for chunk-sized work)
 * 2. ThreadLocal (reuse, zero contention across threads)
 * <p>
 * We use ThreadLocal for production: each virtual thread gets its own instance.
 * TODO: Verify virtual thread creation doesn't spawn excessive MessageDigest objects.
 */
public class ChecksumVerifier {

    // ThreadLocal provides per-thread SHA-256 hasher.
    // Lazily initialized on first access; reused across verify() calls in same thread.
    // Avoids synchronization and MessageDigest construction overhead.
    private static final ThreadLocal<MessageDigest> DIGEST =
            ThreadLocal.withInitial(() -> {
                try {
                    // Obtain SHA-256 algorithm; fails if not available on platform
                    return MessageDigest.getInstance("SHA-256");
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            });

    /**
     * Computes SHA-256 hash of input data as hex string.
     * Resets MessageDigest before use to clear state from prior calls.
     */
    public String compute(byte[] data) {
        // Retrieve thread-local hasher instance
        MessageDigest md = DIGEST.get();

        // Reset internal state; required before reusing same MessageDigest
        md.reset();

        // Hash input and convert bytes to uppercase hex string (e.g., "a1b2c3...")
        return HexFormat.of().formatHex(md.digest(data));
    }

    /**
     * Verifies input data matches expected SHA-256 hash.
     * Returns true if computed hash equals expected hash (case-insensitive).
     */
    public boolean verify(byte[] data, String expectedHash) {
        // Guard: null hash is always invalid
        if (expectedHash == null) {
            return false;
        }

        // Compute actual hash of input data
        String computed = compute(data);

        // Compare case-insensitively; return true only if hashes match exactly
        return computed.equalsIgnoreCase(expectedHash);
    }
}
