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

package de.welthungerhilfe.cgm.scanner.helper;

/**
 * Created by Emerald on 2/19/2018.
 */

public class AppConstants {
    public static final int MAX_IMAGE_SIZE = 800;

    public static final String GOOGLE_GEO_URL = "https://maps.googleapis.com/maps/api/geocode/json";
    public static final String STORAGE_ROOT_URL = "gs://child-growth-monitor.appspot.com";
    //public static final String STORAGE_CONSENT_URL = "/data/person/{id}/consent/";
    public static final String STORAGE_CONSENT_URL = "/data/person/{qrcode}/";
    public static final String STORAGE_MEASURE_URL = "/data/person/{id}/measures/";

    public static final String VAL_SEX_FEMALE = "female";
    public static final String VAL_SEX_MALE = "male";
    public static final String VAL_SEX_OTHER = "fluid";

    public static final String VAL_MEASURE_MANUAL = "manual";
    public static final String VAL_MEASURE_AUTO = "v1";

    public static final String EXTRA_QR = "extra_qr";
    public static final String EXTRA_QR_BITMAP = "extra_qr_bitmap";
    public static final String EXTRA_QR_URL = "extra_qr_url";
    public static final String EXTRA_LOCATION = "extra_location";
    public static final String EXTRA_RADIUS = "extra_radius";
    public static final String EXTRA_PERSON_LIST = "extra_person_list";
    public static final String EXTRA_PERSON = "extra_person";
}
