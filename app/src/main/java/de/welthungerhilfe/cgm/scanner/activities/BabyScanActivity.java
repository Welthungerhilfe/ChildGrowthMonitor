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

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import org.greenrobot.eventbus.EventBus;

import java.util.Date;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.fragments.BabyBack0Fragment;
import de.welthungerhilfe.cgm.scanner.fragments.BabyBack1Fragment;
import de.welthungerhilfe.cgm.scanner.fragments.BabyFront0Fragment;
import de.welthungerhilfe.cgm.scanner.fragments.BabyFront1Fragment;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.events.MeasureResult;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

/**
 * Created by Emerald on 2/20/2018.
 */

public class BabyScanActivity extends AppCompatActivity {
    private final String BABY_FRONT_0 = "baby_front_0";
    private final String BABY_FRONT_1 = "baby_front_1";
    private final String BABY_BACK_0 = "baby_back_0";
    private final String BABY_BACK_1 = "baby_back_1";

    private int step = 0;

    private BabyFront0Fragment babyFront0Fragment;
    private BabyFront1Fragment babyFront1Fragment;
    private BabyBack0Fragment babyBack0Fragment;
    private BabyBack1Fragment babyBack1Fragment;

    private Measure measure;

    protected void onCreate(Bundle saveBundle) {
        super.onCreate(saveBundle);
        setContentView(R.layout.activity_baby_scan);

        measure = new Measure();

        babyFront0Fragment = new BabyFront0Fragment();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.container, babyFront0Fragment, BABY_FRONT_0);
        ft.commit();
    }

    public void setFrontHeight(int frontHeight) {
        measure.setHeight(frontHeight);
        gotoNextStep();
    }

    public void setBackHeight(int backHeight) {
        measure.setWeight(backHeight);
        gotoNextStep();
    }

    public void gotoNextStep() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        step ++;

        if (step == 0) {
            ft.replace(R.id.container, babyFront0Fragment, BABY_FRONT_0);
            ft.commit();
        } else if (step == 1) {
            babyFront1Fragment = new BabyFront1Fragment();
            ft.replace(R.id.container, babyFront1Fragment, BABY_FRONT_1);
            ft.commit();
        } else if (step == 2) {
            babyBack0Fragment = new BabyBack0Fragment();
            ft.replace(R.id.container, babyBack0Fragment, BABY_BACK_0);
            ft.commit();
        } else if (step == 3) {
            babyBack1Fragment = BabyBack1Fragment.newInstance(measure.getWeight());
            ft.replace(R.id.container, babyBack1Fragment, BABY_BACK_1);
            ft.commit();
        } else {
            measure.setDate(System.currentTimeMillis());
            EventBus.getDefault().post(new MeasureResult(measure));
            finish();
        }
    }
}
