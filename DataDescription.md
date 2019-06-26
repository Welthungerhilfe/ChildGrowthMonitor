# Data description

This document describes the data collected and stored for the development, maintenance and operation of the Child Growth Monitor.

It is updated for each minor version of the product.

## Personal data (person)

Personal data is only transmitted to and stored in the production app backend.
Strong encryption in transmission and storage and compliance with all applicable data protection laws is standard.

### Structured person data*

Structured data is transmitted through Azure Storage Account queue and stored in Azure managed postgresql database

```json
    "birthday": timestamp of childs birthday,
    "created": timestamp of data creation,
    "createdBy": team account email address,
    "deleted": feedback update for backend that data was deleted: true/false,
    "guardian": name of mother/father/guardian,
    "id": see below IDs, for example: "c99eac07f31db1e7_person_1561584793879_fsvTmZYomZUcTCPa",
    "isAgeEstimated": true/false,
    "name": name of child
    "qrcode": standard text qrcode, for example "20190626-create-person-test",
    "sex": "female" or "male",
    "surname": last name of child,
    "timestamp": of last sync to backend,
```

### Consent Binary Large Objects (BLOBs)

Scans of Informed consent in png format for each access via qrcode

TODO: Example

## Measurements (measure)

Manual or scan measurements produced by humans or artificial neural networks (ANNs)

### Structured measure data*

Pseudonymization for ml backend is implement. Anonymization needs at least randomization of personId and qrcode.

```json
    "age": age of child in days,
    "createdBy": team account email address,
    "date": date of creation,
    "deleted": feedback update for backend that data was deleted: true/false,
    "headCircumference": of child in cm float,
    "height": of child in cm float,
    "id": see below IDs, for example: "c99eac07f31db1e7_measure_1561378462939_dcK2otQNnjpwnRTk",
    "location": {
        "address": human readable string, i.e "Straße 5, 97070 Würzburg, Germany",
        "latitude": gps, i.e. 33.123147,
        "longitude": gps, i.e. 9.999999
    },
    "muac": in cm,
    "oedema": true/false,
    "personId":  see below IDs, for example: "c99eac07f31db1e7_person_1561120155568_HcyACrT0TR5RGYgQ",
    "timestamp": of last sync to backend,
    "type": type and version of the result generating artificial neural network, for example "v0.2",
    "visible": is this visible, related to marked for deletion above, true/false,
    "weight": in kg float
```

Scan artifacts are linked to measurement data via qrcode and age.

## Artifacts (artifact)

### Structured artifact data

```json
    "createDate": timestamp of creation of datapoint,
    "createdBy": team account email address,
    "deleted": deleted from device after upload true/false,
    "fileSize": in bytes,
    "hashValue": file integrity checksum,
    "id": see ID below, example: "9e58cfb935e72628_artifact-scan-pcd_1560504390615_mq5NyfVWtSOBCTcs",
    "path": "/storage/emulated/0/Child Growth Monitor Scanner App/reviewtest222/measurements/1560504390615/pc/pc_reviewtest222_1560504390615_100_007.pcd",
    "qrCode": "reviewtest222",
    "status": 202,
    "type": "pcd",
    "uploadDate": 0
```

### Scan Binary Large Objects (BLOBs)

#### Point Cloud Data (PCD)

visualization of examples https://github.com/Welthungerhilfe/ChildGrowthMonitor/issues/47

#### Image data (JPG)**

Faces in the images are blurred before transmission to ml backend.

## Device telemetry (device)

```json
"create_timestamp": timestamp of creation of datapoint,,
    "own_data": {
        "artifact_file_size_mb": current file size of scan artifacts in device storage,
        "artifacts": number of own total artifacts,
        "deleted_artifacts": number of deleted scan artifacts,
        "own_measures": number of measurements taken by same user,
        "own_persons": number of persons created by same user,
        "total_artifact_file_size_mb": total file size of scan artifacts in device storage including deleted files,
        "total_artifacts": number of scan artifacts including deleted
    },
    "owner": team account email address,
    "total_data": {
        "total_measures": total number of measurements stored on the device,
        "total_persons": total number of persons whos information is stored on the device
    },
    "uuid": "9e58cfb935e72628"
```

*data concerning health that is attributable to a child
**data revealing ethnic or racial origin that is attributable to a child

## IDs

As a data scientist working with the database I want a simple way to determine what kind of data point I have when I see just the ID.

Therefore we enforce the following ID schema:
`"UUID of App"_"object type"_"epoch ms"_"random string"`

UUID is randomly generated at installation of app and can be forced to regenerate through reinstall.