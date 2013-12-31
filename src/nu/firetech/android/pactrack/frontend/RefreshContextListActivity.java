package nu.firetech.android.pactrack.frontend;

import nu.firetech.android.pactrack.backend.ParcelUpdater;
import nu.firetech.android.pactrack.common.RefreshContext;
import android.app.ListActivity;
import android.os.Bundle;
import android.os.Handler;

//TODO Replace with Fragment.setRetainInstance() efter conversion to Fragments?
public abstract class RefreshContextListActivity extends ListActivity implements RefreshContext {
	private ParcelUpdater mUpdater;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		@SuppressWarnings("deprecation")
		ParcelUpdater updater = (ParcelUpdater) getLastNonConfigurationInstance();
		
		mUpdater = updater;
		if (mUpdater != null) {
			mUpdater.setContext(this);
		}
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return mUpdater;
	}
	
	@Override
	public Handler startRefreshProgress(int maxValue, ParcelUpdater updater) {
		mUpdater = updater;
		return startRefreshProgress(maxValue);
	}
	
	public abstract Handler startRefreshProgress(int maxValue);

	@Override
	public void refreshDone() {
		mUpdater = null;
		if (isChangingConfigurations()) {
			//FIXME This avoids a crash, but casues a lost update when rotating back and forth during an update.
			return;
		}
		onRefreshDone();
	}
	
	public abstract void onRefreshDone();
	
}
