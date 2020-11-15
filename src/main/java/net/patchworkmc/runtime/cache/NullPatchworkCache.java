package net.patchworkmc.runtime.cache;

import java.nio.file.Path;

public class NullPatchworkCache implements PatchworkCache {
    NullPatchworkCache() {

    }

    @Override
    public boolean containsUnpatchedMd5(String unpatchedHash) {
        return false;
    }

    @Override
    public String getNameByUnpatchedHash(String unpatchedHash) {
        // This method should never be called
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPatchedHashByUnpatchedHash(String unpatchedHash) {
        // This method should never be called
        throw new UnsupportedOperationException();
    }

    @Override
    public void addEntry(String fileName, String unpatchedHash, String patchedHash) {
        // Do nothing
    }

    @Override
    public void save() {
        // Do nothing
    }
}
