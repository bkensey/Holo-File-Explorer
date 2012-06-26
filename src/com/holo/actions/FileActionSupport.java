package com.holo.actions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;

import com.holo.fileexplorer.R;
import com.holo.fileexplorer.FileMeta;
import com.holo.fileexplorer.MainActivity;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.RootToolsException;

import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.util.TypedValue;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.Toast;
import android.graphics.drawable.BitmapDrawable;

public final class FileActionSupport implements Constants {

	private static final String TAG = FileActionSupport.class.getName();

	private FileActionSupport() {
	}

	static boolean isMusic(File file) {

		Uri uri = Uri.fromFile(file);
		String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
				MimeTypeMap.getFileExtensionFromUrl(uri.toString()));

		if (type == null)
			return false;
		else
			return (type.toLowerCase().startsWith("audio/"));

	}

	static boolean isVideo(File file) {

		Uri uri = Uri.fromFile(file);
		String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
				MimeTypeMap.getFileExtensionFromUrl(uri.toString()));

		if (type == null)
			return false;
		else
			return (type.toLowerCase().startsWith("video/"));
	}

	public static boolean isPicture(File file) {

		Uri uri = Uri.fromFile(file);
		String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
				MimeTypeMap.getFileExtensionFromUrl(uri.toString()));

		if (type == null)
			return false;
		else
			return (type.toLowerCase().startsWith("image/"));
	}

	public static boolean isProtected(File path) {
		return (!path.canRead() && !path.canWrite());
	}
	
	public static boolean isReadable(String path) {
		List<String> output = null;
		
		try {
			String command = "ls -la " + path;
			output = RootTools.sendShell(command, 10000);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RootToolsException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (TimeoutException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		ListIterator<String> iterator = output.listIterator();
		String[] fields = iterator.next().split("\\s+");
		return (fields[0].substring(1,8) == "r");
	}

	public static boolean isRoot(File dir) {

		return dir.getAbsolutePath().equals("/");
	}

	public static boolean isSdCard(File file) {

		try {
			return (file.getCanonicalPath().equals(Environment
					.getExternalStorageDirectory().getCanonicalPath()));
		} catch (IOException e) {
			return false;
		}

	}

	public static Drawable getIcon(Context context, File file) {

		if (!file.isFile()) // dir
		{
			if (FileActionSupport.isProtected(file)) {
				return context.getResources().getDrawable(
						R.drawable.file_extension_dir_sys);

			} else if (FileActionSupport.isSdCard(file)) {
				return context.getResources().getDrawable(
						R.drawable.file_extension_dir_sdcard);
			} else {
				return context.getResources().getDrawable(
						R.drawable.file_extension_dir);
			}
		} else // file
		{
			String fileName = file.getName();
			if (FileActionSupport.isProtected(file)) {
				return context.getResources().getDrawable(
						R.drawable.file_extension_sys);

			}
			if (fileName.endsWith(".apk")) {
				String filePath = file.getPath();
				PackageInfo packageInfo = context.getPackageManager()
						.getPackageArchiveInfo(filePath,
								PackageManager.GET_ACTIVITIES);
				if (packageInfo != null) {
					ApplicationInfo appInfo = packageInfo.applicationInfo;
					if (Build.VERSION.SDK_INT >= 8) {
						appInfo.sourceDir = filePath;
						appInfo.publicSourceDir = filePath;
					}
					return appInfo.loadIcon(context.getPackageManager());
				}
				return null;
			}
			if (fileName.endsWith(".zip")) {
				return context.getResources().getDrawable(
						R.drawable.file_extension_zip);
			} else if (FileActionSupport.isMusic(file)) {
				return context.getResources().getDrawable(
						R.drawable.file_extension_ogg);
			} else if (FileActionSupport.isVideo(file)) {
				return context.getResources().getDrawable(
						R.drawable.file_extension_mp4);
			} else if (FileActionSupport.isPicture(file)) {
				return context.getResources().getDrawable(
						R.drawable.file_extension_jpeg);
			} else {
				return context.getResources().getDrawable(
						R.drawable.file_extension_generic);
			}
		}

	}

	public static boolean delete(File fileToBeDeleted) {

		try {
			FileUtils.forceDelete(fileToBeDeleted);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	public static boolean mkDir(String canonicalPath, CharSequence newDirName) {

		File newdir = new File(canonicalPath + File.separator + newDirName);
		return newdir.mkdirs();

	}

	/*
	 * @ RETURNS: A file name that is guaranteed to not exist yet.
	 * 
	 * PARAMS: context - Application context. path - The path that the file is
	 * supposed to be in. fileName - Desired file name. This name will be
	 * modified to create a unique file if necessary.
	 */
	public static File createUniqueCopyName(Context context, File path,
			String fileName) {
		// Does that file exist?
		File file = FileUtils.getFile(path, fileName);

		if (!file.exists()) {
			// Nope - we can take that.
			return file;
		}

		// Split file's name and extension to fix internationalization issue
		// #307
		int fromIndex = fileName.lastIndexOf('/');
		String extension = "";
		if (fromIndex > 0) {
			extension = fileName.substring(fromIndex);
			fileName = fileName.substring(0, fromIndex);
		}

		// Try a simple "copy of".
		file = FileUtils.getFile(
				path,
				context.getString(R.string.copied_file_name, fileName).concat(
						extension));

		if (!file.exists()) {
			// Nope - we can take that.
			return file;
		}

		int copyIndex = 2;

		// Well, we gotta find a unique name at some point.
		while (copyIndex < 500) {
			file = FileUtils.getFile(
					path,
					context.getString(R.string.copied_file_name_2, copyIndex,
							fileName).concat(extension));

			if (!file.exists()) {
				// Nope - we can take that.
				return file;
			}

			copyIndex++;
		}

		// I GIVE UP.
		return null;
	}

	public final static int convertDimensionPixelsToPixels(Context context,
			float dimensionPixels) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dimensionPixels * (scale) + 0.5f);
	}

	private static String getSizeStr(long bytes) {

		if (bytes >= FileUtils.ONE_GB) {
			return (double) Math
					.round((((double) bytes / FileUtils.ONE_GB) * 100))
					/ 100
					+ " GB";
		} else if (bytes >= FileUtils.ONE_MB) {
			return (double) Math
					.round((((double) bytes / FileUtils.ONE_MB) * 100))
					/ 100
					+ " MB";
		} else if (bytes >= FileUtils.ONE_KB) {
			return (double) Math
					.round((((double) bytes / FileUtils.ONE_KB) * 100))
					/ 100
					+ " KB";
		}

		return bytes + " bytes";
	}

	public static Map<String, Long> getDirSizes(File dir) {
		Map<String, Long> sizes = new HashMap<String, Long>();

		try {

			Process du = Runtime.getRuntime().exec(
					"/system/bin/du -b -d1 " + dir.getCanonicalPath(),
					new String[] {}, Environment.getRootDirectory());

			BufferedReader in = new BufferedReader(new InputStreamReader(
					du.getInputStream()));
			String line = null;
			while ((line = in.readLine()) != null) {
				String[] parts = line.split("\\s+");

				String sizeStr = parts[0];
				Long size = Long.parseLong(sizeStr);

				String path = parts[1];

				sizes.put(path, size);
			}

		} catch (IOException e) {
			Log.w(TAG,
					"Could not execute DU command for " + dir.getAbsolutePath(),
					e);
		}

		return sizes;

	}

	public static int convertDip2Pixels(Context context, int dip) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				dip, context.getResources().getDisplayMetrics());
	}

	/**
	 * Convert File into Uri.
	 * 
	 * @param file
	 * @return uri
	 */
	public static Uri getUri(File file) {
		if (file != null) {
			return Uri.fromFile(file);
		}
		return null;
	}

	public static boolean isNumeric(String input) {
		try {
			Long.parseLong(input);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}