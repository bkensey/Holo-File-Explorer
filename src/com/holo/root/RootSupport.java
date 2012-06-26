package com.holo.root;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;

public class RootSupport {

	public static ArrayList<ExtendedMount> getMounts() throws FileNotFoundException,
			IOException {
		LineNumberReader lnr = null;
		try {
			lnr = new LineNumberReader(new FileReader("/proc/mounts"));
			String line;
			ArrayList<ExtendedMount> mounts = new ArrayList<ExtendedMount>();
			while ((line = lnr.readLine()) != null) {

				String[] fields = line.split(" ");
				mounts.add(new ExtendedMount(new File(fields[0]), // device
						new File(fields[1]), // mountPoint
						fields[2], // fstype
						fields[3] // flags
				));
			}
			return mounts;
		} finally {
			// no need to do anything here.
		}
	}
	
	public boolean isMount (String path) {
		return false;
	}

}
