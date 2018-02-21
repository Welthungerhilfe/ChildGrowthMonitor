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

/**
 * Created by Emerald on 2/21/2018.
 */

public class SessionManager {
    private final String TAG = SessionManager.class.getSimpleName();
    private final String PREF_KEY_USER = "pref_key_user";

    private final String KEY_USER_SIGNED = "key_user_signed";

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
}
