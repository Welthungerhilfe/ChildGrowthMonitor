package de.welthungerhilfe.cgm.scanner.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.activities.RecorderActivity;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;

/**
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com>
 * Copyright (c) 2018 Welthungerhilfe Innovation
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class BabyInfantChooserFragment extends Fragment implements View.OnClickListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_body_select, container, false);
        Button btnBaby = view.findViewById(R.id.btnBaby);
        btnBaby.setOnClickListener(this);
        Button btnInfant = view.findViewById(R.id.btnInfant);
        btnInfant.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {

        Log.v("BICF","ChooserFragment clicked on "+view.getId());
        switch (view.getId()) {
            case R.id.btnBaby:
                ((RecorderActivity)getActivity()).gotoNextStep(AppConstants.LYING_BABY_SCAN);
                break;
            case R.id.btnInfant:
                ((RecorderActivity)getActivity()).gotoNextStep(AppConstants.STANDING_INFANT_SCAN);
                break;
        }

    }
}
