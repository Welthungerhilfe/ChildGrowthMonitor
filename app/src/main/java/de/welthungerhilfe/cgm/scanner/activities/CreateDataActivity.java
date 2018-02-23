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

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.welthungerhilfe.cgm.scanner.AppController;
import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.adapters.FragmentAdapter;
import de.welthungerhilfe.cgm.scanner.fragments.GrowthDataFragment;
import de.welthungerhilfe.cgm.scanner.fragments.MeasuresDataFragment;
import de.welthungerhilfe.cgm.scanner.fragments.PersonalDataFragment;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.helper.events.MeasureResult;
import de.welthungerhilfe.cgm.scanner.models.Loc;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.models.QRNumber;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

/**
 * Created by Emerald on 2/19/2018.
 */

public class CreateDataActivity extends BaseActivity {
    private final String TAG = CreateDataActivity.class.getSimpleName();

    public Person person;
    public String qrCode;
    public byte[] qrSource;

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
        qrSource = getIntent().getByteArrayExtra(AppConstants.EXTRA_QR_BITMAP);

        person = (Person) getIntent().getSerializableExtra(AppConstants.EXTRA_PERSON);

        setupActionBar();
        initFragments();
        initUI();

        checkQR();
    }

    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void setupActionBar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (person != null)
            actionBar.setTitle("ID: " + person.getId());
        else
            actionBar.setTitle("ID: " + "XXXXXXXXXX");
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

    public void setPersonalData(String name, String surName, String birthday, int age, String sex, Loc loc, String guardian) {
        person = new Person();
        person.setName(name);
        person.setSurname(surName);
        person.setBirthday(birthday);
        person.setAge(age);
        person.setSex(sex);
        person.setLastLocation(loc);
        person.setGuardian(guardian);
        person.setCreated(System.currentTimeMillis());

        createPerson();
    }

    public void setMeasureData(float height, float weight, float muac, String additional) {
        showProgressDialog();

        final Measure measure = new Measure();
        measure.setDate(System.currentTimeMillis());
        measure.setHeight(height);
        measure.setWeight(weight);
        measure.setMuac(muac);
        measure.setArtifact(additional);
        measure.setType("manual");

        AppController.getInstance().firebaseFirestore.collection("persons")
                .document(person.getId())
                .update("measures", measure)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        hideProgressDialog();
                        Toast.makeText(CreateDataActivity.this, "Add measure data failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        List<Measure> measures = person.getMeasures();
                        measures.add(measure);
                        person.setMeasures(measures);

                        hideProgressDialog();
                    }
                });
    }

    private void createPerson() {
        showProgressDialog();

        AppController.getInstance().firebaseFirestore.collection("persons")
                .add(person)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        hideProgressDialog();
                        Toast.makeText(CreateDataActivity.this, "Person created failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(final DocumentReference documentReference) {
                        person.setId(documentReference.getId());

                        String consentPath = AppConstants.STORAGE_CONSENT_URL.replace("{id}", person.getId()) + System.currentTimeMillis() + "_" + qrCode + ".png";
                        StorageReference consentRef = AppController.getInstance().storageRootRef.child(consentPath);
                        UploadTask uploadTask = consentRef.putBytes(qrSource);
                        uploadTask.addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                hideProgressDialog();
                                documentReference.set(person);
                                getSupportActionBar().setTitle("ID: " + person.getId());

                                Toast.makeText(CreateDataActivity.this, "Uploading Consent Failed", Toast.LENGTH_SHORT).show();

                                // Start measuring
                                startActivity(new Intent(CreateDataActivity.this, BodySelectActivity.class));
                            }
                        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                hideProgressDialog();

                                Uri downloadUrl = taskSnapshot.getDownloadUrl();

                                QRNumber qrNumber = new QRNumber();
                                qrNumber.setCode(qrCode);
                                qrNumber.setConsent(downloadUrl.toString());

                                person.setQrNumber(qrNumber);
                                documentReference.set(person);
                                getSupportActionBar().setTitle("ID: " + person.getId());

                                // Start measuring
                                startActivity(new Intent(CreateDataActivity.this, BodySelectActivity.class));
                            }
                        });
                    }
                });
    }

    public void checkQR() {
        AppController.getInstance().firebaseFirestore.collection("persons")
                .whereEqualTo("qrNumber.code", qrCode)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        boolean exist = false;
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                person = document.toObject(Person.class);
                                exist = true;
                                break;
                            }
                        }
                        if (exist) {
                            getSupportActionBar().setTitle("ID: " + person.getId());
                            personalFragment.initUI();
                        }
                    }
                });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MeasureResult event) {
        Measure measure = event.getMeasureResult();

        measureFragment.addMeasure(measure);
        viewpager.setCurrentItem(1);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_search, menu);

        MenuItem searchItem = menu.findItem(R.id.actionSearch);
        SearchManager searchManager = (SearchManager) CreateDataActivity.this.getSystemService(Context.SEARCH_SERVICE);

        SearchView searchView = null;
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
        }
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(CreateDataActivity.this.getComponentName()));
        }

        return super.onCreateOptionsMenu(menu);
    }
}
