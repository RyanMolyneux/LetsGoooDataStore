package com.ryanmolyneux.letsgooo.datastore.datastores

abstract class AbsDatastoreManager {
    private lateinit var datastoreUri: String;
    private lateinit var dataIntegrityProtectionDatastoreUri: String;

    constructor(datastoreUri: String) {
        setDatastoreUri(datastoreUri);
    }

    fun getDatastoreUri(): String {
        return datastoreUri;
    }

    fun setDatastoreUri(datastoreUri: String) {
        this.datastoreUri = datastoreUri;
    }

    protected fun getDataIntegrityProtectionDatastoreUri(): String {
        return dataIntegrityProtectionDatastoreUri;
    }

    protected fun setDataIntegrityProtectionDatastoreUri(dataIntegrityProtectionDatastoreUri: String): Unit {
        this.dataIntegrityProtectionDatastoreUri = dataIntegrityProtectionDatastoreUri;
    }
}