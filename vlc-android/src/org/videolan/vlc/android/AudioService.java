package org.videolan.vlc.android;

import java.util.ArrayList;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

public class AudioService extends Service {
	private static final String TAG = "VLC/AudioService";
	
	
	private LibVLC mLibVLC;
	private Media mMedia;
    private boolean mEndReached = false;
	private ArrayList<IAudioServiceCallback> mCallback;
	private EventManager mEventManager;

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		// Get libVLC instance
        try {
			mLibVLC = LibVLC.getInstance();
		} catch (LibVlcException e) {
			e.printStackTrace();
		}		
		
		mCallback = new ArrayList<IAudioServiceCallback>();
		mEventManager = new EventManager(eventHandler);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mInterface;
	}

	
    /**
     *  Handle libvlc asynchronous events 
     */
    private Handler eventHandler = new Handler() {

		@Override
        public void handleMessage(Message msg) {
            switch (msg.getData().getInt("event")) {
                case EventManager.MediaPlayerPlaying:
                    Log.e(TAG, "MediaPlayerPlaying");
                    executeUpdate();
                    break;
                case EventManager.MediaPlayerPaused:
                    Log.e(TAG, "MediaPlayerPaused");
                    executeUpdate();
                    break;
                case EventManager.MediaPlayerStopped:
                    Log.e(TAG, "MediaPlayerStopped");
                    executeUpdate();
                    break;
                case EventManager.MediaPlayerEndReached:
                    Log.e(TAG, "MediaPlayerEndReached");
                    executeUpdate();
                    hideNotification();
                    mEndReached = true;
                    // TODO: play next song
                    break;
                default:
                    Log.e(TAG, "Event not handled");
                    break;
            }
        }
    };
    
    private void executeUpdate() {
    	for (int i = 0; i < mCallback.size(); i++) {
    		try {
				mCallback.get(i).update();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
    	}
    }
    
    private void showNotification() {
		// add notification to status bar
		Notification notification = new Notification(R.drawable.icon, null,
		        System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, MainActivity.class);
		notificationIntent.putExtra(MainActivity.START_FROM_NOTIFICATION, "Now Playing...");
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this, mMedia.getName(),
		        "## Artist ##", pendingIntent);
		startForeground(3, notification);
    }
    
    private void hideNotification() {
    	stopForeground(true);
    }
    
    private IAudioService.Stub mInterface = new IAudioService.Stub() {
		
    	@Override
    	public String getCurrentMediaPath() throws RemoteException {
    		return mMedia.getPath();
    	}

    	@Override
    	public void pause() throws RemoteException {
    		hideNotification();
    		mLibVLC.pause();
    	}

    	@Override
    	public void play() throws RemoteException {
    		if (mEndReached && mMedia != null) {
    			mLibVLC.readMedia(mMedia.getPath());
    			mEndReached = false;
    		} else {
    			mLibVLC.play();
    			
    		}
    		showNotification();
    	}

    	@Override
    	public void stop() throws RemoteException {	
    		mLibVLC.stop();
    		mMedia = null;
    		hideNotification();
    		executeUpdate();
    	}

    	@Override
    	public void load(String mediaPath) throws RemoteException {
    		// reset EventManager because it could be replaced by
    		// the manager from the video player
    		mLibVLC.setEventManager(mEventManager); 
    		
    		DatabaseManager db = DatabaseManager.getInstance();
    		mMedia = db.getMediaItem(mediaPath);
    		if (mLibVLC.isPlaying()) {
    			mLibVLC.stop();
    		}
    		mLibVLC.readMedia(mediaPath);
    		showNotification();
    		executeUpdate();
    	}


    	@Override
    	public boolean isPlaying() throws RemoteException {
    		return mLibVLC.isPlaying();
    	}

		@Override
		public boolean hasMedia() throws RemoteException {
			return mMedia != null;
		}

		@Override
		public String getArtist() throws RemoteException {
			// TODO: add media parameter
			return null;
		}

		@Override
		public String getTitle() throws RemoteException {
			if (mMedia != null)
				return mMedia.getName();
			else
				return null;
		}

		@Override
		public void addAudioCallback(IAudioServiceCallback cb)
				throws RemoteException {
			mCallback.add(cb);
			executeUpdate();
		}

		@Override
		public void removeAudioCallback(IAudioServiceCallback cb)
				throws RemoteException {
			if (mCallback.contains(cb)){
				mCallback.remove(cb);
			}
		}
	};
	
	
    




}