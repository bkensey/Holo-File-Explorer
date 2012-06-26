package com.holo.fileexplorer;

import java.io.File;
import java.util.Date;

import org.apache.commons.io.FileUtils;

import android.content.Context;
import android.support.v4.app.FragmentActivity;

public class FileMeta {

	public String mName = "";
	public String mLowerCaseName = "";
	public String mDetail = "";
	public String mPath = "";
	public String mExtension = "";
	public String mFullName = "";
	public Date mModifiedDate;
	public boolean isDirectory;
	
	public FileMeta(Context context, String permissions, String colTwo, String colThree, String numBytes, String modifedDate, String modifedTime, String fileName, String parentPath) {
		this.setName(fileName);
		this.setLowerCaseName(fileName.toLowerCase());
		this.setPath(parentPath + '/' + fileName);
		this.setModifiedDate(new Date());
		
		if (numBytes == null) {
			// Mark as a directory so that directories and folders can be
			// handled by the app.
			isDirectory = true;
			if (true/*!file.canRead() && !file.canWrite()*/) {
				// Create string for file detail in this format: 1 file/n files
				this.setDetail("protected folder");
			} else {
				// Grab number of files in directory
//				int numFiles = file.list().length;
//				// Create string for file detail in this format: 1 file/n files
//				this.setDetail(numFiles + " file" + (numFiles != 1 ? "s" : ""));
			}
		} else {
			this.setDetail(FileUtils.byteCountToDisplaySize(Long.parseLong(numBytes)) + "");
			isDirectory = false;
		}

		if (mName == null || mPath == null)
			throw new NullPointerException();
	}
	
	public FileMeta(Context context, File file) {
		this.setName(file.getName());
		this.setLowerCaseName(file.getName().toLowerCase());
		this.setPath(file.getAbsolutePath());
		this.setModifiedDate(new Date(file.lastModified()));

		if (file.isDirectory()) {
			// Mark as a directory so that directories and folders can be
			// handled by the app.
			isDirectory = true;
			if (!file.canRead() && !file.canWrite()) {
				// Create string for file detail in this format: 1 file/n files
				this.setDetail("protected folder");
			} else {
				// Grab number of files in directory
				int numFiles = file.list().length;
				// Create string for file detail in this format: 1 file/n files
				this.setDetail(numFiles + " file" + (numFiles != 1 ? "s" : ""));
			}
		} else {
			this.setDetail(FileUtils.byteCountToDisplaySize(file.length()) + "");
			isDirectory = false;
		}

		if (mName == null || mPath == null)
			throw new NullPointerException();
	}

	public void setName(String mName) {
		this.mName = mName;
	}
	
	private void setLowerCaseName(String mLowerCaseName) {
		this.mLowerCaseName = mLowerCaseName;
	}

	public void setDetail(String mDetail) {
		this.mDetail = mDetail;
	}

	public void setPath(String mPath) {
		this.mPath = mPath;
	}

	public void setExtension(String mExtension) {
		this.mExtension = mExtension;
	}

	public void setFullName(String mFullName) {
		this.mFullName = mFullName;
	}

	public void setModifiedDate(Date mModifiedDate) {
		this.mModifiedDate = mModifiedDate;
	}

	// Comparators now implemented in FileListFragment so as to enable multiple
	// sort schemes
	// @Override
	// public int compareTo(FileMeta f) {
	// if (this.mName != null)
	// return this.mName
	// .compareTo(f.mName);
	// else
	// throw new IllegalArgumentException();
	//
	// }
}
