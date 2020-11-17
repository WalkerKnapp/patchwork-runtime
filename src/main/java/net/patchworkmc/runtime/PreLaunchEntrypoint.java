package net.patchworkmc.runtime;

import net.devtech.grossfabrichacks.entrypoints.PrePrePreLaunch;
import net.fabricmc.loader.FabricLoader;
import net.fabricmc.loader.discovery.ModCandidate;
import net.fabricmc.loader.discovery.ModResolver;
import net.fabricmc.loader.discovery.RuntimeModRemapper;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.loader.metadata.LoaderModMetadata;
import net.fabricmc.loader.metadata.ModMetadataParser;
import net.fabricmc.loader.metadata.ParseMetadataException;
import net.fabricmc.loader.util.FileSystemUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

public class PreLaunchEntrypoint implements PrePrePreLaunch {
    private static final Logger logger = LogManager.getLogger("patchwork-runtime");

    @Override
    public void onPrePrePreLaunch() {
        FabricLoader loader = FabricLoader.INSTANCE;

        logger.info(":ohno: Runtime-patching Forge mods :ohno:");

        RuntimePatcher runtimePatcher = new RuntimePatcher(loader);

        Collection<Path> patchedMods = runtimePatcher.patchMods();

        logger.info("Loading " + patchedMods.size() + " patched Forge mods.");

        int oldModCount = FabricLoaderInterface.getModCount(loader);

        for (Path modJar : patchedMods) {
            loadMod(loader, modJar);
        }

        FabricLoaderInterface.setModCount(loader, oldModCount);
    }

    private void loadMod(FabricLoader loader, Path modPath) {
        ModCandidate candidate = parseMod(modPath);

        if (loader.isDevelopmentEnvironment()) {
            candidate = RuntimeModRemapper.remap(Collections.singletonList(candidate), ModResolver.getInMemoryFs()).stream().findFirst().get();
        }

        FabricLoaderInterface.addMod(loader, candidate);
        FabricLauncherBase.getLauncher().propose(candidate.getOriginUrl());
    }

    private ModCandidate parseMod(Path modPath) {
        try {
            FileSystemUtil.FileSystemDelegate jarFs = FileSystemUtil.getJarFileSystem(modPath, false);

            Path modJson = jarFs.get().getPath("fabric.mod.json");

            LoaderModMetadata info = ModMetadataParser.parseMetadata(logger, modJson);

            return new ModCandidate(info, modPath.toUri().toURL(), 0, true);
        } catch (IOException | ParseMetadataException e) {
            throw new IllegalStateException(e);
        }
    }
}
