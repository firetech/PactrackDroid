package nu.firetech.android.pactrack.backend;

import nu.firetech.android.pactrack.common.RefreshContext;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class ParcelService extends Service implements RefreshContext {
	private static final String TAG = "<PactrackDroid> ParcelService";

	private ParcelDbAdapter mDbAdapter;
	private StatusHandler mHandler;
	private int mMaxStatus;

	

	@Override
	public void onCreate() {
		mHandler = new StatusHandler();

		mDbAdapter = new ParcelDbAdapter(this);
		mDbAdapter.open();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		Log.d(TAG, "Automatic update initiated");
		ParcelUpdater.updateAll(this, mDbAdapter);   
	}

	@Override
	public void onDestroy() {
		mDbAdapter.close();
	}

	@Override
	public Handler getRefreshHandler() {
		return mHandler;
	}

	@Override
	public void startRefreshDialog(int maxValue, RefreshContext.Listener listener) {
		mMaxStatus = maxValue;
	}

	private class StatusHandler extends Handler {
		@Override
		public void handleMessage(Message m) {
			if (m.what == mMaxStatus) {
				ParcelService.this.stopSelf();
			}
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
