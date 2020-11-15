package net.patchworkmc.runtime.util;

import java.io.OutputStream;

public class NullOutputStream extends OutputStream {
    @Override
    public void write(int b) {
        // Do Nothing
    }
}
