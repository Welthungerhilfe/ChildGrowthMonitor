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
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.appeaser.sublimepickerlibrary.datepicker.SelectedDate;
import com.appeaser.sublimepickerlibrary.recurrencepicker.SublimeRecurrencePicker;
import com.bumptech.glide.Glide;

import java.util.Date;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.activities.CreateDataActivity;
import de.welthungerhilfe.cgm.scanner.activities.ImageDetailActivity;
import de.welthungerhilfe.cgm.scanner.activities.LocationDetectActivity;
import de.welthungerhilfe.cgm.scanner.dialogs.DateRangePickerDialog;
import de.welthungerhilfe.cgm.scanner.models.Loc;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

/**
 * Created by Emerald on 2/19/2018.
 */

public class PersonalDataFragment extends Fragment implements View.OnClickListener, DateRangePickerDialog.Callback {
    private final int REQUEST_LOCATION = 0x1000;

    public Context context;

    private LinearLayout lytCreate;

    private ImageView imgConsent, imgBirth;

    private TextView txtDate;

    private EditText editName, editPrename, editLocation, editBirth, editAge, editGuardian;

    private AppCompatRadioButton radioFemale, radioMale, radioFluid;

    private Loc location = null;
    private long birthday = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal, container, false);

        view.findViewById(R.id.rytConsentDetail).setOnClickListener(this);
        //view.findViewById(R.id.imgLocation).setOnClickListener(this);
        imgBirth = view.findViewById(R.id.imgBirth);
        imgBirth.setOnClickListener(this);
        view.findViewById(R.id.txtBack).setOnClickListener(this);
        view.findViewById(R.id.btnNext).setOnClickListener(this);

        lytCreate = view.findViewById(R.id.lytCreate);

        imgConsent = view.findViewById(R.id.imgConsent);

        txtDate = view.findViewById(R.id.txtDate);
        editName = view.findViewById(R.id.editName);
        editPrename = view.findViewById(R.id.editPrename);
        editLocation = view.findViewById(R.id.editLocation);
        editBirth = view.findViewById(R.id.editBirth);
        editAge = view.findViewById(R.id.editAge);
        editGuardian = view.findViewById(R.id.editGuardian);

        radioFemale = view.findViewById(R.id.radioFemale);
        radioMale = view.findViewById(R.id.radioMale);
        radioFluid = view.findViewById(R.id.radioFluid);

        initUI();

        return view;
    }

    public void initUI() {
        if (((CreateDataActivity)getContext()).person != null) {
            lytCreate.setVisibility(View.GONE);

            Glide.with(getContext()).load(((CreateDataActivity)getContext()).person.getQrNumber().getConsent()).into(imgConsent);

            txtDate.setText(Utils.beautifyDate(((CreateDataActivity)getContext()).person.getCreated()));

            editName.setText(((CreateDataActivity)getContext()).person.getName());
            editPrename.setText(((CreateDataActivity)getContext()).person.getSurname());
            editBirth.setText(Utils.beautifyDate(((CreateDataActivity)getContext()).person.getBirthday()));
            editAge.setText(Integer.toString(((CreateDataActivity)getContext()).person.getAge()));
            editGuardian.setText(((CreateDataActivity)getContext()).person.getGuardian());
            if (((CreateDataActivity)getContext()).person.getLastLocation() != null)
                editLocation.setText(((CreateDataActivity)getContext()).person.getLastLocation().getAddress());

            if (((CreateDataActivity)getContext()).person.getSex().equals(AppConstants.VAL_SEX_FEMALE)) {
                radioFemale.setChecked(true);
            } else if (((CreateDataActivity)getContext()).person.getSex().equals(AppConstants.VAL_SEX_MALE)) {
                radioMale.setChecked(true);
            } else if (((CreateDataActivity)getContext()).person.getSex().equals(AppConstants.VAL_SEX_OTHER)) {
                radioFluid.setChecked(true);
            }

            imgBirth.setVisibility(View.GONE);
        } else {
            lytCreate.setVisibility(View.VISIBLE);

            txtDate.setText(Utils.beautifyDate(new Date()));

            byte[] data = ((CreateDataActivity)getContext()).qrSource;
            if (data != null) {
                imgConsent.setImageBitmap(BitmapFactory.decodeByteArray(data, 0, data.length));
            }
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

        /*
        if (location.isEmpty()) {
            editLocation.setError("Please input location");
            valid = false;
        } else {
            editLocation.setError(null);
        }
        */

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

        if (radioFemale.isChecked() || radioMale.isChecked() || radioFluid.isChecked()) {
            valid = true;
        } else {
            valid = false;
            Snackbar.make(radioFemale, "Please select sex", Snackbar.LENGTH_SHORT).show();
        }

        return valid;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imgLocation:
                //startActivityForResult(new Intent(getContext(), LocationDetectActivity.class), REQUEST_LOCATION);
                break;
            case R.id.imgBirth:
                DateRangePickerDialog dateRangePicker = new DateRangePickerDialog();
                dateRangePicker.setCallback(this);
                dateRangePicker.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
                dateRangePicker.show(getActivity().getSupportFragmentManager(), "DATE_RANGE_PICKER");

                break;
            case R.id.btnNext:
                if (validate()) {

                    String sex = "";
                    if (radioMale.isChecked())
                        sex = radioMale.getText().toString();
                    else if (radioFemale.isChecked())
                        sex = radioMale.getText().toString();
                    else if (radioFluid.isChecked())
                        sex = radioFluid.getText().toString();

                    ((CreateDataActivity)getContext()).setPersonalData(
                            editName.getText().toString(), editPrename.getText().toString(),
                            birthday, Integer.parseInt(editAge.getText().toString()),
                            sex, location, editGuardian.getText().toString());
                }

                break;
            case R.id.rytConsentDetail:
                Intent intent = new Intent(getContext(), ImageDetailActivity.class);
                intent.putExtra(AppConstants.EXTRA_QR_BITMAP, ((CreateDataActivity)getContext()).qrSource);
                //startActivity(intent);
                break;
            case R.id.txtBack:
                getActivity().finish();
                break;
        }
    }

    /*
    public void onActivityResult(int reqCode, int resCode, Intent data) {
        if (reqCode == REQUEST_LOCATION && resCode == Activity.RESULT_OK) {
            location = (Loc)data.getSerializableExtra(AppConstants.EXTRA_LOCATION);
            editLocation.setText(location.getAddress());
        }
    }
    */

    @Override
    public void onDateTimeRecurrenceSet(SelectedDate selectedDate, int hourOfDay, int minute, SublimeRecurrencePicker.RecurrenceOption recurrenceOption, String recurrenceRule) {
        birthday = selectedDate.getStartDate().getTimeInMillis();
        editBirth.setText(Utils.beautifyDate(selectedDate.getStartDate().getTimeInMillis()));
    }
}
