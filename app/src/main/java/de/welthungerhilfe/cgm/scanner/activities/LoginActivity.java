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

package de.welthungerhilfe.cgm.scanner.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;

public class LoginActivity extends BaseActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private FirebaseAuth mAuth;

    @BindView(R.id.editUser)
    EditText editUser;
    @BindView(R.id.editPassword)
    EditText editPassword;

    @BindString(R.string.validate_user)
    String strUserValidation;
    @BindString(R.string.validate_password)
    String strPasswordValidation;

    @OnClick(R.id.txtOK)
    void doSignIn(TextView txtOK) {
        if (validate()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        }
    }

    @OnClick(R.id.txtCancel)
    void doSignOut(TextView txtCancel) {

    }

    @OnClick(R.id.txtForgot)
    void doForgot(TextView txtForgot) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);

        mAuth = FirebaseAuth.getInstance();
    }

    private boolean validate() {
        boolean valid = true;

        String user = editUser.getText().toString();
        String password = editPassword.getText().toString();

        if (user.isEmpty()) {
            editUser.setError(strUserValidation);
            valid = false;
        } else {
            editUser.setError(null);
        }

        if (password.isEmpty()) {
            editPassword.setError(strPasswordValidation);
            valid = false;
        } else {
            editPassword.setError(null);
        }

        return valid;
    }
}