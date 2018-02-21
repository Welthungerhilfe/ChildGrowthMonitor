/**
 *  Child Growth Monitor - quick and accurate data on malnutrition
 *  Copyright (c) $today.year Welthungerhilfe Innovation
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.welthungerhilfe.cgm.scanner.activities;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.borax12.materialdaterangepicker.date.DatePickerDialog;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.OnClickListener;
import com.orhanobut.dialogplus.ViewHolder;

import java.util.ArrayList;
import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.adapters.RecyclerDataAdapter;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;

public class MainActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {
    private final int REQUEST_LOCATION = 0x1000;

    @OnClick(R.id.fabCreate)
    void createData(FloatingActionButton fabCreate) {
        startActivity(new Intent(MainActivity.this, QRScanActivity.class));
    }

    @OnClick(R.id.txtSort)
    void doSort(TextView txtSort) {
        ViewHolder viewHolder = new ViewHolder(R.layout.dialog_sort);
        DialogPlus sortDialog = DialogPlus.newDialog(MainActivity.this)
                .setContentHolder(viewHolder)
                .setCancelable(true)
                .setInAnimation(R.anim.abc_fade_in)
                .setOutAnimation(R.anim.abc_fade_out)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(DialogPlus dialog, View view) {
                        switch (view.getId()) {
                            case R.id.rytSortDate:
                                dialog.getHolderView().findViewById(R.id.imgSortDate).setVisibility(View.VISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortLocation).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortWasting).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortStunting).setVisibility(View.INVISIBLE);
                                dialog.dismiss();

                                doSortByDate();
                                break;
                            case R.id.rytSortLocation:
                                dialog.getHolderView().findViewById(R.id.imgSortDate).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortLocation).setVisibility(View.VISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortWasting).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortStunting).setVisibility(View.INVISIBLE);
                                dialog.dismiss();

                                doSortByLocation();
                                break;
                            case R.id.rytSortWasting:
                                dialog.getHolderView().findViewById(R.id.imgSortDate).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortLocation).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortWasting).setVisibility(View.VISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortStunting).setVisibility(View.INVISIBLE);

                                txtSortCase.setText("worst weight/height on top");
                                dialog.dismiss();
                                break;
                            case R.id.rytSortStunting:
                                dialog.getHolderView().findViewById(R.id.imgSortDate).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortLocation).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortWasting).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortStunting).setVisibility(View.VISIBLE);

                                txtSortCase.setText("worst height/age on top");
                                dialog.dismiss();
                                break;
                        }
                    }
                })
                .create();

        sortDialog.show();
    }

    @BindView(R.id.recyclerData)
    RecyclerView recyclerData;
    RecyclerDataAdapter adapterData;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.drawer)
    DrawerLayout drawerLayout;
    @BindView(R.id.navMenu)
    NavigationView navMenu;
    @BindView(R.id.txtSortCase)
    TextView txtSortCase;

    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        setupSidemenu();
        setupActionBar();

        initUI();
    }

    private void initUI() {
        adapterData = new RecyclerDataAdapter(this, new ArrayList<Person>());
        recyclerData.setAdapter(adapterData);
        recyclerData.setLayoutManager(new LinearLayoutManager(MainActivity.this));
    }

    private void setupSidemenu() {
        navMenu.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                drawerLayout.closeDrawers();
                return true;
            }
        });
    }

    private void setupActionBar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle("All Scans");

        mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };

        drawerLayout.addDrawerListener(mDrawerToggle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void doSortByDate() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog dateRangePickerDlg = DatePickerDialog.newInstance(
                MainActivity.this,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        dateRangePickerDlg.show(getFragmentManager(), "Datepickerdialog");
    }

    private void doSortByLocation() {
        Intent intent = new Intent(MainActivity.this, LocationSearchActivity.class);
        startActivityForResult(intent, REQUEST_LOCATION);
    }

    private void doSortByWasting() {

    }

    private void doSortByStunting() {

    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth, int yearEnd, int monthOfYearEnd, int dayOfMonthEnd) {
        Calendar from = Calendar.getInstance();
        from.set(year, monthOfYear, dayOfMonth);

        Calendar to = Calendar.getInstance();
        to.set(yearEnd, monthOfYearEnd, dayOfMonthEnd);

        int diffDays = from.compareTo(to) / 1000 / 60 / 60 / 24;
        txtSortCase.setText("Last Scans (" + Integer.toString(diffDays) + " days)");
    }

    public void onActivityResult(int reqCode, int resCode, Intent result) {
        if (reqCode == REQUEST_LOCATION && resCode == Activity.RESULT_OK) {
            ArrayList<Person> personList = (ArrayList<Person>) result.getSerializableExtra(AppConstants.EXTRA_PERSON_LIST);
            adapterData.resetData(personList);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_search, menu);

        MenuItem searchItem = menu.findItem(R.id.actionSearch);
        SearchManager searchManager = (SearchManager) MainActivity.this.getSystemService(Context.SEARCH_SERVICE);

        SearchView searchView = null;
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
        }
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(MainActivity.this.getComponentName()));
        }

        return super.onCreateOptionsMenu(menu);
    }
}
