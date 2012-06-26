package com.beacon.crawlers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import android.widget.Toast;

import com.holo.actions.FileActions;
import com.holo.fileexplorer.MainActivity;
import com.holo.fileexplorer.R;

public class ArchiveInstruction {
	private static File COPIED_FILE = null;
	private static int pasteMode = 0;
	private static boolean pasteAvailaible;

	public static final int PASTE_MODE_COPY = 1;
	public static final int PASTE_MODE_MOVE = 2;
	private static final int success = 0;
	private static final int err_copying_file = 1;
	private static final int err_pasting_file = 2;
	
	private String destinationDirectory;
	private String zipFileName;
	public List<File> ZIP_FILES = new ArrayList<File>();

	protected static final String TAG = FileActions.class.getName();

	public void markFilesForZip(ArrayList<File> files, MainActivity mContext) {
		Log.v(TAG, "Started copyFiles");
		if (files instanceof List<?>) {
			for (File file : (List<File>) files) {
				int result = addZipSrcFile(file);
				if (result != success)
					Toast.makeText(
							mContext.getApplicationContext(),
							mContext.getString(R.string.copy_failed,
									file.getName()), Toast.LENGTH_SHORT).show();
			}
			Toast.makeText(mContext.getApplicationContext(),
					mContext.getString(R.string.copied_toast),
					Toast.LENGTH_SHORT).show();
		} else
			Toast.makeText(mContext.getApplicationContext(),
					"Error handling selected list to be zipped",
					Toast.LENGTH_SHORT).show();
	}
	
	private synchronized int addZipSrcFile(File file) {
		try {
			ZIP_FILES.add(file);
		} catch (Exception UnsupportedOperationException) {
			return err_copying_file;
		}
		return success;
	}
	
	public synchronized void clearPaste() {
		ZIP_FILES = new ArrayList<File>();
		pasteAvailaible = false;
	} 
}
