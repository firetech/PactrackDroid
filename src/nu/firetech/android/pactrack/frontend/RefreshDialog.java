package nu.firetech.android.pactrack.frontend;

import nu.firetech.android.pactrack.R;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class RefreshDialog extends ProgressDialog implements DialogAwareListActivity.Dialog {
	private static final String KEY_MAX_VALUE = "max_value";
	private static final String KEY_VALUE = "value";

	private int mMaxValue;
	private ProgressHandler mHandler;
	
	public static void show(final DialogAwareListActivity context, int maxValue) {
		RefreshDialog d = new RefreshDialog(context, 0, maxValue);
		d.show();
	}
	
	public static void show(final DialogAwareListActivity context, Bundle dialogData) {
		RefreshDialog d = new RefreshDialog(context, dialogData.getInt(KEY_VALUE), dialogData.getInt(KEY_MAX_VALUE));
		d.show();
	}

	private RefreshDialog(final DialogAwareListActivity context, int initialValue, int maxValue) {
		super(context);
		
		setProgress(initialValue);
		mMaxValue = maxValue;
		mHandler = new ProgressHandler();

		context.addDialog(this);
		setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				context.removeDialog(RefreshDialog.this);
			}
		});
		
        setTitle(R.string.refreshing);
    	setMax(mMaxValue);
        if (mMaxValue > 1) {
        	setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        	setMessage(null);
        } else {
        	setProgressStyle(ProgressDialog.STYLE_SPINNER);
    	    setMessage(context.getString(R.string.refreshing));
        }
	}

	@Override
	public Bundle getInstanceState() {
		Bundle outState = new Bundle(2);
		outState.putInt(KEY_VALUE, getProgress());
		outState.putInt(KEY_MAX_VALUE, mMaxValue);
		return outState;
	}
	
	public Handler getProgressHandler() {
		return mHandler;
	}

	@Override
	public void onContextChange(Context newContext) {}

	@Override
	public void onContextDestroy(Context oldContext) {
		dismiss();
	}
	
	private class ProgressHandler extends Handler {
		@Override
        public void handleMessage(Message m) {
			if (m.what == mMaxValue) {
				dismiss();
			} else {
				setProgress(m.what);
		    }
        }
	};
}
