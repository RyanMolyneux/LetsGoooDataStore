package com.ryanmolyneux.letsgooo.datastore.datastores

import java.io.FileWriter;
import java.io.FileReader;
import java.net.URI

abstract class AbsFileManager: AbsDatastoreManager {
    constructor(datastoreUri: String): super(datastoreUri);

    open fun getNewFileWriter(): FileWriter {
        return FileWriter(URI(getDatastoreUri()).path);
    }

    open fun getNewFileReader(): FileReader {
        return FileReader(URI(getDatastoreUri()).path);
    }
}