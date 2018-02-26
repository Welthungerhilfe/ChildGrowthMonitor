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

package de.welthungerhilfe.cgm.scanner.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import de.welthungerhilfe.cgm.scanner.helper.AppConstants;

/**
 * Created by Emerald on 2/21/2018.
 */

public class BitmapUtils {
    public static void saveBitmap(byte[] imgData) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CGM Scanner");
        if (!mediaStorageDir.exists())
            mediaStorageDir.mkdir();
        File pictureFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ System.currentTimeMillis() + ".png");
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(imgData);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveBitmap(Bitmap bitmap) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CGM Scanner");
        if (!mediaStorageDir.exists())
            mediaStorageDir.mkdir();
        File pictureFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ System.currentTimeMillis() + ".png");
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap getRotatedBitmap(Bitmap bmp, float degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap result = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

        return result;
    }

    public static Bitmap getRotatedBitmap(byte[] data, float degree) {
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        Bitmap rotatedBmp = getRotatedBitmap(bmp, degree);

        return rotatedBmp;
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int w, int h) {
        Bitmap BitmapOrg = bm;
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();
        int newWidth = w;
        int newHeight = h;
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width, height, matrix, true);
        return resizedBitmap;
    }

    public static byte[] getResizedByte(byte[] data, int w, int h) {
        Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
        Bitmap BitmapOrg = bm;
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();
        int newWidth = w;
        int newHeight = h;
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width, height, matrix, true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static byte[] getRotatedByte(Bitmap bmp, float degree) {
        Bitmap rotatedBmp = getRotatedBitmap(bmp, degree);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static byte[] getRotatedByte(byte[] data, float degree) {
        Bitmap rotatedBmp = getRotatedBitmap(data, degree);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static byte[] getResizedByte(Bitmap bm, int w, int h) {
        Bitmap BitmapOrg = bm;
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();
        int newWidth = w;
        int newHeight = h;
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width, height, matrix, true);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static Bitmap getAcceptableBitmap(Bitmap bmp) {
        float ratio = 0;
        float scaledWidth = 0, scaledHeight = 0;
        if (bmp.getHeight() > AppConstants.MAX_IMAGE_SIZE) {
            ratio = (float)AppConstants.MAX_IMAGE_SIZE / bmp.getHeight();
            scaledWidth = bmp.getWidth() * ratio;
            scaledHeight= bmp.getHeight() * ratio;
        }
        if (bmp.getWidth() > AppConstants.MAX_IMAGE_SIZE) {
            ratio = (float) AppConstants.MAX_IMAGE_SIZE / bmp.getWidth();
            scaledWidth = bmp.getWidth() * ratio;
            scaledHeight= bmp.getHeight() * ratio;
        }

        if (ratio == 0)
            return bmp;
        else
            return getResizedBitmap(bmp, (int)scaledWidth, (int)scaledHeight);
    }

    public static byte[] getByteData(Bitmap bmp) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }
}
