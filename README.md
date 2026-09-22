# Beep Timer

## Brief Description

The BeepTimer phone application helps a user keep track of short time periods by issuing a brief tone burst and voice message at fixed intervals. This allows the user to track events without continuously staring at the device. This functionality is available in a few existing Android apps, but only as a small side feature as part of a very heavy app.

## Basic Operation

To use BeepTimer, the user starts by selecting a duration option, for example:

* 1-minute duration (beeps every 10 seconds)
* 2-minute duration (beeps every 20 seconds)
* 6-minute duration (beeps every 60 seconds)

In response, the app issues an initial brief tone and voice message ("Starting") and transitions the display to show an active running screen with prominent "End" control (button) and the current interval indicator.

At each fixed interval, the app issues a "burst" of tones (where the number of tones equals the current interval index—e.g., 1 tone for the 1st interval, 2 tones for the 2nd, up to 6 tones for the 6th) followed by a voice message of the elapsed time/checkpoint (e.g., "10", "20", etc.).

If the user hits the "End" control, the timer is stopped, the app issues a concluding message ("Ending Timer"), and the app closes.

When the duration time is reached, the app issues the final tone burst and a concluding voice message ("Timing Complete").
