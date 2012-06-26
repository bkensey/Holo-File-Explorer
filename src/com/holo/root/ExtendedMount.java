package com.holo.root;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ExtendedMount {
    final File device;
    final File mountPoint;
    final String type;
    final Set<String> flags;

    public ExtendedMount(File device, File path, String type, String flagsStr) {
        this.device = device;
        this.mountPoint = path;
        this.type = type;
        this.flags = new HashSet<String>( Arrays.asList(flagsStr.split(",")));
    }

    @Override
    public String toString() {
        return String.format( "%s on %s type %s %s", device, mountPoint, type, flags );
    }
    
    public boolean isMount(String path) {
    	return path == mountPoint.getAbsolutePath();
    }
}
