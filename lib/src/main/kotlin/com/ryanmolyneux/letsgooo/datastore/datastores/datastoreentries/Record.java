package com.ryanmolyneux.letsgooo.datastore.datastores.datastoreentries;

public class Record extends AbsDatastoreEntry {
    private String name;
    private Task[] tasksOnRecord;

    public Record(String name, Task[] tasksOnRecord) {
        setName(name);
        setTasksOnRecord(tasksOnRecord);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Task[] getTasksOnRecord() {
        return tasksOnRecord;
    }

    public void setTasksOnRecord(Task[] tasksOnRecord) {
        this.tasksOnRecord = tasksOnRecord;
    }
}
