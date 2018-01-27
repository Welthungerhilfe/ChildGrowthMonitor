[Child Growth Monitor](https://ChildGrowthMonitor.org)
=======

Contributing to Zero Hunger through quick, accurate data on malnutrition.

<!-- TOC depthFrom:1 depthTo:3 withLinks:1 updateOnSave:1 orderedList:0 -->

- [Problem](#problem)
- [Solution](#solution)
	- [Mobile App](#mobile-app)
	- [Backend](#backend)
	- [Machine Learning v1.0](#machine-learning-v10)
	- [Current Machine Learning solution](#current-machine-learning-solution)
	- [Machine Learning Dev Process](#machine-learning-dev-process)
- [Data](#data)
	- [Child Data](#child-data)
	- [Scan Data](#scan-data)
	- [Scan artefacts](#scan-artefacts)
- [Scanning Process](#scanning-process)
	- [Top-Down approach front](#top-down-approach-front)
	- [Scan from back](#scan-from-back)
	- [Circular aproach](#circular-aproach)
	- [Scan of hands](#scan-of-hands)

<!-- /TOC -->

## Problem

Hunger or malnutrition is not simply the lack of food, it is usually a more complex health issue.
Parents often don't know that their children are malnurished and take measures too late. 
Current standardized measurements done by aid organisations and governmental health workers are time consuming and expensive.
Children are moving, accurate measurement, especially of height, is often not possible.

Bottomline: accurate data on the nutritional status of children is unrelieble or non existent

## Solution

We are providing a game changing solution to detect malnutrition of children and we make it Open Source to let everyone participate.


### Mobile App

The mobile app provides authenticated users an interface to scan children in 3D with consent of the parents and upload all collected data to the secure backend.

Currently the App works through person detection and pose estimation and overlaying the this information of the position of 14 points on the body of the child with the 3D point cloud from the Tango API.

The next iteration for the Pilot will guide the user to scan the child in a way that a quick, accurate measurement can be taken. This will involve data of the camera pose, point clouds and RGB video. 

#### Hardware requirements

- Currently Google Project Tango devices only
- In the future probably all devices with ARkit/ARcore capabilities (iPhone 6s and newer, 100 million Android devices)

#### Authentication

Users can authenticate themselves via username and password or Google OAuth. This enables access to download the latest neural network and upload data to Firebase Storage and Database.


#### Screens

Also see this [UX Prototype](https://childgrowthmonitor.org/protoio-CGM-html/frame.html)

1. Login/Register

2. Register

3. Camera
  - Scan QR Code (PK - Private/Primary Key)

4. Child Data
  - Enter data of parents and child

5. Camera
  - Scan Child

6. Data
 - Enter additional Data
 - Enter traditional Measurements

7. Scan Result

8. Child History / Overview

**User eXperience (UX)**

- Agumented Reality Userinterface guides through the scanning Process
- Scanning with instant visual feedback
- for the prototype the results of the scanning process will be shown only after input of the traditional measurements and only if more or less accurate


### Backend

Backend is implemented in Google Firebase using Authentication, Database, Storage and Hosting for the Website. 

#### Authentication

Authentication is done via Email-address and password or Google OAuth. 

#### Usermanagement

Users have to be activated by admin to download the current neural networks and upload data.
Registration can be done via mobile app or the website via Firebase Functions.

#### Rights/Roles

Access to data is granted after scanning the key from a letter of consent of the parents.

#### Organisations

not implemented for Pilot

#### Database

Firebase Database is used for structured data. 

#### Storage

Storage is used for large objects such as rgb video and maybe point clouds.



### Machine Learning v1.0

There are many possibilties for developing useful neural networks.

#### Predict height of a person

An accurate prediction of the height of a human is priority number one. Goal is to do an 99,5% accurate prediction, so that we can measure a child of 100cm height with an error of +/- 5mm.

To reconstruct a 3d model of a child or of the skeleton is a non-trivial task using multiple point clouds of a moving child. Using a single point cloud probably won't be accurate enough.

A promising approach could be to input the point clouds, the device pose for camera position and rgb video into a neural networks, to do preprocessing or get the result.

Helpful research has been done in
- 3d point cloud segmentation through labeling the points
- building a spatio-temporal graph for human pose detection
- ... 

#### Predict weight of a person

Predicting the weight of a person is the secondary goal to do "traditional" standardized measurements only using a smartphone without further hardware.

#### Classifying SAM, wasting, stunting, overweight

A promising approach could be to build a classifier to identify health issues.
Downside to this is that without traditional measurements it is not possible to verify the decisions done by the classifier. 

### Current Machine Learning solution

#### Human Pose Detection

Video Sequence ->
1. Body Part Detection
2. Spatio-Temporal Graph
3. Inference
-> Pose Tracks


#### Person Detection

Bottom Up:

1. fully convolutional Resnet-101 [He et al. 2016]
-> Part Propability Scoremaps (heatmaps)
aka. where are Shoulders, Knees, Wrists, Feet, ...

2. Discretize Scoremaps with non-maximum suppression (NMS)

TODO: Advantages of Top-Down approach?

#### Pose Estimation

The current resnet with 101 layers detects 12 joints + forehead and chin of a Person

Spatio-Temporal Graph G = (D, E)
- Part Detections D
- Spatial edges Es within one Frame
- Temporal edges Et across frames
	- Provides Distance Features aka euclidean distance between body parts

--> Bottom Up Sparse Part Connectivity
--> Euclidean Distance beetween Body Parts?

#### People Tracking

- ResNet-101


### Machine Learning Dev Process

#### Gather data

Gathering the data is done via the mobile app.

#### Data Preparation

*   mix Data
*   Visualize to find correlations and imbalances between underweight and not...
*   Split in Training and Evaluation Data

#### Choosing a Model

#### Training

y = m * x + b

output = slope * input + y-intercept

Training m and b

m als Matrix = weights
b als Matrix = biases

#### Evaluation

80% Training Data
20% Evaluation Data

#### Parameter Tuning

hyperparameter
tune initial parameters, training cycles etc.
when is the result good enough?

#### Prediction

Prediction should work offline in the mobile device as well as in the cloud based machine learning system.

## Data

### Child Data
- ID
- boolean Consent of Parents
- docref CoP
- name
- date of birth
- month of birth

### Scan Data
- reference to child
- Date
- GPS coordinates
- Image ref "passport" picture
- Age in month

### Scan artefacts
- reference to scan
- RGB Video
- 3D point clouds with timestamp and 4 floats XYZ in meters and Confidence from 0-1
- device pose with timestamp

## Scanning Process

The scanning process can be broken down to different parts. We will evaluate results to find the best way to gather necessary data. Children are wearing underwear.

### Top-Down approach front

Scan starts at face, goes down to feet and back up again.

### Scan from back

Scan starts at the back of the head, goes down over back to feet and back up again.

### Circular aproach

For getting more information about the volume of the body and thus maybe a more accurate prediction of the weight of the child, a circular scanning process could be helpful.

This can be a seperate second scanning process, collecting additional data to the scan for heigth.

### Scan of hands

Hands can be a good indicator for malnutrition and are not as sensitive from a privacy perspective.  
