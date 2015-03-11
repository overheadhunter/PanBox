#!/bin/sh -ex
#EMBLEM_FILE="emblems.tar.gz"
NAUTILUS_EXTENSION="nautilus_panbox.py"

# copy python extension
if [ -f "$NAUTILUS_EXTENSION" ] 
then
	mkdir -p ~/.local/share/nautilus-python/extensions
	mkdir -p ~/.local/share/nautilus-python/extensions/img && echo "[+] created python extension dirs"
	cp nautilus_panbox.py ~/.local/share/nautilus-python/extensions/nautilus_panbox.py && echo "[+] copied python extension"
	cp img/panbox-icon_22.png ~/.local/share/nautilus-python/extensions/img/panbox-icon_22.png && echo "[+] copied panbox icon"
	cp -r locale/ ~/.local/share/nautilus-python/extensions/ && echo "[+] locale files copied"
else
	echo "$NAUTILUS_EXTENSION missing"
fi

## extract symbols
#if [ -f "$EMBLEM_FILE" ]
#then
#	mkdir -p ~/.icons && echo "[+] created icon dir"
#	tar -xzf emblems.tar.gz -C ~/.icons/ && echo "[+] extracted emblem files"
#else
#	echo "$EMBLEM_FILE missing"
#fi




