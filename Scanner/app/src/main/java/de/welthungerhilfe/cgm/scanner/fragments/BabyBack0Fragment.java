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
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.activities.BabyScanActivity;

/**
 * Created by Emerald on 2/20/2018.
 */

public class BabyBack0Fragment extends Fragment implements View.OnClickListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_baby_back0, container, false);

        view.findViewById(R.id.btnStartScan).setOnClickListener(this);

        return view;
    }

    private void waitScanResult() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                int height = 50 + new Random().nextInt(20);
                ((BabyScanActivity)getActivity()).setBackHeight(height);
            }
        };

        new Timer().schedule(task, 3000);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnStartScan:
                waitScanResult();
                break;
        }
    }
}
