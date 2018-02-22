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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.activities.CreateDataActivity;
import de.welthungerhilfe.cgm.scanner.models.Measure;

/**
 * Created by Emerald on 2/19/2018.
 */

public class MeasuresDataFragment extends Fragment implements View.OnClickListener {

    private TextView txtDate;
    private LinearLayout lytMeasureManual, lytMeasureMachine, lytMeasureOperation;
    private EditText editManualHeight, editManualWeight, editManualMuac, editManualAddition;
    private EditText editMachineHeight, editMachineWeight, editMachineMuac;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_measure, container, false);

        txtDate = view.findViewById(R.id.txtDate);

        editManualHeight = view.findViewById(R.id.editManualHeight);
        editManualWeight = view.findViewById(R.id.editManualWeight);
        editManualMuac = view.findViewById(R.id.editManualMuac);
        editManualAddition = view.findViewById(R.id.editManualAddition);

        editMachineHeight = view.findViewById(R.id.editMachineHeight);
        editMachineWeight = view.findViewById(R.id.editMachineWeight);
        editMachineMuac = view.findViewById(R.id.editMachineMuac);

        lytMeasureManual = view.findViewById(R.id.lytMeasureManual);
        lytMeasureMachine = view.findViewById(R.id.lytMeasureMachine);
        lytMeasureOperation = view.findViewById(R.id.lytMeasureOperation);

        view.findViewById(R.id.txtCancel).setOnClickListener(this);
        view.findViewById(R.id.txtOK).setOnClickListener(this);
        view.findViewById(R.id.btnAlert).setOnClickListener(this);
        view.findViewById(R.id.lytSelectDate).setOnClickListener(this);

        return view;
    }

    public void setMachineMeasure(final Measure measure) {
        String dateStr = measure.getDate();
        txtDate.setText(dateStr);

        editMachineHeight.setText(Float.toString(measure.getHeight()));
        editMachineWeight.setText(Float.toString(measure.getWeight()));
        editMachineMuac.setText(Float.toString(measure.getMuac()));
    }

    public boolean validate() {
        boolean valid = true;

        String height = editManualHeight.getText().toString();
        String weight = editManualWeight.getText().toString();
        String muac = editManualMuac.getText().toString();

        if (height.isEmpty()) {
            editManualHeight.setError("Please input height");
            valid = false;
        } else {
            editManualHeight.setError(null);
        }

        if (weight.isEmpty()) {
            editManualWeight.setError("Please input weight");
            valid = false;
        } else {
            editManualWeight.setError(null);
        }

        if (muac.isEmpty()) {
            editManualMuac.setError("Please input MUAC");
            valid = false;
        } else {
            editManualMuac.setError(null);
        }

        return valid;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.txtCancel:
                break;
            case R.id.txtOK:
                lytMeasureOperation.setVisibility(View.GONE);
                lytMeasureMachine.setVisibility(View.VISIBLE);
                if (validate()) {

                }
                ((CreateDataActivity)getContext()).setMeasureData(
                        Float.parseFloat(editManualHeight.getText().toString()), Float.parseFloat(editManualWeight.getText().toString()),
                        Float.parseFloat(editManualMuac.getText().toString()), editManualAddition.getText().toString());
                break;
            case R.id.btnAlert:
                break;
            case R.id.lytSelectDate:
                break;
        }
    }
}
