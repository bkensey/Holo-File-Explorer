package com.holo.actions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.beacon.crawlers.ArchiveInstruction;
import com.beacon.crawlers.Archiver;
import com.beacon.crawlers.Mover;
import com.holo.fileexplorer.R;
import com.holo.fileexplorer.MainActivity;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.Toast;

public class FileActions {

	private static File COPIED_FILE = null;
	private static List<File> COPIED_FILES = new ArrayList<File>();
	private static int pasteMode = 0;
	private static boolean pasteAvailaible;

	public static final int PASTE_MODE_COPY = 1;
	public static final int PASTE_MODE_MOVE = 2;
	private static final int success = 0;
	private static final int err_copying_file = 1;
	private static final int err_pasting_file = 2;

	protected static final String TAG = FileActions.class.getName();

	public static void copyFiles(ArrayList<File> files, MainActivity mContext) {
		Log.v(TAG, "Started copyFiles");
		clearPaste();
		setPasteMode(PASTE_MODE_COPY);
		pasteAvailaible = true;
		if (files instanceof List<?>) {
			for (File file : (List<File>) files) {
				int result = addPasteSrcFile(file);
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
					"Error handling selected list to be copied",
					Toast.LENGTH_SHORT).show();

		// TODO change either main menu or CAB menu to include paste and
		// "nevermind" and grey-out or remove copy/cut
		// mContext.invalidateOptionsMenu();
	}

	public static void cutFiles(final ArrayList<File> files,
			final MainActivity mContext) {
		clearPaste();
		Log.v(TAG, "Started cutFiles");
		setPasteMode(PASTE_MODE_MOVE);
		pasteAvailaible = true;
		if (files instanceof List<?>) {
			for (File file : (List<File>) files) {
				int result = addPasteSrcFile(file);
				if (result != success)
					Toast.makeText(
							mContext.getApplicationContext(),
							mContext.getString(R.string.cut_failed,
									file.getName()), Toast.LENGTH_SHORT).show();
			}
			Toast.makeText(mContext.getApplicationContext(),
					mContext.getString(R.string.cut_toast), Toast.LENGTH_SHORT)
					.show();
		} else
			Toast.makeText(mContext.getApplicationContext(),
					"Error handling selected list to be copied",
					Toast.LENGTH_SHORT).show();

		// setPasteSrcFile(file, PASTE_MODE_MOVE);
		// Toast.makeText(mContext.getApplicationContext(),
		// mContext.getString(R.string.cut_toast, file.getName()),
		// Toast.LENGTH_SHORT).show();
		// mContext.invalidateOptionsMenu();
	}

	public static boolean paste(Context context, int mode, File destinationDir,
			AbortionFlag flag) {
		int result = 3;
		Log.v(TAG, "Will now paste files on clipboard");
		for (File file : (List<File>) COPIED_FILES) {
			File fileBeingPasted = new File(file.getParent(), file.getName());
			if (doPaste(context, mode, file, destinationDir, flag)) {
				if (getPasteMode() == PASTE_MODE_MOVE) {
					if (fileBeingPasted.isFile()) {
						if (FileUtils.deleteQuietly(fileBeingPasted)) {
							Log.i(TAG, "File deleted after paste "
									+ fileBeingPasted.getAbsolutePath());
						} else {
							Log.w(TAG, "File NOT deleted after paste "
									+ fileBeingPasted.getAbsolutePath());
						}
					} else {
						try {
							FileUtils.deleteDirectory(fileBeingPasted);
						} catch (IOException e) {
							Log.e(TAG,
									"Error while deleting directory after paste - "
											+ fileBeingPasted.getAbsolutePath(),
									e);
							result = err_pasting_file;
							return false;
						}
					}
				}
				result = success;
			} else {
				Log.e(TAG,
						"Error while pasting "
								+ fileBeingPasted.getAbsolutePath());
				result = err_pasting_file;
				return false;
			}
		}
		return result == success;
	}

	private static boolean doPaste(Context context, int mode, File srcFile,
			File destinationDir, AbortionFlag flag) {

		if (!flag.isAborted())
			try {
				if (srcFile.isDirectory()) {

					File newDir = new File(destinationDir.getAbsolutePath()
							+ File.separator + srcFile.getName());
					newDir.mkdirs();

					for (File child : srcFile.listFiles()) {
						doPaste(context, mode, child, newDir, flag);
					}
					return true;
				} else {
					File uniqueDestination = FileActionSupport
							.createUniqueCopyName(context, destinationDir,
									srcFile.getName());
					FileUtils.copyFile(srcFile, uniqueDestination);
					return true;
				}
			} catch (Exception e) {
				return false;
			}
		else {
			return false;
		}
	}

	public static boolean canPaste() { // File destDir
		return pasteAvailaible;
	}

	private static synchronized int addPasteSrcFile(File file) {
		try {
			COPIED_FILES.add(file);
		} catch (Exception UnsupportedOperationException) {
			return err_copying_file;
		}
		return success;
	}

	public static synchronized void setPasteMode(int mode) {
		pasteMode = mode;
	}

	public static synchronized File getFileToPaste() {
		return COPIED_FILE;
	}

	public static synchronized int getPasteMode() {
		return pasteMode;
	}

	public static synchronized void clearPaste() {
		COPIED_FILES = new ArrayList<File>();
		pasteAvailaible = false;
	}

	public static synchronized void setPasteUnavailable() {
		pasteAvailaible = false;
	}

	public static Intent createShareIntent(ArrayList<File> selectedFiles,
			Context mContext) {

		if (selectedFiles.size() == 1) {

			final Intent intent = new Intent(Intent.ACTION_SEND);
			Uri uri = Uri.fromFile((File) selectedFiles.get(0));
			String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
					MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
			intent.setType(type);
			intent.setAction(Intent.ACTION_SEND);
			intent.setType(type == null ? "*/*" : type);
			intent.putExtra(Intent.EXTRA_STREAM, uri);
			// mContext.startActivity(Intent.createChooser(intent,
			// mContext.getString(R.string.share)));
			return intent;

		} else if (selectedFiles.size() > 1) {

			Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
			if (packageMarkedFiles(intent, selectedFiles)) {
				return intent;
				// mContext.startActivity(Intent.createChooser(intent,
				// mContext.getString(R.string.share)));
			}
			return null;

		} else {
			Toast.makeText(
					mContext,
					"Error. Share was somehow called without anything selected",
					Toast.LENGTH_SHORT).show();
			return null;
		}
	}

	public static String getMIMEcategory(String aMIMEtype) {
		if (aMIMEtype != null) {
			aMIMEtype = aMIMEtype.substring(0,
					aMIMEtype.lastIndexOf("/", aMIMEtype.length() - 1))
					+ "/*";
		} else {
			aMIMEtype = "*/*";
		}
		return aMIMEtype;
	}

	protected static boolean packageMarkedFiles(Intent aIntent,
			ArrayList<File> selectedFiles) {
		String theOverallMIMEtype = null;
		String theMIMEtype = null;
		String theOverallMIMEcategory = null;
		String theMIMEcategory = null;
		ArrayList<Uri> selectedFileUris = new ArrayList<Uri>();

		for (File file : selectedFiles) {
			Uri fileUri = Uri.fromFile(file);
			selectedFileUris.add(fileUri);
			theMIMEtype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
					MimeTypeMap.getFileExtensionFromUrl(fileUri.toString()));
			if (theOverallMIMEtype != null) {
				if (!theOverallMIMEtype.equals(theMIMEtype)) {
					theOverallMIMEcategory = getMIMEcategory(theOverallMIMEtype);
					theMIMEcategory = getMIMEcategory(theMIMEtype);
					if (!theOverallMIMEcategory.equals(theMIMEcategory)) {
						theOverallMIMEtype = "multipart/mixed";
						break; // no need to keep looking at the various types
					} else {
						theOverallMIMEtype = theOverallMIMEcategory + "/*";
					}
				} else {
					// nothing to do
				}
			} else {
				theOverallMIMEtype = theMIMEtype;
			}
		}

		if (selectedFileUris != null && theOverallMIMEtype != null) {
			aIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,
					selectedFileUris);
			aIntent.setType(theOverallMIMEtype == null ? "*/*"
					: theOverallMIMEtype);
			return true;
		} else {
			return false;
		}
	}

	public static void openFile(Context context, File aFile) {

		// if (FileExplorerUtils.isProtected(file) || file.isDirectory()) {
		// return;
		// }
		// Intent intent = new Intent();
		// intent.setAction(android.content.Intent.ACTION_VIEW);
		// Uri uri = Uri.fromFile(file);
		// String type =
		// MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
		// intent.setDataAndType(uri, type == null ? "*/*" : type);
		// startActivity((Intent.createChooser(intent,
		// getString(R.string.open_using))));

		if (!aFile.exists()) {
			Toast.makeText(context, R.string.file_not_exist, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);

		Uri uri = Uri.fromFile(aFile);
		String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
				MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
		intent.setDataAndType(uri, type == null ? "*/*" : type);

		/*
		 * TODO: uncomment the following when this app can handle types of
		 * content like text
		 */

		// // Were we in GET_CONTENT mode?
		// Intent originalIntent = getIntent();
		//
		// if (originalIntent != null && originalIntent.getAction() != null &&
		// originalIntent.getAction().equals(Intent.ACTION_GET_CONTENT)) {
		// // In that case, we should probably just return the requested data.
		// intent.setData(Uri.parse(FileManagerProvider.FILE_PROVIDER_PREFIX +
		// aFile));
		// setResult(RESULT_OK, intent);
		// finish();
		// return;
		// }

		try {
			context.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(context, R.string.application_not_available,
					Toast.LENGTH_SHORT).show();
		}
		;
	}

	public static void zipFiles(ArrayList<File> files, File destinationDir, String zipName, MainActivity mContext) {
		Log.v(TAG, "Started zipFiles");
			
		// new Archiver(destinationDir, zipName, mContext).execute(files);

		// Load files into an array prior to calling the archive asyncTask
		ArchiveInstruction zipper = new ArchiveInstruction();
		zipper.markFilesForZip(files, mContext);

		// Call the archive asyncTask and let it go nuts
		new Archiver(destinationDir, zipName, zipper, mContext).execute();

	}

	// public static void showProperties(final FileListEntry file, final
	// MainActivity mContext)
	// {
	// new Builder(mContext)
	// .setTitle(mContext.getString(R.string.properties_for, file.getName()))
	// .setItems(FileActionSupport.getFileProperties(file, mContext), new
	// OnClickListener() {
	//
	// @Override
	// public void onClick(DialogInterface dialog, int which) {
	//
	// }
	// })
	// .setPositiveButton(android.R.string.ok, new
	// DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog, int whichButton) {
	//
	// dialog.dismiss();
	// }
	// })
	// .show();
	// }

	// public static void deleteFile(final File file, final MainActivity
	// mContext,final OperationCallback<Void> callback)
	// {
	// AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
	// builder.setCancelable(true);
	// builder.setMessage(mContext.getString(R.string.confirm_delete,
	// file.getName()))
	// .setCancelable(false)
	// .setPositiveButton(android.R.string.ok, new
	// DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog, int id) {
	//
	// new Trasher(mContext, callback).execute(file);
	// }
	// })
	// .setNegativeButton(android.R.string.cancel, new
	// DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog, int id) {
	// dialog.cancel();
	//
	// }
	// }).setTitle(R.string.confirm);
	//
	// AlertDialog confirm = builder.create();
	// confirm.show();
	// }

	// public static int[] getContextMenuOptions(File file, MainActivity caller)
	// {
	//
	// PreferenceUtil prefs = new PreferenceUtil(caller);
	//
	// if(FileActionSupport.isProtected(file))
	// {
	// return null;
	// }
	// if(FileActionSupport.isSdCard(file))
	// {
	// if(prefs.isEnableSdCardOptions())
	// {
	// return new int[]{R.id.menu_rescan, R.id.menu_props};
	// }
	// else
	// {
	// return new int[]{R.id.menu_props};
	// }
	//
	// }
	// else if(file.isDirectory())
	// {
	// if(prefs.isZipEnabled())
	// {
	// return new int[]{R.id.menu_copy,R.id.menu_cut, R.id.menu_delete,
	// R.id.menu_rename, R.id.menu_zip, R.id.menu_props};
	// }
	// return new int[]{R.id.menu_copy, R.id.menu_cut, R.id.menu_delete,
	// R.id.menu_rename, R.id.menu_props};
	//
	// }
	// else
	// {
	// if(prefs.isZipEnabled())
	// {
	// return new int[]{R.id.menu_share, R.id.menu_copy, R.id.menu_cut,
	// R.id.menu_delete, R.id.menu_rename, R.id.menu_zip, R.id.menu_props};
	// }
	// return new int[]{R.id.menu_share, R.id.menu_copy, R.id.menu_cut,
	// R.id.menu_delete, R.id.menu_rename, R.id.menu_props};
	// }
	// }

	// public static void rename(final File file, final MainActivity mContext,
	// final OperationCallback<Void> callback)
	// {
	// final EditText input = new EditText(mContext);
	// input.setHint(mContext.getString(R.string.enter_new_name));
	// input.setSingleLine();
	//
	// new Builder(mContext)
	// .setTitle(mContext.getString(R.string.rename_dialog_title,
	// file.getName()))
	// .setView(input)
	// .setPositiveButton(android.R.string.ok, new
	// DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog, int whichButton) {
	//
	// CharSequence newName = input.getText();
	// try
	// {
	// File parentFolder = file.getParentFile();
	// if(file.renameTo(new File(parentFolder, newName.toString())))
	// {
	// if(callback!=null)
	// {
	// callback.onSuccess();
	// }
	// Toast.makeText(mContext, mContext.getString(R.string.rename_toast,
	// file.getName(), newName), Toast.LENGTH_LONG).show();
	// mContext.refresh();
	// }
	// else
	// {
	// if(callback!=null)
	// {
	// callback.onFailure(new Exception());
	// }
	// new Builder(mContext)
	// .setTitle(mContext.getString(R.string.error))
	// .setMessage(mContext.getString(R.string.rename_failed, file.getName()))
	// .show();
	// }
	//
	// }
	// catch (Exception e) {
	// if(callback!=null)
	// {
	// callback.onFailure(e);
	// }
	//
	// Log.e(TAG, "Error occured while renaming path", e);
	// new Builder(mContext)
	// .setIcon(android.R.drawable.ic_dialog_alert)
	// .setTitle(mContext.getString(R.string.error))
	// .setMessage(mContext.getString(R.string.rename_failed, file.getName()))
	// .show();
	// }
	// }
	// })
	// .setNegativeButton(android.R.string.cancel, new
	// DialogInterface.OnClickListener() {
	// public void onClick(DialogInterface dialog, int whichButton) {
	//
	// dialog.dismiss();
	// }
	// })
	// .show();
	// }

	// public static void doOperation(FileListEntry entry,int action,
	// MainActivity mContext, OperationCallback<Void> callback) {
	//
	// File file = entry.getPath();
	// switch (action) {
	// case R.id.menu_copy:
	// copyFile(file, mContext);
	// break;
	//
	// case R.id.menu_cut:
	// cutFile(file, mContext);
	// break;
	//
	// case R.id.menu_delete:
	// deleteFile(file, mContext, callback);
	// break;
	//
	// case R.id.menu_share:
	// share(file, mContext);
	// break;
	//
	// case R.id.menu_rename:
	// rename(file, mContext, callback);
	// break;
	//
	// case R.id.menu_zip:
	// zip(file, mContext);
	// break;
	//
	// case R.id.menu_rescan:
	// rescanMedia(mContext);
	// break;
	//
	// case R.id.menu_props:
	// showProperties(entry, mContext);
	// break;
	// default:
	// break;
	// }
	//
	// }

	// private static void rescanMedia(MainActivity mContext) {
	//
	// mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri
	// .parse("file://" + Environment.getExternalStorageDirectory())));
	//
	// Toast.makeText(mContext, R.string.media_rescan_started,
	// Toast.LENGTH_SHORT).show();
	// }
	//
	// public static void share(File file, Context mContext) {
	// final Intent intent = new Intent(Intent.ACTION_SEND);
	//
	// Uri uri = Uri.fromFile(file);
	// String type =
	// MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
	// intent.setType(type);
	// intent.setAction(Intent.ACTION_SEND);
	// intent.setType(type==null?"*/*":type);
	// intent.putExtra(Intent.EXTRA_STREAM, uri);
	//
	// mContext.startActivity(Intent.createChooser(intent,mContext.getString(R.string.share_via)));
	//
	// }

}
