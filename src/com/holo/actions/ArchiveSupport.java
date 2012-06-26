package com.holo.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.content.DialogInterface;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.beacon.crawlers.Archiver;
import com.holo.fileexplorer.R;
import com.holo.fileexplorer.MainActivity;

/**
 * The class provides the utility method to zip files or folders uses
 * java.util.zip package underneath
 */
public final class ArchiveSupport {

	private static List<File> ZIPPED_FILES = new ArrayList<File>();

	private static final int success = 0;
	private static final int err_zipping_file = 1;

	public static synchronized int addZipSrcFile(File file) {
		try {
			ZIPPED_FILES.add(file);
		} catch (Exception UnsupportedOperationException) {
			return err_zipping_file;
		}
		return success;
	}

	static public void zipFolder(String srcFolder, String destZipFile, AbortionFlag flag) throws Exception {

		if (flag.isAborted()) {
			return;
		}
		ZipOutputStream zip = null;
		FileOutputStream fileWriter = null;

		try {
			fileWriter = new FileOutputStream(destZipFile);
			zip = new ZipOutputStream(fileWriter);

			addFolderToZip("", srcFolder, flag, zip);
		} finally {
			zip.flush();
			zip.close();
		}
	}

	static public void zipFile(String srcFile, String destZipFile, AbortionFlag flag) throws Exception {

		if (flag.isAborted()) {
			return;
		}
		ZipOutputStream zip = null;
		FileOutputStream fileWriter = null;

		try {
			fileWriter = new FileOutputStream(destZipFile);
			zip = new ZipOutputStream(fileWriter);

			addFileToZip("", srcFile, flag, zip);
		} finally {
			zip.flush();
			zip.close();
		}
	}

	static private void addFileToZip(String path, String srcFile, AbortionFlag flag, ZipOutputStream zip) throws Exception {

		if (flag.isAborted()) {
			return;
		}
		File folder = new File(srcFile);
		if (folder.isDirectory()) {
			addFolderToZip(path, srcFile, flag, zip);
		} else {
			byte[] buf = new byte[1024];
			int len;
			FileInputStream in = new FileInputStream(srcFile);
			zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
			while ((len = in.read(buf)) > 0) {
				zip.write(buf, 0, len);
			}
		}
	}

	static private void addFolderToZip(String path, String srcFolder, AbortionFlag flag, ZipOutputStream zip) throws Exception {

		if (flag.isAborted()) {
			return;
		}
		File folder = new File(srcFolder);

		for (String fileName : folder.list()) {
			if (path.equals("")) {
				addFileToZip(folder.getName(), srcFolder + "/" + fileName, flag, zip);
			} else {
				addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, flag, zip);
			}
		}
	}
}
