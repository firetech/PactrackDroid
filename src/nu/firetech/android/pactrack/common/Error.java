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

package nu.firetech.android.pactrack.common;

// Not an enum because we need to be able to map from integer and back. 
public class Error {
	public static final int NONE = -1;
	public static final int SERVER = 500;
	public static final int MULTI_PARCEL = 403;
	public static final int NOT_FOUND = 404;
	
	// prevent instantiating
	private Error() {}
}
