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

from _dbus_glib_bindings import DBusGMainLoop
import dbus
import dbus.service
import pynotify

import panbox_constants as const

DBUS = const.DBUS_TRAY
DBUS_PATH = const.DBUS_TRAY_PATH

ICON = const.ICON_BIG


class PanboxTrayInterface(dbus.service.Object):
    def __init__(self):
        pynotify.init('Panbox')
        bus_name = dbus.service.BusName(DBUS, bus=dbus.SessionBus())
        dbus.service.Object.__init__(self, bus_name, DBUS_PATH)

    @dbus.service.method('org.panbox.desktop.linux.dbus.PanboxTrayInterface', in_signature='s')
    def show_notification(self, message):
        n = pynotify.Notification(message, "", ICON)
        n.set_timeout(3000)
        n.show()


DBusGMainLoop(set_as_default=True)
