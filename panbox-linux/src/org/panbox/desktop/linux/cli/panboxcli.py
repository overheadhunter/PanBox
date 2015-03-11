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

__author__ = 'Dominik Spychalski'

import sys
import dbus
import subprocess
import os
import getpass

# dbus constants
DBUS_NAME = 'org.panbox.client'
DBUS_PATH = '/org/panbox/client'

# terminal output constants
START_RED = "\x1B[31;40m"
END_RED = "\x1B[31;0m"

START_GREEN = "\033[92m"
END_GREEN = "\033[0m"


# status-code components

# component code constants
GENERAL =               0b0000
SHAREMANAGER =          0b0001
IDENTITYMANAGER =       0b0010
DBUS =                  0b0011
VFS =                   0b0100
CRYPTO =                0b0101
GUI =                   0b0110
DEVICEMANAGER =         0b0111
KEYMANAGER =            0b1000
PAIRING =               0b1001
CLI =                   0b1010
ADDRESSBOOKMANAGER =    0b1100

# status code constants
OK =                    0b0000
ILLEGALARGUMENT =       0b0001
UNKNOWN =               0b0010
DEVICEEXISTS =          0b0011
SQLERROR =              0b0100
SHAREEXISTS =           0b0101
UNRECOVERABLEKEY =      0b0110
SEQUENCENUMBER =        0b0111
SHAREMETADATA =         0b1000
SHARENOTEXISTS =        0b1001
CONTACTEXISTS =         0b1010
IOERROR =               0b1011
PINVERIFICATIONFAILED = 0b1100

def buildcode(component_id, code):
    ret = component_id << 4
    ret = ret + code
    return ret

GENERAL_OK = buildcode(GENERAL, OK)
SHAREMANAGER_OK = buildcode(SHAREMANAGER, OK)
IDENTITYMANAGER_OK = buildcode(IDENTITYMANAGER, OK)
DBUS_OK = buildcode(DBUS, OK)
VFS_OK = buildcode(VFS, OK)
CRYPTO_OK = buildcode(CRYPTO, OK)
GUI_OK = buildcode(GUI, OK)
DEVICEMANAGER_OK = buildcode(DEVICEMANAGER, OK)
KEYMANAGER_OK = buildcode(KEYMANAGER, OK)
PAIRING_OK = buildcode(PAIRING, OK)

def component_name(component_code):
    ret = ""
    
    if component_code == GENERAL:
        ret = "GENERAL"
    if component_code == SHAREMANAGER:
        ret = "SHAREMANAGER"
    if component_code == IDENTITYMANAGER:
        ret = "IDENTITYMANAGER"
    if component_code == DBUS:
        ret = "DBUS"
    if component_code == VFS:
        ret = "VFS"
    if component_code == CRYPTO:
        ret = "CRYPTO"
    if component_code == GUI:
        ret = "GUI"
    if component_code == DEVICEMANAGER:
        ret = "DEVICEMANAGER"
    if component_code == KEYMANAGER:
        ret = "KEYMANAGER"
    if component_code == PAIRING:
        ret = "PAIRING"
    if component_code == CLI:
        ret = "CLI"
    if component_code == ADDRESSBOOKMANAGER:
        ret = "ADDRESSBOOKMANAGER"

    return ret

def status_name(status_code):
    ret = ""
    
    if status_code == OK:
        ret = "OK"
    if status_code == ILLEGALARGUMENT:
        ret = "ILLEGALARGUMENT"
    if status_code == UNKNOWN:
        ret = "UNKNOWN"
    if status_code == DEVICEEXISTS:
        ret = "DEVICEEXISTS"
    if status_code == SQLERROR:
        ret = "SQLERROR"
    if status_code == SHAREEXISTS:
        ret = "SHAREEXISTS"
    if status_code == UNRECOVERABLEKEY:
        ret = "UNRECOVERABLEKEY"
    if status_code == SEQUENCENUMBER:
        ret = "SEQUENCENUMBER"
    if status_code == SHAREMETADATA:
        ret = "SHAREMETADATA"
    if status_code == SHARENOTEXISTS:
        ret = "SHARENOTEXISTS"
    if status_code == CONTACTEXISTS:
        ret = "CONTACTEXISTS"
    if status_code == IOERROR:
        ret = "IOERROR"
    if status_code == PINVERIFICATIONFAILED:
        ret = "PINVERIFICATIONFAILED"

    return ret


def component_code(code):
    mask = 0b00001111
    code = code >> 4
    return code & mask

def status_code(code):
    mask = 0b00001111
    return code & mask

def is_error(code):
    return status_code(code) != OK

##-----------------------------------------------------
## end status-code components

def dbus_method(method_name):
    try:
        bus = dbus.SessionBus()
        obj = bus.get_object(DBUS_NAME, DBUS_PATH)
        method = obj.get_dbus_method(method_name, DBUS_NAME)
        return method

    except dbus.DBusException as err:
        print err
        exit()

def print_status_code(code, message_success, message_fail):
    if is_error(code):
        print_error(code, message_fail)
    else:
        print_message(message_success)

def print_status(code, message_success, message_fail):
    if is_error(code):
        print_error_message(message_fail)
    else:
        print_message(message_success)

def print_error(code, message):
    cmp_code = component_code(code)
    stat_code = status_code(code)
    
    if status_code != OK:
        print START_RED + "[" + status_name(stat_code) +  " error in component " + component_name(cmp_code) +  " - error-code: " + bin(code).replace("0b", "") + "]" + END_RED
        if message != "":
            print START_RED + "--> " + END_RED + message

def print_error_message(message):
    print START_RED + message + END_RED

def print_message(message):
    print START_GREEN + message + END_GREEN

def print_help():
    print "-v or --version			Show version number of panbox."
    print "-h or --help				Show this help."
    print "-ls or --listShares			List all configured shares."
    print "-lc or --listCSPs			List all supported csps."
    print "-as or --add_share <name> <csp> <path>	Add a new share for <csp> with <name> to <path>"
    print "-rs or --removeShare <name>		Remove Share <name>"
    print "--reset					Reset all panboxconfig, backup your keys if needed"
    print "--mount					Mount all shares."
    print "--unmount				Unmount all shares."
    # print "--start					Starts panbox"
    print "--close					Close panbox"
    print "--status				Checks and returns the state of needed Panbox-Components"
    print "--delete               Deletes the own Identity"
    print "--init					Generats a new identity."
    print "-ei or --exportIdentity [opt] <path>	Export your own identity to share with a friend. (opt = --multiple, for selective export)"
    print "-ii or --import_identity [opt] <vcard>        Import an identity stored in a vcard. (opt == --noverification, to not verify the vcard within the import process with a given pin)"
    print "-gc or --getcontacts			Returns all stored contacts."
    print "--backup                                Backup of Indentity, stored in hidden .pbbackup directory"
    print "--restore <backup-file>                 Restores a backup identity"
    print "--get_id                                Displays onw identity"
    print "-ac or --addcontact                    add a new Panbox-Contact"
    print "-dc or --deletecontact                deletes a Panbox-Contact"

def remove_share():
    share_name = ""
    answer = "123"
    code = 0

    if (len(sys.argv) <= 2):
        print "Need share-name as parameter!"
        exit()
    elif (len(sys.argv) > 2):
        share_name = sys.argv[2]

    remove_share = "Do you really want to remove share '" + share_name + "' ?"
    remove_source = "Remove the share data source directory of share '" + share_name + "', too ?"

    while answer not in ("", "n", "N", "y", "Y"):
        answer = raw_input(remove_share + " [Y/N]: ")
        print answer

    if (answer in ("n", "N")):
        exit()
    answer= "123"
    while answer not in ("", "n", "N", "y", "Y"):
        answer = raw_input(remove_source + " [Y/N]: ")
        print answer

    rmDir = False
    if answer in ("y", "Y"):
        rmDir = True

    if (len(sys.argv) > 2):
        code = dbus_method('removeShare')(sys.argv[2], rmDir)

    
    print_status_code(code, "Share removed successful.", "Error on share remove!")


def add_share():
    if (len(sys.argv) <> 5):
        print "Parameter error!"
        exit()

    share_name = sys.argv[2]
    csp_name = sys.argv[3]
    share_path = sys.argv[4]
    shares = dbus_method("getShares")()

    if share_name in shares:
        print "Share already exists"
        exit()

    if (os.path.isdir(share_path) & os.path.exists(share_path)):
        password = getpass.getpass("Please insert identity-password to add share '" + share_name + "':")
        code = dbus_method("addShare")(share_name, csp_name, share_path, dbus.ByteArray(password.encode("UTF-8")))

        print_status_code(code, "addShare successful", "Error on addShare!")
        
    else:
        code = buildcode(CLI, SHARENOTEXISTS)
        print_error(code, "Parameter error: share-path '" + share_path + "' does not exists!")
        
#def open_url():
#	if (len(sys.argv) <> 3):
#		print "Parameter error!"
#		exit()
#
#	url = sys.argv[2]
#	code = dbus_method("openURL")(url)
#	print_status_code(code, "openURL successful", "Error on openURL!")

def get_shares():
    result = dbus_method("getShares")()
    if result != -1:
        for s in result:
            print s


def get_csps():
    result = dbus_method("getCSPs")()
    if result != -1:
        for s in result:
            print s


def shutdown():
    remote = 1

    try:
        remote = dbus_method("isRemote")()
    except dbus.DBusException as err:
        print err

    try:
        if remote == 0:
            dbus_method('shutdown')()
    except dbus.DBusException as err:
        if err.get_dbus_name() != "org.freedesktop.DBus.Error.NoReply":
            print err


def check_process(process_name):
    ps = subprocess.Popen("ps -ef | grep " + process_name + " | grep -v grep", shell=True, stdout=subprocess.PIPE)
    output = ps.stdout.read()
    l = 0
    l = len(output.split("\n"))

    if l <= 1:
        # process not running
        return 0
    else:
        # process running
        return 1


def get_status():
    dbus_service = check_process("dbus-daemon")
    pb_backend = check_process("panbox_backend")
    pb_frontend = check_process("panbox_tray_icon.py")

    run = "running..."
    nrun = "not running..."

    print "DBus-Service:		" + (run if dbus_service == 1 else nrun)
    print "Panbox-Backend:		" + (run if pb_backend == 1 else nrun)
    print "Panbox-Frontend:	" + (run if pb_frontend == 1 else nrun)


def export_own_identity():
    code = dbus_method('exportOwnIdentity')(sys.argv[2])
    print_status_code(code, "Identity export successful.", "Identity export failed!")


def get_ids(data_mul_array, message):
    ret = []

    input = raw_input(message)
    input = input.replace(" ", "")

    elems = input.split(",")

    for s in elems:
        if s.find("-") == -1:
            s = int(s)
            if s < len(data_mul_array):
                ret.append(data_mul_array[s][0])
        else:
            interval = s.split("-")
            a = 0
            b = 0

            if (len(interval) == 2):
                a = int(interval[0])
                b = int(interval[1])

                if a > b:
                    a = int(interval[1])
                    b = int(interval[0])

                if a >= len(data_mul_array) or b >= len(data_mul_array):
                    print "Interval boundaries out of range!"
                    exit()

                while a <= b:
                    if (a < len(data_mul_array)):
                        ret.append(data_mul_array[a][0])
                        a = a + 1
    return ret


def export_identities(path):
    contacts = get_contacts()
    
    ret = get_ids(contacts, "Insert ID's of contacts to extract (comma separated, interval x-y):")

    code = dbus_method("exportContacts")(ret, path)
    print_status_code(code, "Contact(s) export successful.", "Error on export contacts(s)!")


def start_panbox():
    print "Starting panbox ..."
    subprocess.call(['./start.sh'])


def get_contacts():
    conctacts = dbus_method("getContacts")()
    i = 0
    for s in conctacts:
        print "[" + str(i) + "]    " + s[1]
        i = i + 1

    return conctacts


def import_identity():
    noverification = False
    import_contact = False
    authVerified = False
    vcard = ""
    
    if len(sys.argv) == 4:
        if sys.argv[2].lower() == "--noverification":
            vcard = sys.argv[3]
            noverification = True
        elif sys.argv[3].lower() == "--noverification":
            vcard = sys.argv[2]
            noverification = True
    else:
        vcard = sys.argv[2]
    
    if not noverification:    
        pin = getpass.getpass("Enter verification pin:")
    
        code = dbus_method("verifyContacts")(vcard, pin)
    
        print_status(code, "vCard verification successful...", "Error on vCard pin verification!")
    
        import_contact = True
        authVerified = not is_error(code)
    
        if is_error(code):
            input = raw_input("Continue import despite of verification fail (not recommended)? (Y/N)")
            if input in ("N", "n"):
                import_contact = False
    
    if import_contact or noverification:
        
        vcard_items = dbus_method("getContacts")(vcard)
        
        for s in vcard_items:
            print "[" + str(s[0]) + "]    " + s[1]
        
        ids = get_ids(vcard_items, "Insert ID's of contacts to import (comma separated, interval x-y):")
        
        code = dbus_method("importContact")(ids, vcard, authVerified)
        print_status_code(code, "Contact(s) imported successfully.", "Error on import contact(s)!")


def reset():
    backup = False

    input = raw_input("Backup old Identity (Y/N):")
    if (input in ("Y", "y")):
        backup = True

    mail = raw_input("E-mail:")
    firstname = raw_input("First name:")
    lastname = raw_input("Last name:")
    devicename = raw_input("Device-name:")

    password1 = "passwd1"
    password2 = "passwd2"

    while (password1 <> password2):
        password1 = getpass.getpass("Password:")
        password2 = getpass.getpass("Retype password:")

        if (password1 <> password2):
            print "Passwords don't match, insert again..."

    code = dbus_method("resetIdentity")(mail, firstname, lastname, dbus.ByteArray(password1.encode("UTF-8")), devicename,
                               backup)
    print_status_code(code, "Identity reset successfully.", "Error on reset identity!")


def backup():
    code = dbus_method("backupIdentity")()
    print_status_code(code, "Identity backup successfully.", "Error on backup identity!")


def restore():
    if (len(sys.argv) <> 3):
        print "parameter error"
        exit()
    pbbakfile = sys.argv[2]
    backup = False

    input = raw_input("backup old identity (Y/N):")
    if (input in ("Y", "y")):
        backup = True

    code = dbus_method("restoreIdentity")(pbbakfile, backup)
    print_status_code(code, "Identity restored successfully.", "Error on identity restore!")


def init():
    mail = raw_input("E-mail:")
    firstname = raw_input("First name:")
    lastname = raw_input("Last name:")
    devicename = raw_input("Device-name:")

    password1 = "passwd1"
    password2 = "passwd2"

    while (password1 <> password2):
        password1 = getpass.getpass("Password:")
        password2 = getpass.getpass("Retype password:")

        if (password1 <> password2):
            print "Passwords don't match, insert again..."

    code = dbus_method("createIdentity")(mail, firstname, lastname, dbus.ByteArray(password1.encode("UTF-8")), devicename)
    print_status_code(code, "Identity created successfully.", "Error on identity initialization!")


def delete():
    code = dbus_method("deleteIdentity")()
    print_status_code(code, "Identity deleted successfully.", "Error on delete identity!")


def get_id():
    res = dbus_method("getOwnIdentity")()

    for s in res:
        print s[0] + ": " + s[1]


def add_contact():
    mail = raw_input("E-mail: ")
    firstname = raw_input("First name: ")
    lastname = raw_input("Last name: ")

    code = dbus_method("addContact")(mail, firstname, lastname)
    print_status_code(code, "Contact added successfully.", "Error on add contact!")


def delete_contact():
    contacts = get_contacts()
    ret = []

    input = raw_input("Insert ID's of contacts to delete (comma separated, interval x-y):")
    input = input.replace(" ", "")

    elems = input.split(",")

    for s in elems:
        if s.find("-") == -1:
            s = int(s)
            if s < len(contacts):
                ret.append(contacts[s][0])
        else:
            interval = s.split("-")
            a = 0
            b = 0

            if (len(interval) == 2):
                a = int(interval[0])
                b = int(interval[1])

                if a > b:
                    a = int(interval[1])
                    b = int(interval[0])

                if a >= len(contacts) or b >= len(contacts):
                    print "Interval boundaries out of range!"
                    exit()

                while a <= b:
                    if (a < len(contacts)):
                        ret.append(contacts[a][0])
                        a = a + 1

    code = dbus_method("deleteContact")(ret)
    print_status_code(code, "Contact(s) deleted successfully.", "Error on delete contact(s)!")


def main():
    try:
        opt = ""
        if len(sys.argv) > 1:
            opt = sys.argv[1]

        if opt in ("-v", "--version"):
            print dbus_method("getVersion")()

        elif opt in ("-h", "--help"):
            print_help()

        elif opt in ("-ls", "--listShares"):
            get_shares()

        elif opt in ("-lc", "--listCSPs"):
            get_csps()

        elif opt in ("-rs", "--removeShare"):
            remove_share()

        elif opt in ("-as", "--add_share"):
            add_share()

        elif opt == "--unmount":
            dbus_method('unmount')()

        elif opt == "--mount":
            dbus_method('mount')()

        elif opt == "--close":
            shutdown()

        elif opt == "--status":
            get_status()

        elif opt in ("-ii", "--import_identity"):
            import_identity()

        elif opt in ("-ei", "--exportIdentity"):
            if len(sys.argv) == 4:
                path = ""
                parameter_valid = 0

                if sys.argv[2] == "--multiple":
                    path = sys.argv[3]
                    parameter_valid = 1
                if sys.argv[3] == "--multiple":
                    path = sys.argv[2]
                    parameter_valid = 1

                if parameter_valid == 1:
                    export_identities(path)
                else:
                    print "Parameter error"
            elif len(sys.argv) == 3:
                export_own_identity()
            else:
                print "Parameter missing..."

        elif opt in ("-gc", "--getcontacts"):
            get_contacts()

        elif opt == "--reset":
            reset()

        elif opt == "--backup":
            backup()

        elif opt == "--restore":
            restore()

        elif opt == "--init":
            init()

        elif opt == "--delete":
            delete()

        elif opt == "--get_id":
            get_id()

        elif opt in ("-ac", "--addcontact"):
            add_contact()

        elif opt in ("-dc", "--deletecontact"):
            delete_contact()

        elif opt == "":
            start_panbox()
            
#        elif opt in ("-u", "--url"):
#        	open_url() 

        else:
            print "Wrong arguments: " + opt + " \nuse " + sys.argv[0] + " --help for help"

    except Exception as err:
        print err
        print "for help use --help"
        return 2


if __name__ == '__main__':
    main()
