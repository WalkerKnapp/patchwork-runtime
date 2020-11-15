package net.patchworkmc.runtime;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.mojang.bridge.game.GameVersion;
import net.fabricmc.loader.FabricLoader;
import net.fabricmc.loader.util.FileSystemUtil;
import net.minecraft.SharedConstants;
import net.patchworkmc.manifest.mod.ManifestParseException;
import net.patchworkmc.manifest.mod.ModManifest;
import net.patchworkmc.patcher.Patchwork;
import net.patchworkmc.patcher.util.MinecraftVersion;
import net.patchworkmc.runtime.cache.PatchworkCache;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RuntimePatcher {
    private static final Logger logger = LogManager.getLogger("patchwork-runtime");

    // TODO: Turn this into a configuration file
    private static final boolean useCache = true;

    private final FabricLoader loader;
    private final Patchwork patchwork;

    private final Path inputDir;
    private final Path outputDir;
    private final Path dataDir;

    private final PatchworkCache cache;

    public RuntimePatcher(FabricLoader fabricLoader) {
        try {
            Path patchworkCacheDir = fabricLoader.getModsDir().resolve(".patchwork");

            inputDir = patchworkCacheDir.resolve("unpatched");
            outputDir = patchworkCacheDir.resolve("patched");
            dataDir = patchworkCacheDir.resolve("data");

            loader = fabricLoader;
            patchwork = Patchwork.create(inputDir, outputDir, dataDir, getVersion());

            cache = useCache ? PatchworkCache.readOrCreateCache(patchworkCacheDir.resolve("patchedcache.json")) : PatchworkCache.createNullCache();
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private MinecraftVersion getVersion() {
        GameVersion gameVersion = SharedConstants.getGameVersion();

        for (MinecraftVersion v : MinecraftVersion.values()) {
            if (v.getVersion().equals(gameVersion.getName())) {
                return v;
            }
        }

        throw new IllegalStateException("This version of patchwork-runtime does not support Minecraft " + gameVersion.getName());
    }

    private Stream<Path> collectForgeMods() {
        try {
            return Files.list(loader.getModsDir())
                    .filter(modPath -> modPath.toString().endsWith("jar"))
                    .filter(modPath -> {
                        try {
                            try (FileSystem fs = FileSystemUtil.getJarFileSystem(modPath, false).get()) {
                                Path manifestPath = fs.getPath("/META-INF/mods.toml");

                                FileConfig toml = FileConfig.of(manifestPath);
                                toml.load();

                                Map<String, Object> map = toml.valueMap();
                                ModManifest manifest = ModManifest.parse(map);
                                return manifest.getModLoader().equals("javafml");
                            }
                        } catch (IOException | ManifestParseException e) {
                            return false;
                        }
                    });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<Path> patchMods() {
        List<Path> patchedJars = new ArrayList<>();
        List<Path> forgeJars = collectForgeMods().collect(Collectors.toList());
        List<Path> remainingForgeJars = new ArrayList<>(forgeJars);

        for (Path forgeJar : forgeJars) {
            String unpatchedHash = md5HashFile(forgeJar);

            if (cache.containsUnpatchedMd5(unpatchedHash)) {
                Path patchedPath = outputDir.resolve(cache.getNameByUnpatchedHash(unpatchedHash));

                if (Files.exists(patchedPath)) {
                    if (md5HashFile(patchedPath).equals(cache.getPatchedHashByUnpatchedHash(unpatchedHash))) {
                        patchedJars.add(patchedPath);
                        remainingForgeJars.remove(forgeJar);
                    } else {
                        logger.error("Cached patched jar at " + patchedPath.toAbsolutePath()
                                + " does not match the cached hash: " + cache.getPatchedHashByUnpatchedHash(unpatchedHash)
                                + ". Repatching...");
                        try {
                            Files.delete(patchedPath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    logger.error("Cached patched jar at " + patchedPath.toAbsolutePath() + " does not exist. Repatching...");
                }
            }
        }

        // Patch the jars we have remaining
        try {
            Files.list(inputDir).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });

            for (Path forgeJar : remainingForgeJars) {
                Files.copy(forgeJar, inputDir.resolve(forgeJar.getFileName()));
            }

            patchwork.patchAndFinish();

            for (Path forgeJar : remainingForgeJars) {
                Path patchedJar = outputDir.resolve(forgeJar.getFileName());

                if (!Files.exists(patchedJar)) {
                    throw new IllegalStateException("Failed to create patched jar for mod " + forgeJar.getFileName() + ".");
                }

                cache.addEntry(forgeJar.getFileName().toString(), md5HashFile(forgeJar), md5HashFile(patchedJar));

                patchedJars.add(patchedJar);
            }

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        cache.save();

        return patchedJars;
    }

    public String md5HashFile(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            return DigestUtils.md5Hex(is);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
