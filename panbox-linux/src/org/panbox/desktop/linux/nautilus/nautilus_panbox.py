#!/usr/bin/env python
# 
# Panbox - encryption for cloud storage
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

from gi.repository import Nautilus, GObject

import dbus
import gettext
import os
import urllib
import locale

# Constants
PANBOX_FOLDER = 'file://' + os.path.expanduser('~') + '/Panbox'

DBUS = 'org.panbox.client'
DBUS_PATH = '/org/panbox/client'

APPNAME = 'nautilus_panbox'
LANGDIR = os.path.join(os.path.dirname(__file__), 'locale/')


def get_dbus_method(method_name):
    try:
        bus = dbus.SessionBus()
        obj = bus.get_object(DBUS, DBUS_PATH)
        method = obj.get_dbus_method(method_name, DBUS)
        return method
    except dbus.DBusException:
        print "Method " + method_name + " not found."
        return -1


def set_language():
    method = get_dbus_method('getLocale')
    if method != -1:
        loc = method()
        if loc == "system_default":
            loc, enc = locale.getdefaultlocale()
    else:
        loc, enc = locale.getdefaultlocale()
    lang = gettext.translation(APPNAME, LANGDIR, [loc])
    lang.install()


class PanboxExtension(GObject.GObject, Nautilus.MenuProvider, Nautilus.InfoProvider):
    def __init__(self):
        set_language()
        self.xte_str = "xte 'keydown Control_L' 'key R' 'keyup Control_L'"

    def open_properties(self):
        method = get_dbus_method('openProperties')
        if method != -1:
            method()

    def add_share(self, menu):
        self.open_properties()

    def remove_share(self, menu):
        self.open_properties()

    def get_mount_point(self):
        method = get_dbus_method('getMountPoint')
        if method != -1:
            return method()

    def get_file_status(self, path):
        method = get_dbus_method('getFileStatus')
        path = urllib.unquote(path).decode('utf8')
        if method != -1:
            return method(path)
        else:
            return -1

    def get_share_storage_backend_type(self, share_name):
        method = get_dbus_method('getShareStorageBackendType')
        if method != -1:
            return method(share_name)
        else:
            return -1

    def share_directory(self, menu, path):
        method = get_dbus_method('shareDirectory')
        if method != -1:
            method(path)


    def open_restore_revision_gui(self, menu, path):
        method = get_dbus_method('openRevisionGui')
        if method != -1:
            method(path)

    # Menu when (multiple) files are selected
    def get_file_items_full(self, provider, window, files):
        if self.get_mount_point() != -1:
            panbox_folder = 'file://' + self.get_mount_point()
        else:
            panbox_folder = PANBOX_FOLDER
        if any(urllib.unquote(file.get_uri()).startswith(panbox_folder) for file in files):
            files_to_string = []
            for f in files:
                files_to_string.append(urllib.unquote(f.get_uri()))

            item_name = 'Panbox::Panbox'
            item_label = 'Panbox'
            item_icon = os.path.join(os.path.dirname(__file__), 'img/panbox-icon.png')
            menu = Nautilus.MenuItem(name=item_name, label=item_label, tip='', icon=item_icon)

            submenu = Nautilus.Menu()
            menu.set_submenu(submenu)

            file_path = urllib.unquote(f.get_uri())
            if os.path.dirname(file_path) == panbox_folder:
                # Item
                item_name = 'Panbox::RemoveShare'
                if len(files) == 1:
                    item_label = _('Remove Share')
                    # item_label = 'Remove Share'
                else:
                    item_label = _('Remove Shares')
                    # item_label = 'Remove Shares'
                remove_share = Nautilus.MenuItem(name=item_name, label=item_label)
                submenu.append_item(remove_share)
                remove_share.connect('activate', self.remove_share)

                file = files[0].get_uri()[7:]
                # if len(files) == 1 and self.get_file_status(file) != 3 and self.get_file_status(file) != -1:
                share_name = file_path.replace(PANBOX_FOLDER, "").split("/")[1]
                share_type = self.get_share_storage_backend_type(share_name)
                if share_type == "Dropbox" and len(files) == 1:
                    # Item
                    item_name = 'Panbox::ShareFolder'
                    item_label = _('Share Folder')
                    # item_label = 'Share Folder'
                    share_dir = Nautilus.MenuItem(name=item_name, label=item_label)
                    submenu.append_item(share_dir)
                    share_dir.connect('activate', self.share_directory, file_path[7:])

            file = file_path[7:]
            if os.path.isfile(file) and len(files) == 1:
                share_name = file_path.replace(PANBOX_FOLDER, "").split("/")[1]
                share_type = self.get_share_storage_backend_type(share_name)
                if not share_type == "Directory":
                    # Item
                    item_name = 'Panbox::ShowRevs'
                    item_label = _('Show Revisions')
                    show_revs = Nautilus.MenuItem(name=item_name, label=item_label)
                    submenu.append_item(show_revs)
                    show_revs.connect('activate', self.open_restore_revision_gui, file_path[7:])

            return menu,
        else:
            pass

    # Menu when no files are selected
    def get_background_items_full(self, provider, window, path):
        unquoted_path = urllib.unquote(path.get_uri())
        if self.get_mount_point() != -1:
            panbox_folder = 'file://' + self.get_mount_point()
        else:
            panbox_folder = PANBOX_FOLDER
        if unquoted_path == panbox_folder or unquoted_path.startswith(panbox_folder + "/"):

            item_name = 'Panbox::Panbox'
            item_label = 'Panbox'
            menu = Nautilus.MenuItem(name=item_name, label=item_label)

            submenu = Nautilus.Menu()
            menu.set_submenu(submenu)

            if unquoted_path == panbox_folder:
                # Item
                item_name = 'Panbox::AddShare'
                item_label = _('Add Share')
                add_share = Nautilus.MenuItem(name=item_name, label=item_label)
                submenu.append_item(add_share)
                # Bind menu entries to functions
                add_share.connect('activate', self.add_share)

            return menu,
        else:
            pass

    # Create the file overlays
    def update_file_info_full(self, provider, handle, closure, file):
        # if self.get_mount_point() != -1:
        # panbox_folder = 'file://' + self.get_mount_point()
        # else:
        #     panbox_folder = PANBOX_FOLDER
        # if file.get_uri().startswith(panbox_folder + '/'):
        #     # file_status = self.get_file_status(file.get_uri()[7:])
        #     file_status = 0
        #     if file_status == 0:
        #         file.add_emblem('panbox-checkmark')
        #     elif file_status == 1:
        #         file.add_emblem('panbox-unclear')
        #     elif file_status == 2:
        #         file.add_emblem('panbox-syncing')
        #     elif file_status == 3:
        #         file.add_emblem('panbox-unwatched')
        #     elif file_status == -1:
        #         file.add_emblem('panbox-error')
        # else:
        #     pass
        return Nautilus.OperationResult.COMPLETE
