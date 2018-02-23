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
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.activities.CreateDataActivity;
import de.welthungerhilfe.cgm.scanner.adapters.RecyclerMeasureAdapter;
import de.welthungerhilfe.cgm.scanner.dialogs.ManulMeasureDialog;
import de.welthungerhilfe.cgm.scanner.models.Loc;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

/**
 * Created by Emerald on 2/19/2018.
 */

public class MeasuresDataFragment extends Fragment implements View.OnClickListener, ManulMeasureDialog.OnManualMeasureListener {
    private RecyclerView recyclerMeasure;
    private RecyclerMeasureAdapter adapterMeasure;

    private FloatingActionButton fabCreate;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_measure, container, false);

        recyclerMeasure = view.findViewById(R.id.recyclerMeasure);
        adapterMeasure = new RecyclerMeasureAdapter(getContext(), ((CreateDataActivity)getContext()).measures);
        recyclerMeasure.setAdapter(adapterMeasure);
        recyclerMeasure.setLayoutManager(new LinearLayoutManager(getContext()));

        fabCreate = view.findViewById(R.id.fabCreate);
        fabCreate.setOnClickListener(this);

        return view;
    }

    public void addMeasure(Measure measure) {
        adapterMeasure.addMeasure(measure);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fabCreate:
                if (((CreateDataActivity)getContext()).person == null) {
                    Snackbar.make(fabCreate, "Please register person first", Snackbar.LENGTH_LONG).show();
                } else {
                    ManulMeasureDialog dialog = new ManulMeasureDialog(getContext());
                    dialog.setManualMeasureListener(this);
                    dialog.show();
                }
                break;
        }
    }

    @Override
    public void onManualMeasure(float height, float weight, float muac, Loc location) {
        ((CreateDataActivity)getContext()).setMeasureData(height, weight, muac, "No Additional Info", location);
    }
}
