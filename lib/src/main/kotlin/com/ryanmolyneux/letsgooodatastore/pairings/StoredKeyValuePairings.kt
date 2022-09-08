package com.ryanmolyneux.letsgooodatastore.pairings

import com.ryanmolyneux.letsgooodatastore.datastores.JsonFileManager;
import com.ryanmolyneux.letsgooodatastore.datastores.datastoreentries.AbsDatastoreEntry

class StoredKeyValuePairings<Key, Value: AbsDatastoreEntry>: KeyValuePairings<Key, Value> {
    private val jsonFileManager: JsonFileManager<Key, Value>;

    constructor(mutableMap: MutableMap<Key, Value>, jsonFileManager: JsonFileManager<Key, Value>): super(mutableMap) {
        this.jsonFileManager = jsonFileManager;
        retrieve();
    }

    override fun createPairing(keyOfPairToBeCreated: Key, valueToBeAdded: Value) {
        super.createPairing(keyOfPairToBeCreated, valueToBeAdded);
        store(listOf(keyOfPairToBeCreated));
    }

    override fun updatePairingsValue(keyValueIsPairedWith: Key, newValueToPairKeyWith: Value) {
        super.updatePairingsValue(keyValueIsPairedWith, newValueToPairKeyWith)
        store(listOf(keyValueIsPairedWith));
    }

    override fun deletePairing(keyValueIsPairedWith: Key) {
        super.deletePairing(keyValueIsPairedWith)
        store(listOf(keyValueIsPairedWith));
    }

    private fun store(listOfKeysWhoseValuesHaveBeenUpdated: List<Key>) {
        jsonFileManager.merge(getWrappedMap(), listOfKeysWhoseValuesHaveBeenUpdated);
    }

    private fun retrieve() {
        setWrappedMap(jsonFileManager.read());
    }
}