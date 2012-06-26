/*
 * Copyright (C) 2011 The Android Open Source Project
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

import com.holo.fileexplorer.R;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextPaint;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

/**
 * Represents the coordinates of elements inside a CanvasConversationHeaderView
 * (eg, checkmark, star, detail, sender, labels, etc.) It will inflate a view,
 * and record the coordinates of each element after layout. This will allows us
 * to easily improve performance by creating custom view while still defining
 * layout in XML files.
 */
public class FileListItemCoordinates {
	// Modes.
	public static final int WIDE_MODE = 0;
	public static final int NORMAL_MODE = 1;

	// Static threshold.
	private static int MINIMUM_WIDTH_WIDE_MODE = -1;
	private static int[] SUBJECT_LENGTHS;

	// Checkmark.
	int iconX;
	int iconY;
	int iconWidthIncludingMargins;

	// Checkmark.
	int checkmarkX;
	int checkmarkY;
	int checkmarkWidthIncludingMargins;

	// Star.
	int starX;
	int starY;

	// Senders.
	int nameX;
	int nameY;
	int nameWidth;
	int nameLineCount;
	int nameFontSize;
	int nameAscent;

	// Subject.
	int detailX;
	int detailY;
	int detailWidth;
	int detailLineCount;
	int detailFontSize;
	int detailAscent;

	// Color chip.
	int chipX;
	int chipY;
	int chipWidth;
	int chipHeight;

	// Date.
	int dateXEnd;
	int dateY;
	int dateFontSize;
	int dateAscent;

	// Paperclip.
	int paperclipY;

	// Cache to save Coordinates based on view width.
	private static SparseArray<FileListItemCoordinates> mCache = new SparseArray<FileListItemCoordinates>();

	private static TextPaint sPaint = new TextPaint();

	static {
		sPaint.setTypeface(Typeface.DEFAULT);
		sPaint.setAntiAlias(true);
	}

	// Not directly instantiable.
	private FileListItemCoordinates() {
	}

	/**
	 * Returns the mode of the header view (Wide/Normal/Narrow) given the its
	 * measured width.
	 */
	public static int getMode(Context context, int width) {
		Resources res = context.getResources();
		if (MINIMUM_WIDTH_WIDE_MODE <= 0) {
			MINIMUM_WIDTH_WIDE_MODE = res.getDimensionPixelSize(R.dimen.minimum_width_wide_mode);
		}

		// Choose the correct mode based on view width.
		int mode = NORMAL_MODE;
		if (width > MINIMUM_WIDTH_WIDE_MODE) {
			mode = WIDE_MODE;
		}
		return mode;
	}

	public static boolean isMultiPane(Context context) {
		return false;// UiUtilities.useTwoPane(context);
	}

	/**
	 * Returns the layout id to be inflated in this mode.
	 */
	private static int getLayoutId(int mode) {
		switch (mode) {
		// case WIDE_MODE:
		// return R.layout.file_list_item_wide;
		case NORMAL_MODE:
			return R.layout.file_list_item_normal;
		default:
			throw new IllegalArgumentException("Unknown conversation header view mode " + mode);
		}
	}

	/**
	 * Returns a value array multiplied by the specified density.
	 */
	public static int[] getDensityDependentArray(int[] values, float density) {
		int result[] = new int[values.length];
		for (int i = 0; i < values.length; ++i) {
			result[i] = (int) (values[i] * density);
		}
		return result;
	}

	/**
	 * Returns the height of the view in this mode.
	 */
	public static int getHeight(Context context, int mode) {
		return context.getResources().getDimensionPixelSize(
				(mode == WIDE_MODE) ? R.dimen.message_list_item_height_wide : R.dimen.message_list_item_height_normal);
	}

	/**
	 * Returns the x coordinates of a view by tracing up its hierarchy.
	 */
	private static int getX(View view) {
		int x = 0;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			int[] xy = new int[2];
			view.getLocationOnScreen(xy);
			x=xy[0];
		} else {
			while (view != null) {
				x += (int) view.getX();
				ViewParent parent = view.getParent();
				view = parent != null ? (View) parent : null;
			}
		}

		return x;
	}

	/**
	 * Returns the y coordinates of a view by tracing up its hierarchy.
	 */
	private static int getY(View view) {
		int y = 0;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			int[] xy = new int[2];
			view.getLocationOnScreen(xy);
			y=xy[1];
		} else {
			while (view != null) {
				y += (int) view.getY();
				ViewParent parent = view.getParent();
				view = parent != null ? (View) parent : null;
			}
		}
		return y;
	}

	/**
	 * Returns the width of a view.
	 * 
	 * @param includeMargins
	 *            whether or not to include margins when calculating width.
	 */
	public static int getWidth(View view, boolean includeMargins) {
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
		return view.getWidth() + (includeMargins ? params.leftMargin + params.rightMargin : 0);
	}

	/**
	 * Returns the height of a view.
	 * 
	 * @param includeMargins
	 *            whether or not to include margins when calculating height.
	 */
	public static int getHeight(View view, boolean includeMargins) {
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
		return view.getHeight() + (includeMargins ? params.topMargin + params.bottomMargin : 0);
	}

	/**
	 * Returns the number of lines of this text view.
	 */
	private static int getLineCount(TextView textView) {
		return textView.getHeight() / textView.getLineHeight();
	}

	/**
	 * Returns the length (maximum of characters) of detail in this mode.
	 */
	public static int getSubjectLength(Context context, int mode) {
		Resources res = context.getResources();
		if (SUBJECT_LENGTHS == null) {
			SUBJECT_LENGTHS = res.getIntArray(R.array.detail_lengths);
		}
		return SUBJECT_LENGTHS[mode];
	}

	/**
	 * Reset the caches associated with the coordinate layouts.
	 */
	static void resetCaches() {
		mCache.clear();
	}

	/**
	 * Returns coordinates for elements inside a conversation header view given
	 * the view width.
	 */
	public static FileListItemCoordinates forWidth(Context context, int width) {
		FileListItemCoordinates coordinates = mCache.get(width);
		if (coordinates == null) {
			coordinates = new FileListItemCoordinates();
			mCache.put(width, coordinates);
			// TODO: make the field computation done inside of the constructor
			// and mark fields final

			// Layout the appropriate view.
			int mode = getMode(context, width);
			int height = getHeight(context, mode);
			View view = LayoutInflater.from(context).inflate(getLayoutId(mode), null);
			int widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
			int heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
			view.measure(widthSpec, heightSpec);
			view.layout(0, 0, width, height);

			// Records coordinates.
			View icon = view.findViewById(R.id.icon);
			coordinates.iconX = getX(icon);
			coordinates.iconY = getY(icon);
			coordinates.iconWidthIncludingMargins = getWidth(icon, true);

			View checkmark = view.findViewById(R.id.checkmark);
			coordinates.checkmarkX = getX(checkmark);
			coordinates.checkmarkY = getY(checkmark);
			coordinates.checkmarkWidthIncludingMargins = getWidth(checkmark, true);

			TextView name = (TextView) view.findViewById(R.id.name);
			coordinates.nameX = getX(name);
			coordinates.nameY = getY(name);
			coordinates.nameWidth = getWidth(name, false);
			coordinates.nameLineCount = getLineCount(name);
			coordinates.nameFontSize = (int) name.getTextSize();
			coordinates.nameAscent = Math.round(name.getPaint().ascent());

			TextView detail = (TextView) view.findViewById(R.id.detail);
			coordinates.detailX = getX(detail);
			coordinates.detailY = getY(detail);
			coordinates.detailWidth = getWidth(detail, false);
			coordinates.detailLineCount = getLineCount(detail);
			coordinates.detailFontSize = (int) detail.getTextSize();
			coordinates.detailAscent = Math.round(detail.getPaint().ascent());

			View chip = view.findViewById(R.id.color_chip);
			coordinates.chipX = getX(chip);
			coordinates.chipY = getY(chip);
			coordinates.chipWidth = getWidth(chip, false);
			coordinates.chipHeight = getHeight(chip, false);

			// TextView date = (TextView) view.findViewById(R.id.date);
			// coordinates.dateXEnd = getX(date) + date.getWidth();
			// coordinates.dateY = getY(date);
			// coordinates.dateFontSize = (int) date.getTextSize();
			// coordinates.dateAscent = Math.round(date.getPaint().ascent());
			//
			// // The x-value is computed relative to the date.
			// View paperclip = view.findViewById(R.id.paperclip);
			// coordinates.paperclipY = getY(paperclip);
		}
		return coordinates;
	}
}
