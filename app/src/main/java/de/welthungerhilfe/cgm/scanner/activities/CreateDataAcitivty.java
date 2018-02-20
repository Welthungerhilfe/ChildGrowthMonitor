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
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.adapters.FragmentAdapter;
import de.welthungerhilfe.cgm.scanner.fragments.GrowthDataFragment;
import de.welthungerhilfe.cgm.scanner.fragments.MeasuresDataFragment;
import de.welthungerhilfe.cgm.scanner.fragments.PersonalDataFragment;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.events.MeasureResult;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.models.Person;

/**
 * Created by Emerald on 2/19/2018.
 */

public class CreateDataAcitivty extends AppCompatActivity {
    private final String TAG = CreateDataAcitivty.class.getSimpleName();

    private Person person;
    private String qrCode;

    @BindView(R.id.container)
    CoordinatorLayout container;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tabs)
    TabLayout tabs;
    @BindView(R.id.viewpager)
    ViewPager viewpager;

    private PersonalDataFragment personalFragment;
    private MeasuresDataFragment measureFragment;
    private GrowthDataFragment growthFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        ButterKnife.bind(this);

        EventBus.getDefault().register(this);

        qrCode = getIntent().getStringExtra(AppConstants.EXTRA_QR);

        person = (Person) getIntent().getSerializableExtra(AppConstants.EXTRA_PERSON);
        if (person == null)
            person = new Person();

        setupActionBar();

        initFragments();
        initUI();
    }

    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void setupActionBar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("ID: XXXXXXXXXX");
    }

    private void initFragments() {
        if (personalFragment == null)
            personalFragment = new  PersonalDataFragment();
        if (measureFragment == null)
            measureFragment = new MeasuresDataFragment();
        if (growthFragment == null)
            growthFragment = new GrowthDataFragment();
    }

    private void initUI() {
        FragmentAdapter adapter = new FragmentAdapter(getSupportFragmentManager());
        adapter.addFragment(personalFragment, "PERSONAL");
        adapter.addFragment(measureFragment, "MEASURES");
        adapter.addFragment(growthFragment, "GROWTH");
        viewpager.setAdapter(adapter);

        tabs.setupWithViewPager(viewpager);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MeasureResult event) {
        Measure measure = event.getMeasureResult();

        measureFragment.setMeasure(measure);
        viewpager.setCurrentItem(1);
    }
}
