package com.cyberon.engine;

import java.io.File;

import android.content.Context;

import com.cyberon.dspotterutility.ToolKit;

public class LoadLibrary {
    /**
     * Load libDSpotter.so from the application's library path. If load fail, it
     * will try to load from system path.
     *
     * @param oAppContext [in] The application context.
     */
    public static void loadLibrary(Context oAppContext) {
        String sFile = ToolKit.getNativeLibPath(oAppContext)
                + "/libDSpotter.so";
        File file = new File(sFile);
        if (file.exists())
            System.load(sFile);
        else
            System.loadLibrary("DSpotter");
    }
}
