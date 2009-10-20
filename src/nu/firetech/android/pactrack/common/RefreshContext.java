package nu.firetech.android.pactrack.common;

import android.os.Handler;

public interface RefreshContext {
	public Handler getProgressHandler();
	public void startRefreshProgress(int maxValue, ContextListener listener);
	public void refreshDone();
}
