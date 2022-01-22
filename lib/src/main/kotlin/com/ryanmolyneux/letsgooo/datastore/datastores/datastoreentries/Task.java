package com.ryanmolyneux.letsgooo.datastore.datastores.datastoreentries;


public class Task extends AbsDatastoreEntry {
    public static final String TASK_STATUS_COMPLETE = "TASK_COMPLETE";
    public static final String TASK_STATUS_PENDING = "TASK_PENDING";
    private String name;
    private String currentStatus;

    public Task(String name, String currentStatus) throws Exception {
        setName(name);
        setCurrentStatus(currentStatus);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) throws Exception {
        if ( currentStatus.equals(TASK_STATUS_PENDING) || currentStatus.equals(TASK_STATUS_COMPLETE) ) {
            this.currentStatus = currentStatus;
        } else {
            throw new Exception("Task, method setCurrentStatus must take a parameter holding a value from its set of TASK_STATUS_* member variables.");
        }
    }
}
