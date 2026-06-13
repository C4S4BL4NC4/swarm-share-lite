package io.swarmshare.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileChannelStorageTest {
    private static final Path TEST_FILE = Path.of("fileChannelStorageTest.txt");
    private FileChannelStorage storage;

    @BeforeEach
    void setUp() {
        // Create fresh storage instance for each test
        storage = new FileChannelStorage(TEST_FILE);
    }

    @AfterEach
    void tearDown() {
        // Clean up: close channel and delete test file
        storage.close();
        try {
            Files.deleteIfExists(TEST_FILE);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    void preallocateSpace() {
        // GIVEN: storage with unallocated file
        long expectedSize = 1024;

        // WHEN: preallocate space
        storage.preallocateSpace(expectedSize);

        // THEN: file exists and has exact size
        assertTrue(Files.exists(TEST_FILE), "File should exist after preallocation");
        long actualSize = assertDoesNotThrow(() -> Files.size(TEST_FILE),
                "Should read file size without error");
        assertEquals(expectedSize, actualSize, "File size should match preallocation");
    }

    @Test
    void writeChunk() {
        // TODO: preallocate space, write chunk at offset, verify bytes written
    }

    @Test
    void readChunk() {
        // TODO: preallocate space, write test data, read it back, assert content matches
    }

    @Test
    void checkExistingChunks() {
        // TODO: create manifest, write valid + invalid chunks, verify BitSet marks only valid ones
    }

    @Test
    void close() {
        // TODO: verify channel closes without exception, subsequent ops throw
    }
}