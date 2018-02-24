/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.welthungerhilfe.cgm.scanner.helper;

import android.content.Context;
import android.content.SharedPreferences;

import de.welthungerhilfe.cgm.scanner.models.Loc;

/**
 * Created by Emerald on 2/21/2018.
 */

public class SessionManager {
    private final String TAG = SessionManager.class.getSimpleName();
    private final String PREF_KEY_USER = "pref_key_user";

    private final String KEY_USER_SIGNED = "key_user_signed";
    private final String KEY_USER_LOCATION_LATITUDE = "key_user_location_latitude";
    private final String KEY_USER_LOCATION_LONGITUDE = "key_user_location_longitude";
    private final String KEY_USER_LOCATION_ADDRESS = "key_user_location_address";

    SharedPreferences pref;
    SharedPreferences.Editor editor;

    private Context context;

    public SessionManager(Context ctx) {
        this.context = ctx;
        pref = context.getSharedPreferences(PREF_KEY_USER, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void setSigned(boolean signed) {
        editor.putBoolean(KEY_USER_SIGNED, signed);

        editor.commit();
    }

    public boolean isSigned() {
        return pref.getBoolean(KEY_USER_SIGNED, false);
    }

    public void setLocation(Loc location) {
        editor.putString(KEY_USER_LOCATION_LATITUDE, Double.toString(location.getLatitude()));
        editor.putString(KEY_USER_LOCATION_LONGITUDE, Double.toString(location.getLongitude()));
        editor.putString(KEY_USER_LOCATION_ADDRESS, location.getAddress());

        editor.commit();
    }

    public Loc getLocation() {
        Loc location = new Loc();
        location.setLatitude(Double.parseDouble(pref.getString(KEY_USER_LOCATION_LATITUDE, "0")));
        location.setLongitude(Double.parseDouble(pref.getString(KEY_USER_LOCATION_LONGITUDE, "0")));
        location.setAddress(pref.getString(KEY_USER_LOCATION_ADDRESS, ""));

        return location;
    }
}
