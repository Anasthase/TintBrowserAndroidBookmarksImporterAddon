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
import java.util.List;
import java.util.Random;

import org.tint.addons.framework.Action;
import org.tint.addons.framework.Callbacks;
import org.tint.addons.framework.ShowDialogAction;
import org.tint.androidbookmarksimporter.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.os.RemoteException;

public class Addon extends BaseAddon implements ISyncListener {
	
	private int mNotificationId;
	private Notification mNotification;
	private NotificationManager mNotificationManager;
	
	private int mLastProgress;
	
	private SyncRunnable mSyncRunnable = null;

	public Addon(Service service) {
		super(service);
	}

	@Override
	public int getCallbacks() throws RemoteException {
		return Callbacks.CONTRIBUTE_HISTORY_BOOKMARKS_MENU;
	}

	@Override
	public String getContributedBookmarkContextMenuItem(String currentTabId) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContributedHistoryBookmarksMenuItem(String currentTabId) throws RemoteException {
		return mService.getString(R.string.MenuTitle);
	}

	@Override
	public String getContributedHistoryContextMenuItem(String currentTabId) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContributedLinkContextMenuItem(String currentTabId, int hitTestResult, String url) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContributedMainMenuItem(String currentTabId, String currentTitle, String currentUrl) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onBind() throws RemoteException {
		mNotificationManager = (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);		
	}

	@Override
	public List<Action> onContributedBookmarkContextMenuItemSelected(String currentTabId, String title, String url) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Action> onContributedHistoryBookmarksMenuItemSelected(String currentTabId) throws RemoteException {
		List<Action> result = null;
		
		if (mSyncRunnable == null) {
			startSync();
		} else {
			result = new ArrayList<Action>();
			result.add(new ShowDialogAction(
					mService.getString(R.string.ImportInProgressTitle),
					mService.getString(R.string.ImportInProgressMessage)));
		}
		
		return result;
	}

	@Override
	public List<Action> onContributedHistoryContextMenuItemSelected(String currentTabId, String title, String url) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Action> onContributedLinkContextMenuItemSelected(String currentTabId, int hitTestResult, String url) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Action> onContributedMainMenuItemSelected(String currentTabId, String currentTitle, String currentUrl) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Action> onPageFinished(String tabId, String url) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Action> onPageStarted(String tabId, String url) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<Action> onTabClosed(String tabId) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Action> onTabOpened(String tabId) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<Action> onTabSwitched(String tabId) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onUnbind() throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Action> onUserConfirm(String currentTabId, int questionId, boolean positiveAnswer) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<Action> onUserInput(String currentTabId, int questionId, boolean cancelled, String userInput) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<Action> onUserChoice(String currentTabId, int questionId, boolean cancelled, int userChoice) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void showAddonSettingsActivity() throws RemoteException {
		// TODO Auto-generated method stub
		
	}
	
	private void startSync() {
		mSyncRunnable = new SyncRunnable(mService, this);
		
		new Thread(mSyncRunnable).start();
	}

	@Override
	public void onSyncProgress(int step, int done, int total) {
		switch (step) {
		case 0:
			mLastProgress = -1;
			
			Random r = new Random();			
			mNotificationId = r.nextInt();
			
			mNotification = new Notification(R.drawable.ic_stat_sync, mService.getString(R.string.NotificationImportStartTickerText), System.currentTimeMillis());
			mNotification.flags |= Notification.FLAG_NO_CLEAR;
			
			mNotification.setLatestEventInfo(
					mService,
					mService.getString(R.string.NotificationImportTitle),
					mService.getString(R.string.NotificationImportMessagePreparingImport),
					null);
			
			mNotificationManager.notify(mNotificationId, mNotification);		
			
			break;

		case 1:
			mNotification.setLatestEventInfo(
					mService,
					mService.getString(R.string.NotificationImportTitle),
					mService.getString(R.string.NotificationImportMessageCreatingFolders),
					null);
			
			mNotificationManager.notify(mNotificationId, mNotification);
			
			break;
			
		case 2:
			int progress = (int) (((float) done / total) * 100);
			
			if (progress > mLastProgress) {
				mNotification.setLatestEventInfo(
						mService,
						mService.getString(R.string.NotificationImportTitle),
						String.format(mService.getString(R.string.NotificationImportMessageCreatingBookmarks), progress),
						null);
				
				mNotificationManager.notify(mNotificationId, mNotification);
				
				mLastProgress = progress;
			}
			
			break;
			
		default:
			break;
		}
		
	}

	@Override
	public void onSyncEnd() {
		mSyncRunnable = null;
		
		mNotificationManager.cancel(mNotificationId);
		
		Random r = new Random();
		mNotificationId = r.nextInt();
		
		mNotification = new Notification(R.drawable.ic_stat_sync, mService.getString(R.string.NotificationImportEndTickerText), System.currentTimeMillis());
		mNotification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		mNotification.setLatestEventInfo(
				mService,
				mService.getString(R.string.NotificationImportTitle),
				mService.getString(R.string.NotificationImportEndMessageOk),
				null);
		
		mNotificationManager.notify(mNotificationId, mNotification);
	}

}
