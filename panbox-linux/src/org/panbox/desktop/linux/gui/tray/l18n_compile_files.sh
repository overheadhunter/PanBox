#!/bin/bash
msgfmt panbox_tray_icon_de.pot
mv messages.mo locale/de_DE/LC_MESSAGES/panbox_tray_icon.mo
msgfmt panbox_tray_icon_en.pot
mv messages.mo locale/en_US/LC_MESSAGES/panbox_tray_icon.mo