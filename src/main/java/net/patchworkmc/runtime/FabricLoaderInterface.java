package net.patchworkmc.runtime;

import net.fabricmc.loader.FabricLoader;
import net.fabricmc.loader.ModContainer;
import net.fabricmc.loader.discovery.ModCandidate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.List;

public class FabricLoaderInterface {
    private static final Logger logger = LogManager.getLogger("patchwork-runtime");

    private static Method addModMethod;
    private static Field modsField;

    private static Field abstractListModCount;

    static {
        try {
            addModMethod = FabricLoader.class.getDeclaredMethod("addMod", ModCandidate.class);
            addModMethod.setAccessible(true);

            modsField = FabricLoader.class.getDeclaredField("mods");
            modsField.setAccessible(true);

            abstractListModCount = AbstractList.class.getDeclaredField("modCount");
            abstractListModCount.setAccessible(true);
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            logger.error("Failed to get reference to fabric-loader internals. The fabric-loader version may be incompatible with patchwork-runtime.", e);
        }
    }

    public static void addMod(FabricLoader fabricLoader, ModCandidate modCandidate) {
        try {
            addModMethod.invoke(fabricLoader, modCandidate);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.error("Failed to inject mod into fabric-loader.", e);
            throw new IllegalStateException(e);
        }
    }

    public static List<ModContainer> getMods(FabricLoader fabricLoader) {
        try {
            return (List<ModContainer>) modsField.get(fabricLoader);
        } catch (IllegalAccessException e) {
            logger.error("Failed to get mods from fabric-loader.", e);
            throw new IllegalStateException(e);
        }
    }

    public static int getModCount(FabricLoader fabricLoader) {
        try {
            return abstractListModCount.getInt(getMods(fabricLoader));
        } catch (IllegalAccessException e) {
            logger.error("Failed to get modCount from fabric-loader.", e);
            throw new IllegalStateException(e);
        }
    }

    public static void setModCount(FabricLoader fabricLoader, int modCount) {
        try {
            abstractListModCount.setInt(getMods(fabricLoader), modCount);
        } catch (IllegalAccessException e) {
            logger.error("Failed to set modCount in fabric-loader.", e);
            throw new IllegalStateException(e);
        }
    }
}
