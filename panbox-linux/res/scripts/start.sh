#!/bin/sh
tty -s; if [ $? -ne 0 ]; then xterm -e "$0"; exit; fi
SCRIPT=$(readlink -f "$0")
jar_path=$(dirname "$SCRIPT")
#optimization_flags="-server -XX:+UseParallelGC -XX:+CMSClassUnloadingEnabled -XX:PermSize=256M -XX:MaxPermSize=512M"
java $optimization_flags -Djava.library.path=/usr/lib:/usr/lib/jni:/usr/lib64/libmatthew-java:/usr/share/libmatthew-java/lib -Dlogfile.location=$HOME/.panbox -Dlog4j.configuration=file:$jar_path/log4j.properties -jar $jar_path/panbox-linux.jar $*
