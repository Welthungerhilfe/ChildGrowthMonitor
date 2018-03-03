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

import android.app.FragmentTransaction;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;

import org.greenrobot.eventbus.EventBus;

import java.util.Date;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.fragments.InfantBackFragment;
import de.welthungerhilfe.cgm.scanner.fragments.InfantFrontFragment;
import de.welthungerhilfe.cgm.scanner.fragments.InfantTurnFragment;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.events.MeasureResult;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

/**
 * Created by Emerald on 2/20/2018.
 */

public class InfantScanActivity extends AppCompatActivity {
    private final String INFANT_FRONT = "infant_front";
    private final String INFANT_TRUN = "infant_turn";
    private final String INFANT_BACK = "infant_back";

    private Measure measure;

    private int step = 0;

    private InfantFrontFragment infantFrontFragment;
    private InfantTurnFragment infantTurnFragment;
    private InfantBackFragment infantBackFragment;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_infant_scan);

        measure = new Measure();

        infantFrontFragment = new InfantFrontFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.container, infantFrontFragment, INFANT_FRONT);
        ft.commit();
    }

    public void setFrontHeight(float height) {
        measure.setHeight(height);
        gotoNextStep();
    }

    public void setBackHeight(float height) {
        measure.setWeight(height);
        gotoNextStep();
    }

    public void gotoNextStep() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();

        step ++;

        if (step == 0) {
            ft.replace(R.id.container, infantFrontFragment, INFANT_FRONT);
            ft.commit();
        } else if (step == 1) {
            infantTurnFragment = new InfantTurnFragment();
            ft.replace(R.id.container, infantTurnFragment, INFANT_TRUN);
            ft.commit();
        } else if (step == 2) {
            infantBackFragment = new InfantBackFragment();
            ft.replace(R.id.container, infantBackFragment, INFANT_BACK);
            ft.commit();
        } else {
            measure.setDate(System.currentTimeMillis());
            EventBus.getDefault().post(new MeasureResult(measure));
            finish();
        }
    }
}
