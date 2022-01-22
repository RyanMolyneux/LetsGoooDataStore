package com.ryanmolyneux.letsgooo.datastore.pairings

import com.ryanmolyneux.letsgooo.datastore.datastores.JsonFileManager;
import com.ryanmolyneux.letsgooo.datastore.datastores.datastoreentries.AbsDatastoreEntry

class StoredKeyValuePairings<Key, Value: AbsDatastoreEntry>: KeyValuePairings<Key, Value> {
    private val jsonFileManager: JsonFileManager<Key, Value>;

    constructor(mutableMap: MutableMap<Key, Value>, jsonFileManager: JsonFileManager<Key, Value>): super(mutableMap) {
        this.jsonFileManager = jsonFileManager;
        retrieve();
    }

    override fun createPairing(keyOfPairToBeCreated: Key, valueToBeAdded: Value) {
        super.createPairing(keyOfPairToBeCreated, valueToBeAdded);
        store();
    }

    override fun updatePairingsValue(keyValueIsPairedWith: Key, newValueToPairKeyWith: Value) {
        super.updatePairingsValue(keyValueIsPairedWith, newValueToPairKeyWith)
        store();
    }

    override fun deletePairing(keyValueIsPairedWith: Key) {
        super.deletePairing(keyValueIsPairedWith)
        store();
    }

    private fun store() {
        jsonFileManager.write(getWrappedMap());
    }

    private fun retrieve() {
        setWrappedMap(jsonFileManager.read());
    }
}