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
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;

/**
 * Created by Emerald on 2/20/2018.
 */

public class BodySelectActivity extends AppCompatActivity {

    @OnClick(R.id.btnBaby)
    void scanBaby(Button btnBaby) {
        startActivity(new Intent(BodySelectActivity.this, BabyScanActivity.class));

        finish();
    }
    @OnClick(R.id.btnInfant)
    void scanInfant(Button btnInfant) {
        startActivity(new Intent(BodySelectActivity.this, InfantScanActivity.class));

        finish();
    }

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_body_select);

        ButterKnife.bind(this);
    }
}
