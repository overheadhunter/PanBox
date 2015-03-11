# Prerequisites

Panbox requires Python 2.7, Java 7 or later as well as several additional libraries. 
Before trying to run Panbox, please make sure the following dependencies have been installed on your system:   

 * Ubuntu: dbus-java-bin python-appindicator python-nautilus libbluetooth-dev python-notify python-gtk2 python-dbus
 * Arch: dbus-java (from AUR) python2-nautilus python2-notify java-bluecove libappindicator-gtk2 libappindicator-gtk3 
 * Fedora: libmatthew-java python-appindicator nautilus-python notify-python bluecove 
 * Gentoo: dev-java/dbus-java dev-java/libmatthew-java dev-python/nautilus-python dev-python/notify-python net-wireless/bluez

Should you choose to make use of Oracle JDK/JRE instead of OpenJDK, please make sure to install the
Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files, which are required for AES
encryption with key sizes greater 128 bits.   

 
# Starting Panbox

Run the startup script 'start.sh' from within the directory the panbox archive has been extracted to: 
./start.sh 

If this fails with a permission denied error, check if the file is executable. If it isn't, run     
chmod +x start.sh

# Systray support and startup arguments

Depending on the linux distribution as well as the desktop environment of your choice, you may encounter problems 
with DBUS support and/or regarding the Panbox tray app. Should you experience any such problems, you may try starting 
Panbox with the following arguments:

./start.sh --fallback-tray-gtk
This will start Panbox with a gtk based status icon instead of the default appindicator-based tray icon 
	
./start.sh --fallback-tray-java
This will start Panbox with a fallback java tray icon instead of the default appindicator

./start.sh --no-dbus
This will start Panbox without the integrated DBUS service. Note that as gtk/appindicator-based tray support relies on a python 
script communicating with Panbox via DBUS, this implicitely also enables the  --fallback-tray-java flag.        

# FUSE options

You may provide custom FUSE mount options as arguments to the startup script. Panbox passes those options to the underlying 
fuse layer during startup. If no custom mount options are provided, due to performance reasons Panbox per default enables the 
big_writes option and increases the value of max_write. 




