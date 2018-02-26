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

package de.welthungerhilfe.cgm.scanner.helper.tasks;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Emerald on 2/23/2018.
 */

public class AddressTask extends AsyncTask<Void, Void, StringBuilder> {

    public interface OnAddressResult {
        void onAddress(String address);
    }

    private OnAddressResult addressListener;
    private double latitude, longitude;

    public AddressTask(double lat, double lng, OnAddressResult listener) {
        latitude = lat;
        longitude = lng;
        addressListener = listener;
    }

    @Override
    protected void onCancelled() {
        // TODO Auto-generated method stub
        super.onCancelled();
        this.cancel(true);
    }

    @Override
    protected StringBuilder doInBackground(Void... params) {
        // TODO Auto-generated method stub
        try {
            String latLng = latitude + "," + longitude;

            HttpURLConnection conn = null;
            StringBuilder jsonResults = new StringBuilder();
            String googleMapUrl = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" + latLng;

            URL url = new URL(googleMapUrl);
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
            return jsonResults;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    @Override
    protected void onPostExecute(StringBuilder result) {
        super.onPostExecute(result);
        try {
            JSONObject response = new JSONObject(result.toString());
            String status = response.getString("status");
            if (status.equals("OK")) {
                JSONArray results = response.getJSONArray("results");
                if (results.length() > 0) {
                    JSONObject jsonAddress = results.getJSONObject(0);
                    String address = jsonAddress.getString("formatted_address");
                    if (addressListener != null)
                        addressListener.onAddress(address);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
