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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class BookmarksWrapper {
	
	public static final String FOLDER_NAME = "Imported from Android";
	
	public static final String AUTHORITY = "org.tint.providers.bookmarksprovider";
	public static final Uri BOOKMARKS_URI = Uri.parse("content://org.tint.providers.bookmarksprovider/bookmarks");
	
	public static String[] HISTORY_BOOKMARKS_PROJECTION = new String[] {
		Columns._ID,
        Columns.TITLE,
        Columns.URL,
        Columns.VISITS,
        Columns.CREATION_DATE,
        Columns.VISITED_DATE,
        Columns.BOOKMARK,
        Columns.IS_FOLDER,
        Columns.PARENT_FOLDER_ID,
        Columns.FAVICON,
        Columns.THUMBNAIL };
	
	public static class Columns {
		public static final String _ID = "_id";
		public static final String TITLE = "title";
		public static final String URL = "url";
		public static final String CREATION_DATE = "creation_date";
		public static final String VISITED_DATE = "visited_date";
		public static final String VISITS = "visits";
		public static final String BOOKMARK = "bookmark";
		public static final String IS_FOLDER = "is_folder";
		public static final String PARENT_FOLDER_ID = "parent_folder_id";
		public static final String FAVICON = "favicon";
		public static final String THUMBNAIL = "thumbnail";
	}
	
	public static long insert(ContentResolver contentResolver, ContentValues values) {
		Uri result = contentResolver.insert(BOOKMARKS_URI, values);
		if (result != null) {
			return ContentUris.parseId(result);
		} else {
			return -1;
		}
	}
	
	public static void bulkInsert(ContentResolver contentResolver, ContentValues[] valuesArray) {
		contentResolver.bulkInsert(BOOKMARKS_URI, valuesArray);
	}
	
	public static long createImportFolder(ContentResolver contentResolver, String folderName) {
		
		Log.d("BookmarksWrapper", "createImportFolder()");
		
		ContentValues values = new ContentValues();
		values.put(Columns.TITLE, folderName);
		values.put(Columns.IS_FOLDER, 1);
		
		contentResolver.insert(BOOKMARKS_URI, values);
		
		return getFolderId(contentResolver, folderName);
	}
	
	public static long deleteImportFolderContent(ContentResolver contentResolver, String folderName, boolean deleteFolder) {	
		long folderId = getFolderId(contentResolver, folderName);
		if (folderId != -1) {
			doDeleteImportFolder(contentResolver, folderId, deleteFolder);
		}
		
		Log.d("BookmarksWrapper", "deleteImportFolderContent()" + Long.toString(folderId));
		
		return folderId;
	}
	
	public static long getFolderId(ContentResolver contentResolver, String folderName) {
		String whereClause = Columns.IS_FOLDER + " > 0 AND " + Columns.TITLE + " = \"" + folderName + "\"";
		
		Cursor c = contentResolver.query(BOOKMARKS_URI, HISTORY_BOOKMARKS_PROJECTION, whereClause, null, null);
		if ((c != null) &&
				(c.moveToFirst())) {
			return c.getLong(c.getColumnIndex(Columns._ID));
		}
		
		return -1;
	}
	
	private static void doDeleteImportFolder(ContentResolver contentResolver, long folderId, boolean deleteFolder) {
		Cursor c = getChildrenFolders(contentResolver, folderId);
		if ((c != null) &&
				(c.moveToFirst())) {
			do {
				
				long childId = c.getLong(c.getColumnIndex(Columns._ID));
				doDeleteImportFolder(contentResolver, childId, true);
				
			} while (c.moveToNext());
		}
		
		// Delete content
		String whereClause = Columns.PARENT_FOLDER_ID + " = " + folderId;
		contentResolver.delete(BOOKMARKS_URI, whereClause, null);
		
		if (deleteFolder) {
			whereClause = Columns._ID + " = " + folderId;
			contentResolver.delete(BOOKMARKS_URI, whereClause, null);
		}
	}
	
	private static Cursor getChildrenFolders(ContentResolver contentResolver, long folderId) {
		String whereClause = Columns.IS_FOLDER + " > 0 AND " + Columns.PARENT_FOLDER_ID + " = " + folderId;
		return contentResolver.query(BOOKMARKS_URI, HISTORY_BOOKMARKS_PROJECTION, whereClause, null, null);
	}

}
