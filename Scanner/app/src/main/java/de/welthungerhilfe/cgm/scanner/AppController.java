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

package de.welthungerhilfe.cgm.scanner;

import android.app.Application;
import android.os.StrictMode;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.Utils;

public class AppController extends Application {
    public static final String TAG = AppController.class.getSimpleName();

    private static AppController mInstance;

    public FirebaseAuth firebaseAuth;
    public FirebaseUser firebaseUser;

    public FirebaseStorage firebaseStorage;
    public StorageReference storageRootRef;

    public FirebaseFirestore firebaseFirestore;

    @Override
    public void onCreate() {
        super.onCreate();

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        //Utils.overrideFont(getApplicationContext(), "SERIF", "roboto.ttf");

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        firebaseStorage = FirebaseStorage.getInstance(AppConstants.STORAGE_ROOT_URL);
        storageRootRef = firebaseStorage.getReference();

        firebaseFirestore = FirebaseFirestore.getInstance();

        mInstance = this;
    }

    public static synchronized AppController getInstance() {
        return mInstance;
    }

    public void prepareFirebaseUser() {
        firebaseUser = firebaseAuth.getCurrentUser();
    }
}
