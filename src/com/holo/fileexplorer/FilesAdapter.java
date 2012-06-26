/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.holo.fileexplorer;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class implements the adapter for displaying messages based on cursors.
 */
/* package */class FilesAdapter extends ArrayAdapter<FileMeta> {
	private static final String STATE_CHECKED_ITEMS = "com.android.email.activity.MessagesAdapter.checkedItems";

	// /* package */ static final String[] MESSAGE_PROJECTION = new String[] {
	// EmailContent.RECORD_ID, MessageColumns.MAILBOX_KEY,
	// MessageColumns.ACCOUNT_KEY,
	// MessageColumns.DISPLAY_NAME, MessageColumns.SUBJECT,
	// MessageColumns.TIMESTAMP,
	// MessageColumns.FLAG_READ, MessageColumns.FLAG_FAVORITE,
	// MessageColumns.FLAG_ATTACHMENT,
	// MessageColumns.FLAGS, MessageColumns.SNIPPET
	// };

	public static final int COLUMN_ID = 0;
	public static final int COLUMN_MAILBOX_KEY = 1;
	public static final int COLUMN_ACCOUNT_KEY = 2;
	public static final int COLUMN_DISPLAY_NAME = 3;
	public static final int COLUMN_SUBJECT = 4;
	public static final int COLUMN_DATE = 5;
	public static final int COLUMN_READ = 6;
	public static final int COLUMN_FAVORITE = 7;
	public static final int COLUMN_ATTACHMENTS = 8;
	public static final int COLUMN_FLAGS = 9;
	public static final int COLUMN_SNIPPET = 10;

private Context mContext;
	private List<FileMeta> adapterItems;

	LayoutInflater mInflater;
	/**
	 * Set of seleced message IDs.
	 */
	private final HashSet<String> mSelectedSet = new HashSet<String>();

	/**
	 * Callback from MessageListAdapter. All methods are called on the UI
	 * thread.
	 */
	public interface Callback {
		/** Called when the use starts/unstars a message */
		void onAdapterFavoriteChanged(FileListItem itemView, boolean newFavorite);

		/** Called when the user selects/unselects a message */
		void onAdapterSelectedChanged(FileListItem itemView,
				boolean newSelected, int mSelectedCount);
	}
	
	private final Callback mCallback;
	
	public static class ViewHolder 
    {
      public TextView resName;
      public ImageView resIcon;
      public ImageView resActions;
      public TextView resData;
    }

	// private final Callback mCallback;

	// private ThreePaneLayout mLayout;

	public FilesAdapter(Context context, Callback callback, int textViewResourceId,
			List<FileMeta> fileListItems) {
		super(context, textViewResourceId, fileListItems);
		
		mCallback = callback;
		mContext = context;
		adapterItems = fileListItems;
		mInflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		// mCallback = callback;
	}

	public void onSaveInstanceState(Bundle outState) {
		// outState.putLongArray(STATE_CHECKED_ITEMS,
		// Utility.toPrimitiveLongArray(getSelectedSet()));
	}

	public void loadState(Bundle savedInstanceState) {
		Set<String> checkedset = getSelectedSet();
		checkedset.clear();
		for (String l : savedInstanceState.getStringArray(STATE_CHECKED_ITEMS)) {
			checkedset.add(l);
		}
		notifyDataSetChanged();
	}

	public Set<String> getSelectedSet() {
		return mSelectedSet;
	}
	
	public void setSelectedSet(Set<String> set) {
		for (String rememberedPath: set) {  
			mSelectedSet.add(rememberedPath);
		}
	}

	/**
	 * Clear the selection. It's preferable to calling {@link Set#clear()} on
	 * {@link #getSelectedSet()}, because it also notifies observers.
	 */
	public void clearSelection() {
		Set<String> checkedset = getSelectedSet();
		if (checkedset.size() > 0) {
			checkedset.clear();
			notifyDataSetChanged();
		}
	}

	public boolean isSelected(FileListItem itemView) {
		return getSelectedSet().contains(itemView.getIdentifer());
	}
	
	public void toggleSelected(FileListItem itemView) {
		updateSelected(itemView, !isSelected(itemView));
	}

	/**
	 * This is used as a callback from the list items, to set the selected state
	 * 
	 * <p>
	 * Must be called on the UI thread.
	 * 
	 * @param itemView
	 *            the item being changed
	 * @param newSelected
	 *            the new value of the selected flag (checkbox state)
	 */
	private void updateSelected(FileListItem itemView, boolean newSelected) {
		if (newSelected) {
			mSelectedSet.add(itemView.getIdentifer());
		} else {
			mSelectedSet.remove(itemView.getIdentifer());
		}
		if (mCallback != null) {
			mCallback.onAdapterSelectedChanged(itemView, newSelected,
					mSelectedSet.size());
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		FileListItem listItem;		
		
		//ViewHolder holder = null;
        if (convertView == null) 
        {
        	listItem = new FileListItem(mContext, adapterItems.get(position));
//            holder = new ViewHolder();
//            holder.resName = (TextView)convertView.findViewById(R.id.name);
//            holder.resData = (TextView)convertView.findViewById(R.id.detail);
//            holder.resIcon = (ImageView)convertView.findViewById(R.id.icon);
//            convertView.setTag(holder);
        } 
        else
        {
        	listItem = (FileListItem)convertView;
        }
        listItem.reInit(adapterItems.get(position));
        listItem.bindViewInit(this);
//        final FileListItem currentItem = items.get(position);
//        holder.resName.setText(currentItem.getName());
//        holder.resData.setText(currentItem.getData());
        //holder.resIcon.setImageDrawable(FileExplorerUtils.getIcon(mContext, currentItem.getPath()));

		return listItem;

	}


	/**
	 * This is used as a callback from the list items, to set the favorite state
	 * 
	 * <p>
	 * Must be called on the UI thread.
	 * 
	 * @param itemView
	 *            the item being changed
	 * @param newFavorite
	 *            the new value of the favorite flag (star state)
	 */
	public void updateFavorite(FileListItem itemView, boolean newFavorite) {
		changeFavoriteIcon(itemView, newFavorite);
		// if (mCallback != null) {
		// mCallback.onAdapterFavoriteChanged(itemView, newFavorite);
		// }
	}

	private void changeFavoriteIcon(FileListItem view, boolean isFavorite) {
		view.invalidate();
	}

	
}
