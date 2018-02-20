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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;

/**
 * Created by Emerald on 2/19/2018.
 */

public class MeasuresDataFragment extends Fragment {

    @BindView(R.id.txtDate)
    TextView txtDate;
    @BindView(R.id.editHeight)
    EditText editHeight;
    @BindView(R.id.editWeight)
    EditText editWeight;
    @BindView(R.id.editMuac)
    EditText editMuac;
    @BindView(R.id.editAddition)
    EditText editAddition;
    @BindView(R.id.btnAlert)
    Button btnAlert;
    @BindView(R.id.txtCancel)
    TextView txtCancel;
    @BindView(R.id.txtOK)
    TextView txtOK;

    @OnClick(R.id.lytSelectDate)
    void selectDate() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_measure, container, false);

        ButterKnife.bind(view);

        return view;
    }
}
