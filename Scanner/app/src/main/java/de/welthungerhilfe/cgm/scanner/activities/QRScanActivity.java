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

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import de.welthungerhilfe.cgm.scanner.dialogs.ConfirmDialog;
import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.BitmapUtils;
import de.welthungerhilfe.cgm.scanner.views.QRScanView;

/**
 * Created by Emerald on 2/19/2018.
 */

public class QRScanActivity extends AppCompatActivity implements ConfirmDialog.OnConfirmListener, QRScanView.QRScanHandler {
    private final String TAG = QRScanActivity.class.getSimpleName();
    private final int PERMISSION_LOCATION = 0x1000;

    private QRScanView qrScanView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        qrScanView = new QRScanView(this);
        setContentView(qrScanView);

        ConfirmDialog confirmDialog = new ConfirmDialog(this);
        confirmDialog.setMessage("Please make sure to take a photo of the consent form from the parents to avoid legal issues");
        confirmDialog.setConfirmListener(this);
        confirmDialog.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        qrScanView.stopCamera();
    }

    @Override
    public void onConfirm(boolean result) {
        if (result) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, PERMISSION_LOCATION);
            } else {
                qrScanView.setResultHandler(this);
                qrScanView.startCamera();
            }
        } else {
            finish();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_LOCATION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(QRScanActivity.this, "Camera permission required for QR scanning", Toast.LENGTH_LONG).show();
            } else {
                qrScanView.setResultHandler(this);
                qrScanView.startCamera();
            }
        }
    }

    @Override
    public void handleQRResult(String qrCode, byte[] bitmap) {
        Intent intent = new Intent(QRScanActivity.this, CreateDataActivity.class);
        intent.putExtra(AppConstants.EXTRA_QR, qrCode);
        intent.putExtra(AppConstants.EXTRA_QR_BITMAP, BitmapUtils.getRotatedByte(bitmap, 90));
        startActivity(intent);
        finish();
    }
}
