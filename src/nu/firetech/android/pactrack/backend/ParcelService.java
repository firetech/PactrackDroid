package nu.firetech.android.pactrack.backend;

import nu.firetech.android.pactrack.common.ContextListener;
import nu.firetech.android.pactrack.common.RefreshContext;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class ParcelService extends Service implements RefreshContext {
	private static final String TAG = "<PactrackDroid> ParcelService";

	private Handler mHandler;
	private ParcelDbAdapter mDbAdapter;

	@Override
	public void onCreate() {
		mHandler = new Handler();
		mDbAdapter = new ParcelDbAdapter(this);
		mDbAdapter.open();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		Log.d(TAG, "Automatic update initiated");
		ParcelUpdater.updateAll(true, this, mDbAdapter);
	}

	@Override
	public void onDestroy() {
		mDbAdapter.close();
	}

	@Override
	public Handler getProgressHandler() {
		return mHandler;
	}

	@Override
	public void startRefreshProgress(int maxValue, ContextListener listener) {
		Log.d(TAG, "Automatic update running");
	}

	@Override
	public void refreshDone() {
		stopSelf();	
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
