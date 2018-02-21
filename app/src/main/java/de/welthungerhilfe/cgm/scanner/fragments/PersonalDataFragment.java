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

package de.welthungerhilfe.cgm.scanner.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.activities.BodySelectActivity;
import de.welthungerhilfe.cgm.scanner.activities.CreateDataAcitivty;
import de.welthungerhilfe.cgm.scanner.activities.ImageDetailActivity;
import de.welthungerhilfe.cgm.scanner.activities.LocationDetectActivity;
import de.welthungerhilfe.cgm.scanner.models.Loc;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.BitmapUtils;

/**
 * Created by Emerald on 2/19/2018.
 */

public class PersonalDataFragment extends Fragment implements View.OnClickListener {
    private final int REQUEST_LOCATION = 0x1000;

    public Context context;

    private ImageView imgConsent;

    private EditText editName, editPrename, editLocation, editBirth, editAge, editGuardian;

    private AppCompatRadioButton radioFemale, radioMale, radioFluid;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal, container, false);

        view.findViewById(R.id.rytConsentDetail).setOnClickListener(this);
        view.findViewById(R.id.imgLocation).setOnClickListener(this);
        view.findViewById(R.id.imgBirth).setOnClickListener(this);
        view.findViewById(R.id.txtBack).setOnClickListener(this);
        view.findViewById(R.id.btnNext).setOnClickListener(this);

        imgConsent = view.findViewById(R.id.imgConsent);
        byte[] data = ((CreateDataAcitivty)getContext()).qrSource;
        if (data != null) {
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            imgConsent.setImageBitmap(BitmapUtils.rotateBitmap(bmp, 90));
        }

        editName = view.findViewById(R.id.editName);
        editPrename = view.findViewById(R.id.editPrename);
        editLocation = view.findViewById(R.id.editLocation);
        editBirth = view.findViewById(R.id.editBirth);
        editAge = view.findViewById(R.id.editAge);
        editGuardian = view.findViewById(R.id.editGuardian);

        radioFemale = view.findViewById(R.id.radioFemale);
        radioMale = view.findViewById(R.id.radioMale);
        radioFluid = view.findViewById(R.id.radioFluid);

        return view;
    }

    public void onActivityResult(int reqCode, int resCode, Intent data) {
        if (reqCode == REQUEST_LOCATION && resCode == Activity.RESULT_OK) {
            Loc location = (Loc)data.getSerializableExtra(AppConstants.EXTRA_LOCATION);
            editLocation.setText(location.getAddress());
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imgLocation:
                startActivityForResult(new Intent(getContext(), LocationDetectActivity.class), REQUEST_LOCATION);
                break;
            case R.id.imgBirth:

                break;
            case R.id.btnNext:
                startActivity(new Intent(getContext(), BodySelectActivity.class));
                break;
            case R.id.rytConsentDetail:
                Intent intent = new Intent(getContext(), ImageDetailActivity.class);
                intent.putExtra(AppConstants.EXTRA_QR_BITMAP, ((CreateDataAcitivty)getContext()).qrSource);
                startActivity(intent);
                break;
        }
    }
}
