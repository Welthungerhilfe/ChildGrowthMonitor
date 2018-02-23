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

package de.welthungerhilfe.cgm.scanner.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.activities.CreateDataActivity;

/**
 * Created by Emerald on 2/23/2018.
 */

public class ManulMeasureDialog extends Dialog {
    @BindView(R.id.editManualHeight)
    EditText editManualHeight;
    @BindView(R.id.editManualWeight)
    EditText editManualWeight;
    @BindView(R.id.editManualMuac)
    EditText editManualMuac;
    @BindView(R.id.editManualAddition)
    EditText editManualAddition;
    @BindView(R.id.btnOK)
    Button btnOK;

    @OnClick(R.id.btnAlert)
    void onAlert(Button btnAlert) {

    }
    @OnClick(R.id.txtCancel)
    void onCancel(TextView txtCancel) {
        dismiss();
    }
    @OnClick(R.id.btnOK)
    void OnConfirm(Button btnOK) {
        if (validate()) {
            if (measureListener != null)
                measureListener.onManualMeasure(
                        Float.parseFloat(editManualHeight.getText().toString()),
                        Float.parseFloat(editManualWeight.getText().toString()),
                        Float.parseFloat(editManualMuac.getText().toString())
                        );
            dismiss();
        }
    }

    private OnManualMeasureListener measureListener;

    public ManulMeasureDialog(@NonNull Context context) {
        super(context);

        this.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.dialog_manual_measure);
        this.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.getWindow().getAttributes().windowAnimations = R.style.DialogAnimationScale;
        this.setCancelable(false);

        ButterKnife.bind(this);
    }

    public void setManualMeasureListener(OnManualMeasureListener listener) {
        measureListener = listener;
    }

    private boolean validate() {
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

    public interface OnManualMeasureListener {
        void onManualMeasure(float height, float weight, float muac);
    }
}
