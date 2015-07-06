/*
 * Copyright (C) 2014 Joakim Andersson
 * Copyright (C) 2014 blunden
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

package nu.firetech.android.pactrack.backend;

import java.util.ArrayList;
import java.util.HashMap;

import nu.firetech.android.pactrack.common.Error;

class Parcel {
	private int mErrorCode;

	private String mParcel;
	private String mCustomer;
	private String mSent;
	private double mWeight;
	private String mPostal;
	private String mService;
	private String mStatus;
	private int mStatusCode = ParcelDbAdapter.STATUS_UNKNOWN;
	
	private ArrayList<ParcelEvent> mEvents = new ArrayList<ParcelEvent>();
	
	Parcel(int errorCode) {
		mErrorCode = errorCode;
	}

	@SuppressWarnings("unchecked")
	Parcel(HashMap<String, Object> data) {
		mParcel = (String)data.get(ParcelDbAdapter.KEY_PARCEL);
		
		mCustomer = (String)data.get(ParcelDbAdapter.KEY_CUSTOMER);
		mPostal = (String)data.get(ParcelDbAdapter.KEY_POSTAL);
		mService = (String)data.get(ParcelDbAdapter.KEY_SERVICE);
		mStatus = (String)data.get(ParcelDbAdapter.KEY_STATUS);
		
		try {
			mWeight = Double.parseDouble((String)data.get(ParcelDbAdapter.KEY_WEIGHT));
			if ("g".equals((String)data.get(ParcelJsonParser.KEY_WEIGHT_UNIT))) {
				mWeight /= 1000;
			}
		} catch (Exception e) {
			mWeight = 0;
		}
		
		String statusCode = (String)data.get(ParcelDbAdapter.KEY_STATUSCODE);
		if (statusCode != null) {
			if ("INFORMED".equals(statusCode)) {
				mStatusCode = ParcelDbAdapter.STATUS_PREINFO;
			} else if ("EN_ROUTE".equals(statusCode)) {
				mStatusCode = ParcelDbAdapter.STATUS_ENROUTE;
			} else if ("AVAILABLE_FOR_DELIVERY".equals(statusCode)) {
				mStatusCode = ParcelDbAdapter.STATUS_COLLECTABLE;
			} else if ("DELIVERED".equals(statusCode)) {
				mStatusCode = ParcelDbAdapter.STATUS_DELIVERED;
			}
		}
		
		String sent = (String)data.get(ParcelDbAdapter.KEY_SENT);
		if (sent != null) {
			mSent = sent.replace('T', ' ');
		} else {
			mSent = "";
		}
		
		for (HashMap<String,Object> eventData : (ArrayList<HashMap<String,Object>>)data.get(ParcelJsonParser.KEY_EVENTS)) {
			mEvents.add(new ParcelEvent(eventData));
		}
		
		mErrorCode = Error.NONE;
	}
	
////////////////////////////////////////////////////////////////////////////////
	
	public int getError() {
		return mErrorCode;
	}

	public String getParcel() {
		return mParcel;
	}

	public String getCustomer() {
		return mCustomer;
	}

	public String getSent() {
		return mSent;
	}

	public double getWeight() {
		return mWeight;
	}

	public String getPostal() {
		return mPostal;
	}

	public String getService() {
		return mService;
	}

	public String getStatus() {
		return mStatus;
	}

	public int getStatusCode() {
		return mStatusCode;
	}
	
	public ParcelEvent[] getEvents() {
		return mEvents.toArray(new ParcelEvent[0]);
	}
}

