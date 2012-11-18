/*
 * Android Bookmarks Importer for Tint Browser
 * 
 * Copyright (C) 2012 - to infinity and beyond J. Devauchelle and contributors.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package org.tint.androidbookmarksimporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class SyncRunnable implements Runnable {
	
	private static final Uri sUri = Uri.parse("content://com.android.browser/bookmarks");
	
	private static final String[] sProjection = new String[] { Columns.ID, Columns.TITLE, Columns.URL, Columns.TYPE, Columns.PARENT };	
	
	private Context mContext;
	private ContentResolver mContentResolver;
	
	private ISyncListener mListener;
	
	public SyncRunnable(Context context, ISyncListener listener) {
		mContext = context;
		mContentResolver = mContext.getContentResolver();
		mListener = listener;
	}

	@Override
	public void run() {
		
		publishProgress(0, 0, 0);
		
		long folderId = BookmarksWrapper.deleteImportFolderContent(mContentResolver, BookmarksWrapper.FOLDER_NAME, false);
		if (folderId == -1) {
			folderId = BookmarksWrapper.createImportFolder(mContentResolver, BookmarksWrapper.FOLDER_NAME);
		}
		
		publishProgress(1, 0, 0);
		
		Map<Long, Long> foldersMap = new HashMap<Long, Long>();
		foldersMap.put((long) 1, folderId);
		
		foldersMap = createFoldersRecursive(1, folderId, foldersMap);
		
		createBookmarks(foldersMap);
		
		publishEnd();
	}
	
	private void createBookmarks(Map<Long, Long> foldersMap) {
		Cursor c = getBookmarksCursor();
		if (c != null) {
			
			int i = 0;
			int count = c.getCount();
		
			publishProgress(2, 0, count);
			
			if (c.moveToFirst()) {
								
				List<ContentValues> valuesList = new ArrayList<ContentValues>();
				
				int parentIndex = c.getColumnIndex(Columns.PARENT);
				int titleIndex = c.getColumnIndex(Columns.TITLE);
				int urlIndex = c.getColumnIndex(Columns.URL);
				
				do {	
					
					publishProgress(2, ++i, count);
					
					long androidParentId = c.getLong(parentIndex);
					
					if (foldersMap.containsKey(androidParentId)) {
						long parentId = foldersMap.get(androidParentId);
						
						String title = c.getString(titleIndex);
						String url = c.getString(urlIndex);

						ContentValues values = new ContentValues();
						values.put(BookmarksWrapper.Columns.TITLE, title);
						values.put(BookmarksWrapper.Columns.URL, url);
						values.put(BookmarksWrapper.Columns.BOOKMARK, 1);
						values.put(BookmarksWrapper.Columns.IS_FOLDER, 0);
						values.put(BookmarksWrapper.Columns.PARENT_FOLDER_ID, parentId);

						valuesList.add(values);
					}
					
				} while (c.moveToNext());
				
				BookmarksWrapper.bulkInsert(mContentResolver, valuesList.toArray(new ContentValues[valuesList.size()]));
			}
			
			c.close();
		}
	}
	
	private Map<Long, Long> createFoldersRecursive(long androidParentId, long parentId, Map<Long, Long> foldersMap) {
		Cursor c = getFoldersCursor(androidParentId);
		
		if (c != null) {
			if (c.moveToFirst()) {
				
				int idIndex = c.getColumnIndex(Columns.ID);
				int titleIndex = c.getColumnIndex(Columns.TITLE);
				
				do {
					
					ContentValues values = new ContentValues();
					
					values.put(BookmarksWrapper.Columns.TITLE, c.getString(titleIndex));
					values.putNull(BookmarksWrapper.Columns.URL);
					values.put(BookmarksWrapper.Columns.BOOKMARK, 0);
					values.put(BookmarksWrapper.Columns.IS_FOLDER, 1);
					values.put(BookmarksWrapper.Columns.PARENT_FOLDER_ID, parentId);
					
					long insertedId = BookmarksWrapper.insert(mContentResolver, values);
					
					long androidId = c.getLong(idIndex);
					
					foldersMap.put(androidId, insertedId);
					
					if (insertedId != -1) {
						foldersMap = createFoldersRecursive(c.getLong(idIndex), insertedId, foldersMap);
					}
					
				} while (c.moveToNext());
			}
			
			c.close();
		}
		
		return foldersMap;
	}
	
	private Cursor getBookmarksCursor() {
		String whereClause = Columns.TYPE + " = " + Integer.toString(1);
		
		return mContext.getContentResolver().query(sUri, sProjection, whereClause, null, null);
	}
	
	private Cursor getFoldersCursor(long parentId) {
		String whereClause = Columns.TYPE + " = " + Integer.toString(2) + " AND " + Columns.PARENT + " = " + Long.toString(parentId);
		
		return mContext.getContentResolver().query(sUri, sProjection, whereClause, null, null);
	}
	
	private void publishProgress(Integer... values) {
		mListener.onSyncProgress(values[0], values[1], values[2]);
	}
	
	private void publishEnd() {
		mListener.onSyncEnd();
	}
	
	private static class Columns {
		public static final String ID = "_id";
		public static final String TITLE = "title";
		public static final String URL = "url";
		public static final String TYPE = "type";
		public static final String PARENT = "parent";
	}

}
