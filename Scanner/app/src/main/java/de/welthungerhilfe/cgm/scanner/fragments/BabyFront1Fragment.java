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
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.OnClickListener;
import com.orhanobut.dialogplus.ViewHolder;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.activities.BabyScanActivity;
import de.welthungerhilfe.cgm.scanner.activities.MainActivity;

/**
 * Created by Emerald on 2/20/2018.
 */

public class BabyFront1Fragment extends Fragment {
    private ViewHolder scanDialogViewHolder;
    private DialogPlus scanResultDialog;

    private int frontHeight = 0;

    public void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);

        scanDialogViewHolder = new ViewHolder(R.layout.dialog_scan_result);
        scanResultDialog = DialogPlus.newDialog(getContext())
            .setContentHolder(scanDialogViewHolder)
            .setCancelable(false)
            .setInAnimation(R.anim.abc_fade_in)
            .setOutAnimation(R.anim.abc_fade_out)
            .setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(DialogPlus dialog, View view) {
                    switch (view.getId()) {
                        case R.id.txtRepeat:
                            dialog.dismiss();

                            waitScanResult();
                            break;
                        case R.id.btnNext:
                            dialog.dismiss();

                            ((BabyScanActivity)getActivity()).setFrontHeight(frontHeight);
                            break;
                    }
                }
            })
            .create();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_baby_front1, container, false);

        waitScanResult();

        return view;
    }

    private void showScanResultDialog() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView txtHeight = scanResultDialog.getHolderView().findViewById(R.id.txtHeight);
                frontHeight = 50 + new Random().nextInt(20);
                txtHeight.setText(Integer.toString(frontHeight));

                scanResultDialog.show();
            }
        });
    }

    private void waitScanResult() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                showScanResultDialog();
            }
        };

        new Timer().schedule(task, 3000);
    }
}
