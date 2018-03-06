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
    public static final String VAL_MEASURE_AUTO = "v0.1";

    public static final String EXTRA_QR = "extra_qr";
    public static final String EXTRA_QR_BITMAP = "extra_qr_bitmap";
    public static final String EXTRA_QR_URL = "extra_qr_url";
    public static final String EXTRA_LOCATION = "extra_location";
    public static final String EXTRA_RADIUS = "extra_radius";
    public static final String EXTRA_PERSON_LIST = "extra_person_list";
    public static final String EXTRA_PERSON = "extra_person";

    // Workflow
    public static final int CHOOSE_BABY_OR_INFANT = 0;
    public static final int LYING_BABY_SCAN = 100;
    public static final int BABY_FULL_BODY_FRONT_ONBOARDING = 101;
    public static final int BABY_FULL_BODY_FRONT_SCAN = 102;
    public static final int BABY_FULL_BODY_FRONT_RECORDING = 103;
    public static final int BABY_LEFT_RIGHT_ONBOARDING = 104;
    public static final int BABY_LEFT_RIGHT_SCAN = 105;
    public static final int BABY_LEFT_RIGHT_RECORDING = 106;
    public static final int BABY_FULL_BODY_BACK_ONBOARDING = 107;
    public static final int BABY_FULL_BODY_BACK_SCAN = 108;
    public static final int BABY_FULL_BODY_BACK_RECORDING = 109;
    public static final int STANDING_INFANT_SCAN = 200;
    public static final int INFANT_FULL_BODY_FRONT_ONBOARDING = 201;
    public static final int INFANT_FULL_BODY_FRONT_SCAN = 202;
    public static final int INFANT_FULL_BODY_FRONT_RECORDING = 203;
    public static final int INFANT_360_TURN_ONBOARDING = 204;
    public static final int INFANT_360_TURN_SCAN = 205;
    public static final int INFANT_360_TURN_RECORDING = 206;
    public static final int INFANT_FRONT_UP_DOWN_ONBOARDING = 207;
    public static final int INFANT_FRONT_UP_DOWN_SCAN = 208;
    public static final int INFANT_FRONT_UP_DOWN_RECORDING = 209;
    public static final int INFANT_BACK_UP_DOWN_ONBOARDING = 210;
    public static final int INFANT_BACK_UP_DOWN_SCAN = 211;
    public static final int INFANT_BACK_UP_DOWN_RECORDING = 212;

}
