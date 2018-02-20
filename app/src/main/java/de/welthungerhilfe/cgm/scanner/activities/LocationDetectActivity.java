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

package de.welthungerhilfe.cgm.scanner.activities;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.models.Loc;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;

/**
 * Created by Emerald on 2/20/2018.
 */

public class LocationDetectActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerDragListener {

    @BindView(R.id.txtAddress)
    TextView txtAddress;
    @BindView(R.id.mapView)
    MapView mapView;
    GoogleMap googleMap;

    @OnClick(R.id.lytConfirm)
    void onConfirm(LinearLayout lytConfirm) {
        setResult(RESULT_OK, LocationDetectActivity.this.getIntent().putExtra(AppConstants.EXTRA_LOCATION, location));
        finish();
    }
    @OnClick(R.id.imgClose)
    void onClose(ImageView imgClose) {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    private Loc location;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_location_detect);

        ButterKnife.bind(this);

        mapView.onCreate(saveBundle);
        mapView.onResume();
        MapsInitializer.initialize(this);
        mapView.getMapAsync(this);

        location = new Loc();
        location.setLatitude(40.771330);
        location.setLongitude(-74.353335);
        location.setAddress("Livingstone");
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null)
            mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onMapReady(GoogleMap mMap) {
        googleMap = mMap;

        Marker marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(40.771330, -74.353335)));
        marker.setDraggable(true);
        googleMap.setOnMarkerDragListener(this);

        CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(40.771330, -74.353335)).zoom(12).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {
        LatLng latLng = marker.getPosition();
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        getAddressFromLocation();
    }

    private void getAddressFromLocation() {
        String latLng = location.getLatitude() + "," + location.getLongitude();
        new LocationTask().execute(new String[] {latLng});

        /*
        Thread thread = new Thread() {
            @Override
            public void run() {
                Geocoder geocoder = new Geocoder(LocationDetectActivity.this, Locale.getDefault());
                String result = null;
                try {
                    List<Address> addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addressList != null && addressList.size() > 0) {
                        Address address = addressList.get(0);
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                            sb.append(address.getAddressLine(i));
                        }
                        sb.append(address.getLocality()).append("\n");
                        sb.append(address.getPostalCode()).append("\n");
                        sb.append(address.getCountryName());
                        result = sb.toString();
                    }
                } catch (IOException e) {
                    Log.e("Location Address Loader", "Unable connect to Geocoder", e);
                } finally {
                    if (result != null) {
                        location.setAddress(result);
                        txtAddress.setText(result);
                    }
                }
            }
        };
        thread.start();
        */
    }


    public class LocationTask extends AsyncTask<String, Void, StringBuilder> {

        @Override
        protected void onCancelled() {
            // TODO Auto-generated method stub
            super.onCancelled();
            this.cancel(true);
        }

        @Override
        protected StringBuilder doInBackground(String... params) {
            // TODO Auto-generated method stub
            try {
                HttpURLConnection conn = null;
                StringBuilder jsonResults = new StringBuilder();
                String googleMapUrl = "http://maps.googleapis.com/maps/api/geocode/json?latlng=" + params[0];

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
                        location.setAddress(address);
                        txtAddress.setText(address);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
