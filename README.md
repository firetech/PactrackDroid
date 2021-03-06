PactrackDroid
======

This is PactrackDroid, an Android application to keep track of parcels sent with
the PostNord, the Swedish and Danish mail service (in Swedish only for now).
Originally developed by Joakim "firetech" Tufvegren, but abandoned in 2011.
"blunden" made updates to the design of the app to fit Android 4.0+ in 2013,
after which the project was un-abandoned and received a major overhaul.

Due to changes in free usage limits of the PostNord API, the project was again
abandoned in March of 2018, and is now to be considered dead. The
[official PostNord Android app](https://play.google.com/store/apps/details?id=se.postnord.private)
does what PactrackDroid did, but better, and also includes some other features.

Home page with more info and changelog (in Swedish):
http://firetech.nu/pactrackdroid/

Consumer ID
-----------

The API used by PactrackDroid requires an "API key", and the API has a hard
limit on number of requests per day and key. Due to this, to avoid abuse, the
API key used by the release version of PactrackDroid is _not_ open source.

If you fork the project, you will need to create your own API key at
https://developer.postnord.com and save it, into (for example)
`app/src/main/res/values/postnord_apikey.xml`, using the following syntax:

    <?xml version="1.0" encoding="utf-8"?>
    <resources xmlns:tools="http://schemas.android.com/tools"
               tools:ignore="TypographyDashes">
      <string name="postnord_apikey">
        xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
      </string>
    </resources>


Authors
-------

PactrackDroid was created by Joakim "firetech" Tufvegren in September 2009.
After December 2013, the project was maintained by "firetech" and "blunden".
