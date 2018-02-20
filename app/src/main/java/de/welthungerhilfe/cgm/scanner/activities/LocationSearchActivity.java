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
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;

/**
 * Created by Emerald on 2/20/2018.
 */

public class LocationSearchActivity extends AppCompatActivity implements OnMapReadyCallback, SeekBar.OnSeekBarChangeListener {

    private ArrayList<Person> personList;

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
        setResult(RESULT_OK, LocationSearchActivity.this.getIntent().putExtra(AppConstants.EXTRA_PERSON_LIST, personList));
        finish();
    }

    private int radius;
    private Circle circleRange;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_location_search);

        ButterKnife.bind(this);

        personList = new ArrayList<>();

        mapView.onCreate(saveBundle);
        mapView.onResume();
        MapsInitializer.initialize(this);
        mapView.getMapAsync(this);

        radius = seekRadius.getProgress();
        txtRadius.setText(Integer.toString(radius));
        seekRadius.setOnSeekBarChangeListener(this);
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

        circleRange = googleMap.addCircle(new CircleOptions()
                .center(new LatLng(40.771330, -74.353335))
                .radius(1 * 1000)
                .strokeColor(Color.WHITE)
                .strokeWidth(4.0f)
                .fillColor(getResources().getColor(R.color.colorGreenTransparent)));
        circleRange.setVisible(true);

        CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(40.771330, -74.353335)).zoom(12).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        radius = i == 0 ? 1 : i;
        circleRange.setRadius(radius * 1000);
        txtRadius.setText(Integer.toString(radius));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
