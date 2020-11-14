package net.patchworkmc.runtime;

import net.devtech.grossfabrichacks.entrypoints.PrePrePreLaunch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PreLaunchEntrypoint implements PrePrePreLaunch {
    private static final Logger logger = LogManager.getLogger("patchwork-runtime");
    @Override
    public void onPrePrePreLaunch() {
        logger.info(":ohno:");
    }
}
