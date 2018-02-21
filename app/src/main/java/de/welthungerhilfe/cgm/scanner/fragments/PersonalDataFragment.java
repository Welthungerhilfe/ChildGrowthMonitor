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
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.util.Calendar;

import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.activities.BodySelectActivity;
import de.welthungerhilfe.cgm.scanner.activities.CreateDataAcitivty;
import de.welthungerhilfe.cgm.scanner.activities.ImageDetailActivity;
import de.welthungerhilfe.cgm.scanner.activities.LocationDetectActivity;
import de.welthungerhilfe.cgm.scanner.models.Loc;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.BitmapUtils;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

/**
 * Created by Emerald on 2/19/2018.
 */

public class PersonalDataFragment extends Fragment implements View.OnClickListener, DatePickerDialog.OnDateSetListener {
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
            imgConsent.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.length));
        }

        editName = view.findViewById(R.id.editName);
        editPrename = view.findViewById(R.id.editPrename);
        editLocation = view.findViewById(R.id.editLocation);
        editBirth = view.findViewById(R.id.editBirth);
        editBirth.setText("02/21/2018");
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

    public boolean validate() {
        boolean valid = true;

        String name = editName.getText().toString();
        String prename = editPrename.getText().toString();
        String location = editLocation.getText().toString();
        String birth = editBirth.getText().toString();
        String age = editAge.getText().toString();
        String guardian = editGuardian.getText().toString();

        if (name.isEmpty()) {
            editName.setError("Please input name");
            valid = false;
        } else {
            editName.setError(null);
        }

        if (prename.isEmpty()) {
            editPrename.setError("Please input prename");
            valid = false;
        } else {
            editPrename.setError(null);
        }

        if (location.isEmpty()) {
            editLocation.setError("Please input location");
            valid = false;
        } else {
            editLocation.setError(null);
        }

        if (birth.isEmpty()) {
            editBirth.setError("Please input birthday");
            valid = false;
        } else {
            editBirth.setError(null);
        }

        if (age.isEmpty()) {
            editAge.setError("Please input age");
            valid = false;
        } else {
            editName.setError(null);
        }

        if (guardian.isEmpty()) {
            editGuardian.setError("Please input guardian");
            valid = false;
        } else {
            editGuardian.setError(null);
        }

        return valid;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imgLocation:
                startActivityForResult(new Intent(getContext(), LocationDetectActivity.class), REQUEST_LOCATION);
                break;
            case R.id.imgBirth:
                com.wdullaer.materialdatetimepicker.date.DatePickerDialog.newInstance(this).show(getActivity().getFragmentManager(), "Datepickerdialog");
                break;
            case R.id.btnNext:
                if (validate()) {
                    String personId = Utils.getSaltString(10);

                    String consentPath = AppConstants.STORAGE_CONSENT_URL.replace("{id}", personId) + System.currentTimeMillis() + "_" + ((CreateDataAcitivty)getContext()).qrCode + ".png";
                    StorageReference consentRef = AppController.getInstance().storageRootRef.child(consentPath);
                    UploadTask uploadTask = consentRef.putBytes(((CreateDataAcitivty)getContext()).qrSource);
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Uri downloadUrl = taskSnapshot.getDownloadUrl();

                            ((CreateDataAcitivty)getContext()).setPersonalData(
                                    editName.getText().toString(), editPrename.getText().toString(),
                                    editLocation.getText().toString(), editBirth.getText().toString(),
                                    Integer.parseInt(editAge.getText().toString()), "male", downloadUrl.toString());
                        }
                    });
                }

                break;
            case R.id.rytConsentDetail:
                Intent intent = new Intent(getContext(), ImageDetailActivity.class);
                intent.putExtra(AppConstants.EXTRA_QR_BITMAP, ((CreateDataAcitivty)getContext()).qrSource);
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        editBirth.setText((monthOfYear + 1) + "/" + dayOfMonth + "/" + year);
    }
}
