/*
 * Copyright (C) 2009 Joakim Andersson
 * 
 * This file is part of PactrackDroid, an Android application to keep
 * track of parcels sent with the Swedish mail service (Posten).
 * 
 * PactrackDroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * PactrackDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package nu.firetech.android.pactrack.frontend;

import java.lang.reflect.Method;

import nu.firetech.android.pactrack.R;
import android.content.Intent;
import android.os.Bundle;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public abstract class BarcodeListeningListActivity extends DialogAwareListActivity {
	private BarcodeListener mBarcodeListener;
	
	public void setBarcodeListener(BarcodeListener listener) {
		mBarcodeListener = listener;
	}
	
	public void initiateScan(BarcodeListener listener) {
		setBarcodeListener(listener);
		IntentIntegrator.initiateScan(this, R.string.install_barcode_title, R.string.install_barcode_message, R.string.yes, R.string.no);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (mBarcodeListener != null) {
			IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
			if (scanResult != null) {
				mBarcodeListener.handleBarcode(scanResult);
			}
			mBarcodeListener = null;
		}
	}
	
	@Override
	protected Method getShowMethodForClass(Class<?> c) throws NoSuchMethodException {
		try {
			return c.getMethod("show", BarcodeListeningListActivity.class, Bundle.class);
		} catch (NoSuchMethodException e) {
			//Fall back to original dialog type.
			return c.getMethod("show", DialogAwareListActivity.class, Bundle.class);	
		}
	}
}
