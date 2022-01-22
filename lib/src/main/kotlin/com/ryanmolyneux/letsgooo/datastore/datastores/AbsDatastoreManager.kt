package com.ryanmolyneux.letsgooo.datastore.datastores

abstract class AbsDatastoreManager {
    private lateinit var datastoreUri: String;

    constructor(datastoreUri: String) {
        setDatastoreUri(datastoreUri);
    }

    fun getDatastoreUri(): String {
        return datastoreUri;
    }

    fun setDatastoreUri(datastoreUri: String) {
        this.datastoreUri = datastoreUri;
    }
}