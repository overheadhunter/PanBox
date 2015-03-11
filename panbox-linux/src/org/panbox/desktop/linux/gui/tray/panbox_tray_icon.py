#!/usr/bin/env python
# 
#               Panbox - encryption for cloud storage 
#      Copyright (C) 2014-2015 by Fraunhofer SIT and Sirrix AG 
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 
# Additonally, third party code may be provided with notices and open source
# licenses from communities and third parties that govern the use of those
# portions, and any licenses granted hereunder do not alter any rights and
# obligations you may have under such open source licenses, however, the
# disclaimer of warranty and limitation of liability provisions of the GPLv3 
# will apply to all the product.
# 

__author__ = 'Timo Nolle'

import os
import subprocess
import locale
import gettext
import dbus
import gtk
import pynotify

import sys


# Import local classes and files
from panbox_tray_dbus_interface import PanboxTrayInterface
import panbox_constants as const

# Constants
PANBOX_FOLDER = const.PANBOX_FOLDER
DBUS = const.DBUS_CLIENT
DBUS_PATH = const.DBUS_CLIENT_PATH

APPNAME = 'panbox_tray_icon'
LANGDIR = os.path.join(os.path.dirname(__file__), 'locale')

ICON = const.ICON
ICON_BIG = const.ICON_BIG

use_appindicator = True
try:
    import appindicator
except:
    use_appindicator = False


def get_dbus_method(method_name):
    try:
        bus = dbus.SessionBus()
        obj = bus.get_object(DBUS, DBUS_PATH)
        method = obj.get_dbus_method(method_name, DBUS)
        return method
    except dbus.DBusException:
        n = pynotify.Notification(_('No connection to Panbox service'), "", ICON_BIG)
        n.set_timeout(3000)
        n.show()
        return -1


def is_mounted():
    method = get_dbus_method('isMounted')
    if method != -1:
        return method()
    else:
        return 0


def set_language():
    method = get_dbus_method('getLocale')
    if method != -1:
        loc = method()
        print loc
        if loc == "system_default":
            loc, enc = locale.getdefaultlocale()
    else:
        loc, enc = locale.getdefaultlocale()
    lang = gettext.translation(APPNAME, LANGDIR, [loc])
    lang.install()


def get_mount_point(self):
    method = get_dbus_method('getMountPoint')
    if method != -1:
        return method()
    else:
        return -1


def open_properties(self):
    method = get_dbus_method('openProperties')
    if method != -1:
        method()


def shutdown(self):
    method = get_dbus_method('shutdown')
    if method != -1:
        method()
    else:
        gtk.main_quit()


def about(self):
    method = get_dbus_method('about')
    if method != -1:
        method()


def get_shares():
    method = get_dbus_method('getShares')
    if method != -1:
        return method()
    else:
        return -1


def open_panbox_folder(self):
    mount = get_mount_point(self)
    if mount != -1:
        path = PANBOX_FOLDER[7:] if mount == -1 else mount
        subprocess.Popen(["xdg-open", path])


def popup_menu_cb(widget, button, time):
    menu = generate_menu()
    if menu:
        menu.popup(None, None, None, button, time)


def add_shares_menu(self, item):
    shares = get_shares()
    menu = gtk.Menu()
    if is_mounted() and len(shares) != 0:
        for s in shares:
            menu.append(gtk.MenuItem(s))
    else:
        no_shares_item = gtk.MenuItem('No Shares')
        no_shares_item.set_sensitive(False)
        menu.append(no_shares_item)
    menu.show_all()
    item.set_submenu(menu)


def generate_menu():
    menu = gtk.Menu()

    # Item
    properties_item = gtk.MenuItem(_('Properties'))
    properties_item.connect('activate', open_properties)
    menu.append(properties_item)

    # Separator
    menu.append(gtk.SeparatorMenuItem())

    # Item
    open_panbox_folder_item = gtk.MenuItem(_('Open Panbox Folder'))
    open_panbox_folder_item.connect('activate', open_panbox_folder)
    menu.append(open_panbox_folder_item)
    
    # Separator
    menu.append(gtk.SeparatorMenuItem())

    # Item
    about_item = gtk.MenuItem(_('About'))
    about_item.connect('activate', about)
    menu.append(about_item)

    # Item
    exit_item = gtk.MenuItem(_('Exit'))
    exit_item.connect('activate', shutdown)
    menu.append(exit_item)

    menu.show_all()
    return menu


def mount_unmount(self):
    if is_mounted():
        unmount()
    else:
        mount()


def mount():
    method = get_dbus_method('mount')
    if method != -1:
        method()


def unmount():
    method = get_dbus_method('unmount')
    if method != -1:
        method()


def main():
    opt = ""
    if len(sys.argv) > 1:
        opt = sys.argv[1]

    if opt in ("--tray-gtk"):
        use_appindicator = False
    elif opt in ("", "--tray-appindicator"):
        use_appindicator = True
    else:
        print "Unknown argument " + opt
        use_appindicator=True

    pynotify.init(APPNAME)

    # Set the current language
    set_language()

    if use_appindicator:
        ind = appindicator.Indicator('Panbox', ICON,
                                     appindicator.CATEGORY_APPLICATION_STATUS)
        ind.set_status(appindicator.STATUS_ACTIVE)
        #ind.set_status(appindicator.STATUS_ATTENTION)
        ind.set_menu(generate_menu())
    else:
        status_icon = gtk.StatusIcon()
        status_icon.set_from_file(os.path.abspath(ICON))
        status_icon.set_tooltip('Panbox')
        status_icon.connect('popup-menu', popup_menu_cb)

    PanboxTrayInterface()
    gtk.main()


if __name__ == '__main__':
    main()
