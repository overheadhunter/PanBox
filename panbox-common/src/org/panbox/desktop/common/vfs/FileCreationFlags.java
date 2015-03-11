package org.panbox.desktop.common.vfs;


public enum FileCreationFlags {

    CREATE_ALWAYS, CREATE_NEW, OPEN_ALWAYS, OPEN_EXISTING, TRUNCATE_EXISTING;
    
    public boolean hasToExist() {
        return shouldOpen() || shouldTruncate();
    }

    public boolean shouldCreate() {
        return equals(CREATE_NEW) || equals(CREATE_ALWAYS) || equals(OPEN_ALWAYS);
    }

    public boolean shouldOpen() {
        return equals(OPEN_ALWAYS) || equals(OPEN_EXISTING);
    }

    public boolean shouldTruncate() {
        return equals(TRUNCATE_EXISTING);
    }
}
