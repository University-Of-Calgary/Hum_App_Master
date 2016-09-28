# The Ranchlands Hum Application - A community noise detection and analysis application in an Android app.

To refer to the original Hum Application developed, please visit the following repository -> https://github.com/orchidas/Hum-App

The following additions were made during the course of my internship as a Mitacs Globalink Intern at the University of Calgary '16 at the SMILE lab under the supervision of Dr. Mike Smith, Professor, Department of Electrical and Computer Engineering, UofC. 

Added the functionality to calibrate the frequency response of an inexpensive android microphone by calibrating the internal microphone against an industry grade external microphone. The industry microphone used for the purpose is a  Mic W i436.

The task of recording community noise nuisances using multiple microphones (especially inexpensive android microphones) is challenging because of the limitations associated with the internal microphone of an inexpensive android device. When multiple sound recording equipments are used, the frequency response generated using individual microphones maybe different even for the same noise due to the absence of calibration with the android microphones. 

This application tackles the problem of calibrating the microphone of an android device using an external industry microphone. 

A constant noise producing source (e.g. white noise) is recorded by first plugging in the external microphone and then making a recording using the android device's internal microphone. The calibrated values are generated as the ratio of the recordings made using the external microphone and the internal microphone. 
