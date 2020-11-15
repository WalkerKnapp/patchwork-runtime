package net.patchworkmc.runtime.cache;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

class FilePatchworkCache implements PatchworkCache {
    private static final Gson gson = new Gson();

    private static class CacheEntry {
        String name;
        String patchedMd5;

        private CacheEntry(String name, String patchedMd5) {
            this.name = name;
            this.patchedMd5 = patchedMd5;
        }
    }

    private final Path cacheLocation;

    // A map of unpatched md5 hashes => names+patched hashs
    private static final Type cacheMapType = new TypeToken<Map<String, CacheEntry>>() { }.getType();
    public Map<String, CacheEntry> cache;

    @Override
    public boolean containsUnpatchedMd5(String unpatchedHash) {
        return cache.containsKey(unpatchedHash);
    }

    @Override
    public String getNameByUnpatchedHash(String unpatchedHash) {
        return cache.get(unpatchedHash).name;
    }

    @Override
    public String getPatchedHashByUnpatchedHash(String unpatchedHash) {
        return cache.get(unpatchedHash).patchedMd5;
    }

    @Override
    public void addEntry(String fileName, String unpatchedHash, String patchedHash) {
        cache.put(unpatchedHash, new CacheEntry(fileName, patchedHash));
    }

    @Override
    public void save() {
        try (Writer writer = Files.newBufferedWriter(cacheLocation)) {
            gson.toJson(cache, writer);
        } catch (IOException e) {
            logger.error("Failed to save patchwork cache.", e);
        }
    }

    public void read() throws IOException {
        try (Reader reader = Files.newBufferedReader(cacheLocation)) {
            cache = gson.fromJson(reader, cacheMapType);
        }
    }

    FilePatchworkCache(Path cacheLocation) {
        this.cache = new HashMap<>();
        this.cacheLocation = cacheLocation;
    }
}
