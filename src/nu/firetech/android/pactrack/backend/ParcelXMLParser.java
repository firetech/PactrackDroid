/*
 * Copyright (C) 2014 Joakim Andersson
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

import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import nu.firetech.android.pactrack.common.Error;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

//FIXME Rewrite to parse http://logistics.postennorden.com/wsp/rest-services/ntt-service-rest/api/shipment.json
public class ParcelXMLParser extends DefaultHandler {
	private static final String TAG = "<PactrackDroid> ParcelXMLParser";
	
	private static final String BASE_URL = "http://server.logistik.posten.se/servlet/PacTrack?lang=SE&kolliid=%s"; 
	
	public static final int STATUS_DELIVERED = 7;
	public static final int STATUS_COLLECTABLE = 6;
	public static final int STATUS_PREINFO = 0;

	public static Parcel fetch(String parcelId) {
		try {
			URL parcelUrl = new URL(String.format(BASE_URL, parcelId));

			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser(); 
			XMLReader xr = sp.getXMLReader();

			ParcelXMLParser parser = new ParcelXMLParser();
			xr.setContentHandler(parser);

			xr.parse(new InputSource(new InputStreamReader(parcelUrl.openStream(), "ISO-8859-1"))); 

			if (parser.mData.isEmpty()) {
				throw new Exception();
			}

			return new Parcel(parser.mData);
		} catch (IllegalArgumentException e) {
			Log.d(TAG, "MULTI_PARCEL error", e);
			return new Parcel(Error.MULTI_PARCEL);
		} catch (NoSuchFieldError e) {
			Log.d(TAG, "NOT_FOUND error", e);
			return new Parcel(Error.NOT_FOUND);
		} catch (Exception e) { //Other errors are server errors
			Log.d(TAG, "SERVER error", e);
			return new Parcel(Error.SERVER);
		}
	}

	////////////////////////////////////////////////////////////////////////////////

	private Stack<String> mElementPath = new Stack<String>();
	private HashMap<String,Object> mData = new HashMap<String,Object>();

	private boolean mInEvent = false;
	private HashMap<String,Object> mEventData = new HashMap<String,Object>();


	private ParcelXMLParser() {
		mData.put("events", new ArrayList<ParcelEvent>());
	} 

	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
		mElementPath.push(localName);

		if (localName.equals("parcel")) {
			String parcel = atts.getValue("id");
			if (parcel != null) {
				mData.put("parcel", parcel);
			}
	    } else if (localName.equals("event") || localName.equals("errorevent")) {
			mInEvent = true;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		if (!localName.equals(mElementPath.peek())) {
			throw new SAXException("Unexpected end tag!");
		} else if (localName.equals("event") || localName.equals("errorevent")) {
			if (mInEvent) {
				((ArrayList<ParcelEvent>)mData.get("events")).add(new ParcelEvent(mEventData));
				mEventData.clear();
			} else {
				throw new SAXException("Unexpected event end tag!");
			}
			mInEvent = false;
		}

		mElementPath.pop();
	}

	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		String lastTag = mElementPath.peek();
		String contents = new String(ch, start, length);
		
		HashMap<String,Object> destHash = null;

		// Handle errors
		if (lastTag.equals("noofparcelentries") && !contents.equals("1")) {
			//Too many parcels to handle, we don't support this, yet...
			throw new IllegalArgumentException();
		} else if (lastTag.equals("internalstatus") && contents.equals("0")) {
			//Parcel doesn't exist.
			throw new NoSuchFieldError();

		// Get parcel fields
		} else if (!mInEvent && (lastTag.equals("customername") ||
				lastTag.equals("datesent") ||
				lastTag.equals("actualweight") ||
				lastTag.equals("receiverzipcode") ||
				lastTag.equals("receivercity") ||
				lastTag.equals("servicename") ||
				lastTag.equals("statusdescription") ||
				lastTag.equals("statuscode"))) {
			destHash = mData;
		
		// Get Event fields
		} else if (mInEvent &&
				(lastTag.equals("location") ||
				lastTag.equals("description") ||
				lastTag.equals("date") ||
				lastTag.equals("time"))) {
			destHash = mEventData;
		}
		
		if (destHash != null) {
			if (destHash.containsKey(lastTag)) {
				contents = (String)destHash.get(lastTag) + contents;
			}
			destHash.put(lastTag, contents);
		}
		
	}
}
