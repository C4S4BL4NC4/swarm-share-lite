// storage/FileChannelStorage.java
package io.swarmshare.storage;

import io.swarmshare.core.domain.ChunkDescriptor;
import io.swarmshare.core.domain.ChunkId;
import io.swarmshare.core.domain.Manifest;
import io.swarmshare.core.port.StorageProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;
import java.util.Optional;

/**
 * Writes chunks directly to their byte offset in a pre-allocated output file.
 * Uses FileChannel for position-aware reads and writes — no sequential constraint.
 * <p>
 * Why FileChannel over FileOutputStream?
 * - FileChannel.write(buffer, position) writes at explicit offset without seeking
 * - Multiple virtual threads can write to different offsets concurrently
 * - FileOutputStream requires sequential writes; concurrent seeks need external locking
 */
public class FileChannelStorage implements StorageProvider {

    private final Path outputPath;
    private final ChecksumVerifier verifier;

    // FileChannel is thread-safe for positional I/O at different offsets
    private FileChannel channel;

    public FileChannelStorage(Path outputPath) {
        this.outputPath = outputPath;
        this.verifier = new ChecksumVerifier();
    }

    /**
     * Pre-allocates output file to exact size.
     * Allows all chunks to write to their target offsets immediately.
     */
    @Override
    public void preallocateSpace(long totalSize) {
        try {
            channel = FileChannel.open(outputPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE);

            // truncate() sets logical size but doesn't allocate on all filesystems
            channel.truncate(totalSize);

            // Force allocation by writing a single byte at the end
            // This ensures the OS reserves all totalSize bytes
            ByteBuffer marker = ByteBuffer.allocate(1);
            channel.write(marker, totalSize - 1);

            // Sync to disk (optional but ensures durability)
            channel.force(false);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to preallocate output file", e);
        }
    }

    /**
     * Writes chunk data to exact byte offset.
     * Thread-safe for concurrent writes to different offsets.
     */
    @Override
    public void writeChunk(ChunkId id, long offset, byte[] data) {
        try {
            // Wrap data array without copying; reuses existing memory
            ByteBuffer buffer = ByteBuffer.wrap(data);

            // Loop handles partial writes (rare, but FileChannel contract requires it)
            while (buffer.hasRemaining()) {
                // Write from current position to file offset
                // Position is atomic; safe for concurrent calls at different offsets
                channel.write(buffer, offset + buffer.position());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write chunk " + id.index(), e);
        }
    }

    /**
     * Reads chunk data from exact byte offset.
     * Returns empty Optional if file is shorter than expected.
     */
    @Override
    public Optional<byte[]> readChunk(ChunkId id, long offset, int size) {
        try {
            // Allocate buffer to hold exactly 'size' bytes
            ByteBuffer buffer = ByteBuffer.allocate(size);
            int bytesRead = 0;

            // Loop until all bytes read or EOF reached
            while (bytesRead < size) {
                // Read from file offset; -1 signals EOF
                int n = channel.read(buffer, offset + bytesRead);
                if (n == -1) return Optional.empty(); // File shorter than expected

                // Accumulate total bytes read
                bytesRead += n;
            }

            // Return complete chunk as byte array
            return Optional.of(buffer.array());
        } catch (IOException e) {
            // Return empty on any I/O error (file corruption, missing chunk, etc.)
            return Optional.empty();
        }
    }

    /**
     * Validates all chunks in manifest: reads each and verifies checksum.
     * Returns BitSet with bit=1 for chunks that exist and match expected hash.
     */
    @Override
    public BitSet checkExistingChunks(Manifest manifest) {
        // Create BitSet sized for total chunk count (all bits initially 0)
        BitSet existing = new BitSet(manifest.totalChunks());

        // Check each chunk declared in manifest
        for (ChunkDescriptor desc : manifest.chunks()) {
            readChunk(desc.id(), desc.offset(), desc.size())
                    // Verify checksum matches expected SHA256
                    .filter(data -> verifier.verify(data, desc.sha256()))
                    // If both read and checksum succeeded, mark as existing
                    .ifPresent(_ -> existing.set(desc.id().index()));
        }

        // Return bitmap: 1 = valid chunk exists, 0 = missing or corrupted
        return existing;
    }

    /**
     * Closes file channel; swallows exceptions (safe for cleanup).
     */
    public void close() {
        try {
            // Close channel if open; no-op if already null
            if (channel != null) channel.close();
        } catch (IOException e) {
            // Swallow on close (log if instrumented; don't propagate)
        }
    }
}