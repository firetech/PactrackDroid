/*
 * Copyright (C) 2013 Joakim Andersson
 * Copyright (C) 2013 blunden
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
	private int mStatusCode = -1;
	
	private ArrayList<ParcelEvent> mEvents = new ArrayList<ParcelEvent>();
	
	Parcel(int errorCode) {
		mErrorCode = errorCode;
	}

	@SuppressWarnings("unchecked")
	Parcel(HashMap<String, Object> data) {
		mParcel = (String)data.get("parcel");
		
		mCustomer = (String)data.get("customername");
		mPostal = new StringBuilder((String)data.get("receiverzipcode"))
				.append(' ')
				.append((String)data.get("receivercity"))
				.toString();
		mService = (String)data.get("servicename");
		mStatus = (String)data.get("statusdescription");
		
		try {
			mWeight = Double.parseDouble((String)data.get("actualweight"));
		} catch (Exception e) {
			mWeight = 0;
		}
		
		try {
			mStatusCode = Integer.parseInt((String)data.get("statuscode"));
		} catch (Exception e) {
			mStatusCode = -1;
		}
		
		String sent = (String)data.get("datesent");
		if (sent != null) {
			mSent = new StringBuilder(sent.substring(0, 4))
				.append('-')
				.append(sent.substring(4, 6))
				.append('-')
				.append(sent.substring(6, 8))
				.toString();
		} else {
			mSent = "";
		}
		
		mEvents.addAll((ArrayList<ParcelEvent>)data.get("events"));
		
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

