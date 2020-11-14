package net.patchworkmc.runtime;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.mojang.bridge.game.GameVersion;
import net.fabricmc.loader.FabricLoader;
import net.minecraft.SharedConstants;
import net.patchworkmc.manifest.mod.ManifestParseException;
import net.patchworkmc.manifest.mod.ModManifest;
import net.patchworkmc.patcher.Patchwork;
import net.patchworkmc.patcher.util.MinecraftVersion;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

public class RuntimePatcher {
    private final FabricLoader loader;
    private final Patchwork patchwork;

    private final Path inputDir;
    private final Path outputDir;
    private final Path dataDir;

    public RuntimePatcher(FabricLoader fabricLoader) {
        try {
            Path patchworkCacheDir = fabricLoader.getModsDir().resolve(".patchwork");

            inputDir = patchworkCacheDir.resolve("unpatched");
            outputDir = patchworkCacheDir.resolve("patched");
            dataDir = patchworkCacheDir.resolve("data");

            loader = fabricLoader;
            patchwork = Patchwork.create(inputDir, outputDir, dataDir, getVersion());
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
            return Files.list(loader.getModsDir()).filter(modPath -> {
                try {
                    URI jarUri = new URI("jar:" + modPath.toUri());
                    try (FileSystem fs = FileSystems.newFileSystem(jarUri, Collections.emptyMap())) {
                        Path manifestPath = fs.getPath("/META-INF/mods.toml");

                        FileConfig toml = FileConfig.of(manifestPath);
                        toml.load();

                        Map<String, Object> map = toml.valueMap();
                        ModManifest manifest = ModManifest.parse(map);
                        return manifest.getModLoader().equals("javafml");
                    }
                } catch (URISyntaxException | IOException | ManifestParseException e) {
                    return false;
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
