PactrackDroid
======

This is PactrackDroid, an Android application to keep track of parcels sent with
the PostNord, the Swedish and Danish mail service (in Swedish only for now).
Originally developed by Joakim Andersson, but abandoned in 2011. blunden made
updates to the design of the app to fit Android 4.0+ in 2013, after which the
project was un-abandoned.

Home page with more info and changelog: http://firetech.nu/pactrackdroid/

Consumer ID
-----------

The API used by PactrackDroid requires a "consumer ID", which, according to the
terms and conditions, must be kept secret. To comply with this, the consumer ID
used by the official version is not open sourced.

If you fork the project, you will need to create your own consumer ID at
http://www.postnordlogistics.se/sv/online-services/widgetsochwebservices/
and save it, into (for example) res/values/postnord_consumerid.xml, using the
following syntax:

    <?xml version="1.0" encoding="utf-8"?>
    <resources xmlns:tools="http://schemas.android.com/tools"
               tools:ignore="TypographyDashes">
      <string name="postnord_consumerid">
        xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
      </string>
    </resources>


Authors
-------

PactrackDroid was created and is maintained by Joakim Andersson.
The user interface, as it looks today, was made by blunden. The project would
still be abandoned if it wasn't for that awesome new look. :)
