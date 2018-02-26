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
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;

/**
 * Created by Emerald on 2/19/2018.
 */

public class ConfirmDialog extends Dialog {

    @BindView(R.id.txtMessage)
    TextView txtMessage;

    @OnClick(R.id.txtOK)
    void onConfirm(TextView txtOK) {
        dismiss();
        if (confirmListener != null) {
            confirmListener.onConfirm(true);
        }
    }

    @OnClick(R.id.txtCancel)
    void onCancel(TextView txtCancel) {
        dismiss();
        if (confirmListener != null) {
            confirmListener.onConfirm(false);
        }
    }

    public interface OnConfirmListener {
        void onConfirm(boolean result);
    }

    private OnConfirmListener confirmListener;

    public ConfirmDialog(@NonNull Context context) {
        super(context);

        this.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.dialog_confirm);
        this.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.getWindow().getAttributes().windowAnimations = R.style.DialogAnimationScale;
        this.setCancelable(false);

        ButterKnife.bind(this);
    }

    public void setConfirmListener(OnConfirmListener confirmListener) {
        this.confirmListener = confirmListener;
    }

    public void setMessage(String message) {
        txtMessage.setText(message);
    }
}
