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
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.appeaser.sublimepickerlibrary.datepicker.SelectedDate;
import com.appeaser.sublimepickerlibrary.recurrencepicker.SublimeRecurrencePicker;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.OnClickListener;
import com.orhanobut.dialogplus.ViewHolder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.adapters.RecyclerDataAdapter;
import de.welthungerhilfe.cgm.scanner.dialogs.DateRangePickerDialog;
import de.welthungerhilfe.cgm.scanner.helper.SessionManager;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class MainActivity extends BaseActivity implements RecyclerDataAdapter.OnPersonDetail, DateRangePickerDialog.Callback, EventListener<QuerySnapshot> {
    private final String TAG = MainActivity.class.getSimpleName();
    private final int REQUEST_LOCATION = 0x1000;

    private int sortType = 0;
    private int diffDays = 0;
    private ArrayList<Person> personList = new ArrayList<>();

    @OnClick(R.id.fabCreate)
    void createData(FloatingActionButton fabCreate) {
        Crashlytics.log("Add person by QR");
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
                                dialog.getHolderView().findViewById(R.id.imgSortClear).setVisibility(View.INVISIBLE);
                                dialog.dismiss();

                                doSortByDate();
                                break;
                            case R.id.rytSortLocation:
                                dialog.getHolderView().findViewById(R.id.imgSortDate).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortLocation).setVisibility(View.VISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortWasting).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortStunting).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortClear).setVisibility(View.INVISIBLE);
                                dialog.dismiss();

                                doSortByLocation();
                                break;
                            case R.id.rytSortWasting:
                                dialog.getHolderView().findViewById(R.id.imgSortDate).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortLocation).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortWasting).setVisibility(View.VISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortStunting).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortClear).setVisibility(View.INVISIBLE);

                                txtSortCase.setText("worst weight/height on top");
                                dialog.dismiss();

                                doSortByWasting();
                                break;
                            case R.id.rytSortStunting:
                                dialog.getHolderView().findViewById(R.id.imgSortDate).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortLocation).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortWasting).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortStunting).setVisibility(View.VISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortClear).setVisibility(View.INVISIBLE);

                                txtSortCase.setText("worst height/age on top");
                                dialog.dismiss();

                                doSortByStunting();
                                break;
                            case R.id.rytSortClear:
                                dialog.getHolderView().findViewById(R.id.imgSortDate).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortLocation).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortWasting).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortStunting).setVisibility(View.INVISIBLE);
                                dialog.getHolderView().findViewById(R.id.imgSortClear).setVisibility(View.VISIBLE);

                                txtSortCase.setText("No filter");
                                dialog.dismiss();

                                clearFilters();
                                break;
                        }
                    }
                })
                .create();
        TextView txtSortDate = sortDialog.getHolderView().findViewById(R.id.txtSortDate);
        txtSortDate.setText("last " + Integer.toString(diffDays) + " days");

        TextView txtSortLocation = sortDialog.getHolderView().findViewById(R.id.txtSortLocation);
        if (session.getLocation().getAddress().equals("")) {
            txtSortLocation.setText("last location not available");
        } else {
            txtSortLocation.setText(session.getLocation().getAddress());
        }

        ImageView imgSortDate = sortDialog.getHolderView().findViewById(R.id.imgSortDate);
        ImageView imgSortLocation = sortDialog.getHolderView().findViewById(R.id.imgSortLocation);
        ImageView imgSortWasting = sortDialog.getHolderView().findViewById(R.id.imgSortWasting);
        ImageView imgSortStunting = sortDialog.getHolderView().findViewById(R.id.imgSortStunting);
        ImageView imgSortClear = sortDialog.getHolderView().findViewById(R.id.imgSortClear);
        switch (sortType) {
            case 0:
                imgSortClear.setVisibility(View.VISIBLE);
                break;
            case 1:
                imgSortDate.setVisibility(View.VISIBLE);
                break;
            case 2:
                imgSortLocation.setVisibility(View.VISIBLE);
                break;
            case 3:
                imgSortWasting.setVisibility(View.VISIBLE);
                break;
            case 4:
                imgSortStunting.setVisibility(View.VISIBLE);
                break;
        }

        sortDialog.show();
    }

    @BindView(R.id.refreshLayout)
    SwipeRefreshLayout refreshLayout;
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
    @BindView(R.id.txtNoPerson)
    TextView txtNoPerson;

    private ActionBarDrawerToggle mDrawerToggle;

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        session = new SessionManager(MainActivity.this);

        setupSidemenu();
        setupActionBar();

        initUI();

        /*
        AppController.getInstance().firebaseFirestore.collection("persons")
                //.orderBy("created", Query.Direction.DESCENDING)
                .addSnapshotListener(this);
        */

        showProgressDialog();

        loadData();
    }

    private void initUI() {
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                adapterData.resetData(new ArrayList<Person>());
                loadData();
            }
        });

        adapterData = new RecyclerDataAdapter(this, new ArrayList<Person>());
        adapterData.setPersonDetailListener(this);
        recyclerData.setAdapter(adapterData);
        recyclerData.setLayoutManager(new LinearLayoutManager(MainActivity.this));
    }

    private void setupSidemenu() {
        navMenu.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menuHome:
                        break;
                    case R.id.menuLogout:
                        AppController.getInstance().firebaseAuth.signOut();
                        session.setSigned(false);
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                        break;
                }
                drawerLayout.closeDrawers();
                return true;
            }
        });
        View headerView = navMenu.getHeaderView(0);
        TextView txtUsername = headerView.findViewById(R.id.txtUsername);
        txtUsername.setText(AppController.getInstance().firebaseUser.getEmail());
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

    private void loadData() {
        AppController.getInstance().firebaseFirestore.collection("persons")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        hideProgressDialog();
                        boolean noData = true;
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                Person person = document.toObject(Person.class);

                                adapterData.addPerson(person);
                                noData = false;
                            }
                        }
                        if (noData) {
                            recyclerData.setVisibility(View.GONE);
                            txtNoPerson.setVisibility(View.VISIBLE);
                        } else {
                            recyclerData.setVisibility(View.VISIBLE);
                            txtNoPerson.setVisibility(View.GONE);
                        }
                        refreshLayout.setRefreshing(false);
                    }
                });
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
        sortType = 1;

        DateRangePickerDialog dateRangePicker = new DateRangePickerDialog();
        dateRangePicker.setCallback(this);
        dateRangePicker.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        dateRangePicker.show(getFragmentManager(), "DATE_RANGE_PICKER");
    }

    private void doSortByLocation() {
        sortType = 2;

        Intent intent = new Intent(MainActivity.this, LocationSearchActivity.class);
        startActivityForResult(intent, REQUEST_LOCATION);
    }

    private void doSortByWasting() {
        sortType = 3;

        adapterData.setWastingFilter();
    }

    private void doSortByStunting() {
        sortType = 4;

        adapterData.setStuntingFilter();
    }

    private void clearFilters() {
        sortType = 0;
        adapterData.clearFitlers();
    }

    public void onActivityResult(int reqCode, int resCode, Intent result) {
        if (reqCode == REQUEST_LOCATION && resCode == Activity.RESULT_OK) {
            int radius = result.getIntExtra(AppConstants.EXTRA_RADIUS, 0);

            adapterData.setLocationFilter(session.getLocation(), radius);
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
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    adapterData.search(query);
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onPersonDetail(Person person) {
        Intent intent = new Intent(MainActivity.this, CreateDataActivity.class);
        intent.putExtra(AppConstants.EXTRA_PERSON, person);
        startActivity(intent);
    }

    @Override
    public void onDateTimeRecurrenceSet(SelectedDate selectedDate, int hourOfDay, int minute, SublimeRecurrencePicker.RecurrenceOption recurrenceOption, String recurrenceRule) {
        Calendar start = selectedDate.getStartDate();
        Calendar end = selectedDate.getEndDate();

        diffDays = (int) (end.getTimeInMillis() - start.getTimeInMillis()) / 1000 / 60 / 60 / 24;
        long startDate = start.getTimeInMillis();
        long endDate = end.getTimeInMillis();
        if (start.getTimeInMillis() == end.getTimeInMillis()) {
            diffDays = 1;
            Date date = new Date(start.get(Calendar.YEAR) - 1900, start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH), 0, 0, 0);

            startDate = date.getTime();
            endDate = startDate + (3600 * 24 - 1) * 1000;
        }

        txtSortCase.setText("Last Scans (" + Integer.toString(Math.abs(diffDays)) + " days)");
        adapterData.setDateFilter(startDate, endDate);
    }

    @Override
    public void onEvent(QuerySnapshot snapshot, FirebaseFirestoreException e) {
        List<DocumentChange> documents = snapshot.getDocumentChanges();
        for (DocumentChange change: documents) {
            Person person = change.getDocument().toObject(Person.class);
            if (change.getType().equals(DocumentChange.Type.ADDED)) {
                adapterData.addPerson(person);
            } else if (change.getType().equals(DocumentChange.Type.MODIFIED)) {
                adapterData.updatePerson(person);
            } else if (change.getType().equals(DocumentChange.Type.REMOVED)) {
                adapterData.removePerson(person);
            }
            if (adapterData.getItemCount() == 0) {
                recyclerData.setVisibility(View.GONE);
                txtNoPerson.setVisibility(View.VISIBLE);
            } else {
                recyclerData.setVisibility(View.VISIBLE);
                txtNoPerson.setVisibility(View.GONE);
            }
        }
    }
}
