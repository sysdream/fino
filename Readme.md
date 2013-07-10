Fino: An Android Dynamic Analysis Tool
======================================

Well, the presentation of this tool at the 29C3 event was pretty short and we did not take time to put all the doc here so here is a little HowTo (much more doc to come but hopefully this small README would be enough to cover what has been shown during our presentation).

How to install
--------------

1. Clone all the required git repos (fino, gadget and gadget-client) available on Sysdream's Github
2. Make sure apktool, jarsigner and sed are installed and available in your environment
3. Make sure fino/inject.sh script is chmod +x
4. Make sure you have your Android debug keystore available in ~/.android/debug.keystore. If not, create it:

        keytool -genkey -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android -keyalg RSA -validity 14000
  
   (use any values for the certificate)

5. Take any APK file, and inject Fino's minimal service using fino/inject.sh script like this: 
    
        cd fino
        ./inject.sh my-original.apk dest-app-with-fino.apk

6. Install the APK inside an emulator or in your phone using adb or a file manager: 

        adb install dest-app-with-fino.apk.
   
   If you use adb, no matter if "Accept unknown sources" option is enabled, it will install the app. Check this option if you install it from a file manager.

You have successfully injected Fino's service inside an existing APK and drop the patched version of it into an Android device.
Let setup Gadget now.

1. Compile and install the Gadget application on the Android device (we will provide a clean APK later, for the moment simply use ant to build it and install it). If cxompilation does not work, use the gadget.apk file we dropped in the repo.
2. Launch Gadget, it will start a background service handling every TCP clients
3. With adb, forward a local port of your computer to the android device (if you are using a real phone, you're supposed to have it connected via USB): 

        adb forward tcp:1234 tcp:4444. 
   
   You can set the port used  by Gadget in its main config screen.
4. In the gadget-client repo, you'll find a shell.py script. Launch it (give every needed parameters, that is local ip, local port and your apk package name).
5. Enjoy the shell.

How to interact
---------------

Once done, you can access your target app with some "magic" variables:
* app: this variable provides everything, and in particular a method called "find" which is very useful to find running activities
* R: same as in the Java code. Use it the same way (to access string ids, layout ids, and so on)

To get a reference to an existing class, use app.get_class(). This method is documented (docstring).
Once an activity is retrieved, you can access everything in it and change almost everything, as shown in our demo. The only limit is the obfuscation (added by proguard for example) and you should need a quick static analysis first before going deeper inside the running app).


Quick tips
---------------

Some useful code snippets when dealing with android applications with Fino:

* To get all the running activities of a given application, use this: 

        activities = app.find('android.app.Activity')
    
* Auto-completion is provided by pressing <Alt> + <Tab>. 
* Python's dir() method may be useful to explore objects at runtime but may sometimes not help anymore when facing code obfuscation
* To refresh a running activity, the Python's Activity class implements a method called refresh() which handles everything
* If a property name is also a method name, you must specify if you want to call the method or just get the property by using the corresponding helper: someobject._P.someproperty or someobject._M.somemethod()

                                                                                             
