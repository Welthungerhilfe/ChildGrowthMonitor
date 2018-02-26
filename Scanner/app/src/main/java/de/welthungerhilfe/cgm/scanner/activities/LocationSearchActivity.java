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

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.models.Loc;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.tasks.AddressTask;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

/**
 * Created by Emerald on 2/20/2018.
 */

public class LocationSearchActivity extends AppCompatActivity implements OnMapReadyCallback, SeekBar.OnSeekBarChangeListener, AddressTask.OnAddressResult {
    private final int PERMISSION_LOCATION = 0x1001;

    private ArrayList<Person> personList;
    private Location location = null;
    private SessionManager session;

    @BindView(R.id.txtAddress)
    TextView txtAddress;
    @BindView(R.id.txtRadius)
    TextView txtRadius;
    @BindView(R.id.seekRadius)
    AppCompatSeekBar seekRadius;
    @BindView(R.id.mapView)
    MapView mapView;
    GoogleMap googleMap;

    @OnClick(R.id.txtCancel)
    void onCancel(TextView txtCancel) {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    @OnClick(R.id.txtOK)
    void onOK(TextView txtOK) {
        setResult(RESULT_OK, LocationSearchActivity.this.getIntent().putExtra(AppConstants.EXTRA_RADIUS, radius));
        finish();
    }

    private int radius;
    private Circle circleRange;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_location_search);

        ButterKnife.bind(this);

        personList = new ArrayList<>();
        session = new SessionManager(LocationSearchActivity.this);

        mapView.onCreate(saveBundle);
        mapView.onResume();
        MapsInitializer.initialize(this);
        mapView.getMapAsync(this);

        radius = seekRadius.getProgress();
        txtRadius.setText(Integer.toString(radius));
        seekRadius.setOnSeekBarChangeListener(this);

        getCurrentLocation();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION"}, PERMISSION_LOCATION);
        } else {
            LocationManager lm = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

            boolean isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            } else if (isNetworkEnabled || isGPSEnabled) {
                List<String> providers = lm.getProviders(true);
                for (String provider : providers) {
                    Location l = lm.getLastKnownLocation(provider);
                    if (l == null) {
                        continue;
                    }
                    if (location == null || l.getAccuracy() < location.getAccuracy()) {
                        location = l;
                    }
                }
                if (location != null) {
                    searchNearbyPersons();

                    new AddressTask(location.getLatitude(), location.getLongitude(), this).execute();

                    if (googleMap != null)
                        drawCircle();
                }
            }
        }
    }

    private void drawCircle() {
        circleRange = googleMap.addCircle(new CircleOptions()
                .center(new LatLng(location.getLatitude(), location.getLongitude()))
                .radius(radius * 1000)
                .strokeColor(Color.WHITE)
                .strokeWidth(4.0f)
                .fillColor(getResources().getColor(R.color.colorGreenTransparent)));
        circleRange.setVisible(true);

        googleMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())));

        CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(location.getLatitude(), location.getLongitude())).zoom(12).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void putMarkers() {
        if (googleMap != null) {
            for (int i = 0; i < personList.size(); i++) {
                googleMap.addMarker(new MarkerOptions().position(new LatLng(personList.get(i).getLastLocation().getLatitude(), personList.get(i).getLastLocation().getLongitude())));
            }
        }
    }

    private void searchNearbyPersons() {
        /*
        personList = new ArrayList<>();
        AppController.getInstance().firebaseFirestore.collection("persons")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                Person person = document.toObject(Person.class);
                                if (Utils.distanceBetweenLocs(session.getLocation(), person.getLastLocation()) < radius)
                                    personList.add(person);
                            }
                        }
                        putMarkers();
                    }
                });
                */
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

        if (location != null) {
            drawCircle();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        radius = i == 0 ? 1 : i;
        txtRadius.setText(Integer.toString(radius));
        if (circleRange != null) {
            circleRange.setRadius(radius * 1000);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        radius = seekBar.getProgress() == 0 ? 1 : seekBar.getProgress();
        if (googleMap != null) {
            searchNearbyPersons();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_LOCATION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(LocationSearchActivity.this, "App can't get device location", Toast.LENGTH_LONG).show();
            } else {
                getCurrentLocation();
            }
        }
    }

    @Override
    public void onAddress(String address) {
        txtAddress.setText(address);

        Loc loc = new Loc();
        loc.setLatitude(location.getLatitude());
        loc.setLongitude(location.getLongitude());
        loc.setAddress(address);
        session.setLocation(loc);
    }
}
