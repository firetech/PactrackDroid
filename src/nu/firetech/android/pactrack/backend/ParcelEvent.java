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

import java.util.HashMap;

public class ParcelEvent {
	private String mLocation;
	private String mDescription;
	private String mTime;
	private boolean mError = false;
	
	ParcelEvent(HashMap<String,Object> eventData) {
		mLocation = (String)eventData.get("location");
		mDescription = (String)eventData.get("description");
		
		String date = (String)eventData.get("date");
		String time = (String)eventData.get("time");
		mTime = new StringBuilder(date.substring(0, 4))
			.append('-')
			.append(date.substring(4, 6))
			.append('-')
			.append(date.substring(6, 8))
    		.append(' ')
    		.append(time.substring(0, 2))
    		.append(':')
    		.append(time.substring(2, 4))
			.toString();
		
		if (eventData.containsKey("errorevent") &&
				eventData.get("errorevent").equals("true")) {
			mError = true;
		}
	}
	
////////////////////////////////////////////////////////////////////////////////

	public String getLocation() {
		return mLocation;
	}

	public String getDescription() {
		return mDescription;
	}

	public String getTime() {
		return mTime;
	}
	
	public boolean isError() {
		return mError;
	}
	
	public String toString() {
		return mTime + ": " + mLocation + " - " + mDescription;
	}
}
