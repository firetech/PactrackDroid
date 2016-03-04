/*
 * Copyright (C) 2016 Joakim Tufvegren
 * Copyright (C) 2016 blunden
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import nu.firetech.android.pactrack.R;
import nu.firetech.android.pactrack.common.Error;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

/* 
 * You can get your own apiKey at http://developer.postnord.com
 */

public class ParcelJsonParser {
	private static final String TAG = "<PactrackDroid> ParcelXMLParser";
	
	//TODO support more languages
	private static final String BASE_URL = 
			"https://api2.postnord.com/rest/shipment/v1/trackandtrace/findByIdentifier.json?id=%s&locale=sv&apikey=%s";

	public static final String KEY_EVENTS = "events";
	public static final String KEY_WEIGHT_UNIT = "weight_unit";
	
	private static String apiKey = null;
	
	public static void loadApiKey(Context ctx) {
		if (apiKey == null) {
			apiKey = ctx.getString(R.string.postnord_apikey);
		}
	}

	public static Parcel fetch(String parcelId) {
		try {
			if (apiKey == null) {
				throw new IllegalStateException("No apiKey loaded.");
			}

			ParcelJsonParser parser = new ParcelJsonParser();
			
			return parser.parse(parcelId);
		} catch (Exception e) {
			Log.d(TAG, "SERVER error", e);
			return new Parcel(Error.SERVER);
		}
	}

	////////////////////////////////////////////////////////////////////////////////

	private ParcelJsonParser() {}
	
	private Parcel parse(String parcelId) throws Exception {
		HashMap<String,Object> data = new HashMap<String,Object>();
		
		StringBuilder jsonText = new StringBuilder();
		
		URL parcelUrl = new URL(String.format(BASE_URL, parcelId, apiKey));
		
		BufferedReader in = new BufferedReader(new InputStreamReader(parcelUrl.openStream(), "UTF-8"));
		for (String line = in.readLine(); line != null; line = in.readLine()) {
			jsonText.append(line);
		}
		in.close();
		
		JSONObject json = new JSONObject(jsonText.toString()).getJSONObject("TrackingInformationResponse");
		int error = checkError(json);
		if (error != Error.NONE) {
			return new Parcel(error);
		}
		JSONObject shipment = json.getJSONArray("shipments").getJSONObject(0), item;
		try {
			item = findItem(shipment, parcelId);
		} catch (IllegalArgumentException e) {
			return new Parcel(Error.MULTI_PARCEL);
		}
		if (item == null) {
			return new Parcel(Error.NOT_FOUND);
		}
		
		if (shipment.has("consignor")) {
			data.put(ParcelDbAdapter.KEY_CUSTOMER, shipment.getJSONObject("consignor").getString("name"));
		}
		data.put(ParcelDbAdapter.KEY_SERVICE, shipment.getJSONObject("service").getString("name"));
		data.put(ParcelDbAdapter.KEY_STATUS, shipment.getJSONObject("statusText").getString("header"));
		data.put(ParcelDbAdapter.KEY_STATUSCODE, shipment.getString("status"));
		
		if (shipment.has("totalWeight")) {
			JSONObject weight = shipment.getJSONObject("totalWeight");
			data.put(ParcelDbAdapter.KEY_WEIGHT, weight.getString("value"));
			data.put(ParcelJsonParser.KEY_WEIGHT_UNIT, weight.getString("unit"));
		}
		
		if (shipment.has("consignee")) {
			JSONObject address = shipment.getJSONObject("consignee").getJSONObject("address");
			StringBuilder postal = new StringBuilder();
			if (address.has("postCode")) {
				postal.append(address.getString("postCode"));
			}
			if (address.has("city")) {
				postal.append(" ");
				postal.append(address.getString("city"));
			}
			data.put(ParcelDbAdapter.KEY_POSTAL, postal.toString().trim());
		}

		data.put(ParcelDbAdapter.KEY_PARCEL, item.getString("itemId"));
		if (item.has("dropOffDate")) {
			data.put(ParcelDbAdapter.KEY_SENT, item.getString("dropOffDate"));
		}

		data.put(KEY_EVENTS, parseEvents(item.getJSONArray("events")));

		return new Parcel(data);
	}

	private JSONObject findItem(JSONObject shipment, String parcelId) throws JSONException, IllegalArgumentException {
		JSONArray items = shipment.getJSONArray("items");
		JSONObject item = null;
		for (int i = 0; i < items.length(); i++) {
			JSONObject thisItem = items.getJSONObject(i);
			boolean match = false;

			if (items.length() == 1) {
				//Can be a "shipment" with only one parcel, ignore the ID.
				match = true;
			} else {
				String itemId = thisItem.getString("itemId");
				if (itemId.equals(parcelId)) {
					match = true;
				} else if (itemId.endsWith(parcelId) && itemId.length() > parcelId.length()) {
					try {
						// Handles missing leading zeroes
						match = (Integer.parseInt(itemId) == Integer.parseInt(parcelId));
					} catch (Exception e) {
						// Ignore conversion errors
					}
				}
			}

			if (match){
				if (item == null) {
					item = thisItem;
				} else {
					throw new IllegalArgumentException("Multiple items matched");
				}
			}
		}
		return item;
	}
	
	private int checkError(JSONObject json) throws JSONException {
		JSONArray shipments = json.getJSONArray("shipments");
		if (shipments.length() == 0) {
			return Error.NOT_FOUND;
		} else if (shipments.length() > 1) {
			return Error.MULTI_PARCEL;
		}
		return Error.NONE;
	}
	
	private ArrayList<HashMap<String,Object>> parseEvents(JSONArray events) throws JSONException {
		ArrayList<HashMap<String,Object>> eventList = new ArrayList<HashMap<String,Object>>();
		
		for (int i = 0; i < events.length(); i++) {
			HashMap<String,Object> eventData = new HashMap<String,Object>();
			JSONObject event = events.getJSONObject(i);

			String description = "";
			if (event.has("eventDescription")) {
				description = event.getString("eventDescription");
			}
			eventData.put(ParcelDbAdapter.KEY_TIME, event.getString("eventTime"));
			eventData.put(ParcelDbAdapter.KEY_LOC, event.getJSONObject("location").getString("displayName"));
			eventData.put(ParcelDbAdapter.KEY_DESC, description);

			eventList.add(eventData);
		}
		
		return eventList;
	}
}
