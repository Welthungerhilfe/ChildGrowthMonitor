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
import android.util.Log;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
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
import de.welthungerhilfe.cgm.scanner.models.Consent;
import de.welthungerhilfe.cgm.scanner.models.Loc;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.models.Person;
import de.welthungerhilfe.cgm.scanner.models.QRNumber;

/**
 * Created by Emerald on 2/19/2018.
 */

public class CreateDataActivity extends BaseActivity {
    private final String TAG = CreateDataActivity.class.getSimpleName();

    public Person person;
    public ArrayList<Measure> measures;
    public ArrayList<Consent> consents;

    public String qrCode;
    public byte[] qrSource;
    public String qrPath;

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
        measures = new ArrayList<>();
        consents = new ArrayList<>();



        if (qrCode != null) {
            checkQR();
        }

        if (person != null) {
            loadMeasures();
            loadConsents();
        } else {
            Log.w(TAG,"person was null");
        }

        setupActionBar();
        initFragments();
        initUI();

        uploadQR();
    }

    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void setupActionBar() {
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        if (person != null)
            actionBar.setTitle("ID: " + person.getQrcode());
        else
            actionBar.setTitle("ID: " + qrCode);
    }

    private void initFragments() {
        if (personalFragment == null)
            personalFragment = PersonalDataFragment.newInstance(this);
        if (measureFragment == null)
            measureFragment = MeasuresDataFragment.newInstance(this);
        if (growthFragment == null)
            growthFragment = GrowthDataFragment.newInstance(this);
    }

    private void initUI() {
        FragmentAdapter adapter = new FragmentAdapter(getFragmentManager());
        adapter.addFragment(personalFragment, "PERSONAL");
        adapter.addFragment(measureFragment, "MEASURES");
        adapter.addFragment(growthFragment, "GROWTH");
        viewpager.setAdapter(adapter);

        tabs.setupWithViewPager(viewpager);
    }

    public void setPersonalData(String name, String surName, long birthday, boolean age, String sex, Loc loc, String guardian) {
        boolean isNew = false;
        if (person == null) {
            if (qrPath == null) {
                Toast.makeText(CreateDataActivity.this, "Still uploading QR code, please try after a while", Toast.LENGTH_SHORT).show();
                return;
            } else {
                isNew = true;

                person = new Person();
                /*
                QRNumber qrNumber = new QRNumber();
                qrNumber.setCode(qrCode);
                ArrayList<String> consents = new ArrayList<>();
                consents.add(qrPath);
                qrNumber.setConsents(consents);
                */
                person.setQrcode(qrCode);
            }
        }

        person.setName(name);
        person.setSurname(surName);
        person.setLastLocation(loc);
        person.setBirthday(birthday);
        person.setGuardian(guardian);
        person.setSex(sex);
        person.setAgeEstimated(age);
        person.setCreated(System.currentTimeMillis());

        if (isNew)
            createPerson();
        else
            updatePerson();
    }

    public void setMeasureData(float height, float weight, float muac, float headCircumference, String additional, Loc location) {
        showProgressDialog();

        final Measure measure = new Measure();
        measure.setDate(System.currentTimeMillis());
        long age = (System.currentTimeMillis() - person.getBirthday()) / 1000 / 60 / 60 / 24;
        measure.setAge(age);
        measure.setHeight(height);
        measure.setWeight(weight);
        measure.setMuac(muac);
        measure.setHeadCircumference(headCircumference);
        measure.setArtifact(additional);
        measure.setLocation(location);
        measure.setType(AppConstants.VAL_MEASURE_MANUAL);

        AppController.getInstance().firebaseFirestore.collection("persons")
                .document(person.getId())
                .collection("measures")
                .add(measure)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        hideProgressDialog();
                        Toast.makeText(CreateDataActivity.this, "Add measure data failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        personalFragment.initUI();
                        measureFragment.addMeasure(measure);
                        growthFragment.setChartData();

                        HashMap<String, Measure> lastMeasure = new HashMap<>();
                        lastMeasure.put("lastMeasure", measure);
                        documentReference.getParent().getParent().set(lastMeasure, SetOptions.merge());

                        if (measure.getLocation() != null) {
                            HashMap<String, Loc> lastLocation = new HashMap<>();
                            lastLocation.put("lastLocation", measure.getLocation());
                            documentReference.getParent().getParent().set(lastLocation, SetOptions.merge());
                        }

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
                        Toast.makeText(CreateDataActivity.this, "Person create failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(final DocumentReference documentReference) {
                        person.setId(documentReference.getId());
                        Map<String, Object> personID = new HashMap<>();
                        personID.put("id", person.getId());
                        documentReference.update(personID);

                        final Consent consent = new Consent();

                        if (qrCode != null)
                            consent.setQrcode(qrCode);
                        else
                            consent.setQrcode(person.getQrcode());

                        // because we uploaded the consent to storage before there was a database
                        // reference we need to recover the timestamp from the filename
                        // in order to use the same timestamp later on to check for a thumbnail
                        String createdString = qrPath.split("_"+consent.getQrcode()+".png")[0].split("data%2Fperson%2F"+qrCode+"%2F")[1];
                        Log.v(TAG,"createdString: "+createdString);
                        Long created = Long.parseLong(createdString);
                        consent.setCreated(created);

                        consent.setConsent(qrPath);

                        documentReference.collection("consents")
                                .add(consent)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        consents.add(0, consent);
                                    }
                                });

                        // Start measuring
                        Intent intent =new Intent(CreateDataActivity.this, ScreenRecordActivity.class);
                        intent.putExtra(AppConstants.EXTRA_PERSON, person);
                        startActivity(intent);
                    }
                });
    }

    private void updatePerson() {
        showProgressDialog();

        AppController.getInstance().firebaseFirestore.collection("persons")
                .document(person.getId())
                .set(person)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        hideProgressDialog();
                        Toast.makeText(CreateDataActivity.this, "Person create failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        hideProgressDialog();
                    }
                });
    }

    private void checkQR() {
        AppController.getInstance().firebaseFirestore.collection("persons")
                .whereEqualTo("qrcode", qrCode)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        boolean exist = false;
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                person = document.toObject(Person.class);
                                exist = true;
                                loadMeasures();
                                loadConsents();
                                break;
                            }
                        }
                        if (exist) {
                            personalFragment.initUI();
                        }
                    }
                });
    }

    private void uploadQR() {
        if (qrSource == null)
            return;
        //String consentPath = AppConstants.STORAGE_CONSENT_URL.replace("{id}", person.getId()) + System.currentTimeMillis() + "_" + qrCode + ".png";
        final String consentPath = AppConstants.STORAGE_CONSENT_URL.replace("{qrcode}",  qrCode) + System.currentTimeMillis() + "_" + qrCode + ".png";
        StorageReference consentRef = AppController.getInstance().storageRootRef.child(consentPath);
        UploadTask uploadTask = consentRef.putBytes(qrSource);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                hideProgressDialog();

                Toast.makeText(CreateDataActivity.this, "Uploading Consent Failed", Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                hideProgressDialog();

                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                qrPath = downloadUrl.toString();

                if (person != null) {
                    final Consent consent = new Consent();
                    consent.setCreated(System.currentTimeMillis());
                    consent.setConsent(qrPath);
                    if (qrCode != null)
                        consent.setQrcode(qrCode);
                    else
                        consent.setQrcode(person.getQrcode());
                    AppController.getInstance().firebaseFirestore.collection("persons")
                            .document(person.getId())
                            .collection("consents")
                            .add(consent)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    consents.add(0, consent);
                                }
                            });
                }
            }
        });
    }

    private void loadMeasures() {
        showProgressDialog();

        if (person != null) {
            AppController.getInstance().firebaseFirestore.collection("persons")
                    .document(person.getId())
                    .collection("measures")
                    .orderBy("date", Query.Direction.DESCENDING)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            hideProgressDialog();
                            if (task.isSuccessful()) {
                                for (DocumentSnapshot document : task.getResult()) {
                                    Measure measure = document.toObject(Measure.class);

                                    measures.add(measure);
                                }
                            }
                        }
                    });
        }
    }

    private void loadConsents() {
        showProgressDialog();

        if (person != null) {
            AppController.getInstance().firebaseFirestore.collection("persons")
                    .document(person.getId())
                    .collection("consents")
                    .orderBy("created", Query.Direction.DESCENDING)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            hideProgressDialog();
                            if (task.isSuccessful()) {
                                for (DocumentSnapshot document : task.getResult()) {
                                    Consent consent = document.toObject(Consent.class);
                                    consents.add(consent);

                                }
                                if (consents.size() > 0) {
                                    personalFragment.showConsent();
                                }
                            }
                        }
                    });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MeasureResult event) {
        final Measure measure = event.getMeasureResult();
        long age = (System.currentTimeMillis() - person.getBirthday()) / 1000 / 60 / 60 / 24;
        measure.setAge(age);
        measure.setType(AppConstants.VAL_MEASURE_AUTO);

        AppController.getInstance().firebaseFirestore.collection("persons")
                .document(person.getId())
                .collection("measures")
                .add(measure)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        hideProgressDialog();
                        Toast.makeText(CreateDataActivity.this, "Add measure data failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        personalFragment.initUI();
                        measureFragment.addMeasure(measure);
                        growthFragment.setChartData();

                        HashMap<String, Measure> lastMeasure = new HashMap<>();
                        lastMeasure.put("lastMeasure", measure);
                        documentReference.getParent().getParent().set(lastMeasure, SetOptions.merge());
                        HashMap<String, Loc> lastLocation = new HashMap<>();
                        lastLocation.put("lastLocation", measure.getLocation());
                        documentReference.getParent().getParent().set(lastLocation, SetOptions.merge());
                        hideProgressDialog();

                        viewpager.setCurrentItem(1);
                    }
                });
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

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(menuItem);
    }
}
