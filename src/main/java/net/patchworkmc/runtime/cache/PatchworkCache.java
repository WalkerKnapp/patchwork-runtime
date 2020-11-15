package net.patchworkmc.runtime.cache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public interface PatchworkCache {
    boolean containsUnpatchedMd5(String unpatchedHash);

    String getNameByUnpatchedHash(String unpatchedHash);

    String getPatchedHashByUnpatchedHash(String unpatchedHash);

    void addEntry(String fileName, String unpatchedHash, String patchedHash);

    void save();

    Logger logger = LogManager.getLogger("patchwork-runtime");

    static PatchworkCache readOrCreateCache(Path cacheLocation) {
        PatchworkCache cache;

        try {
            cache = new FilePatchworkCache(cacheLocation);

            if (Files.exists(cacheLocation)) {
                // Read our existing cache data
                try {
                    ((FilePatchworkCache) cache).read();
                } catch (IOException e) {
                    logger.error("Failed to read patchwork cache. Trying to delete...", e);

                    Files.delete(cacheLocation);

                    // After deleting, try to recreate a new one
                    cache.save();
                }
            } else {
                // Create the cache file
                cache.save();
            }
        } catch (Throwable t) {
            logger.error("Failed when fetching from the Patchwork Cache. Reverting to using no cache.", t);
            cache = new NullPatchworkCache();
        }

        return cache;
    }

    static PatchworkCache createNullCache() {
        return new NullPatchworkCache();
    }
}
