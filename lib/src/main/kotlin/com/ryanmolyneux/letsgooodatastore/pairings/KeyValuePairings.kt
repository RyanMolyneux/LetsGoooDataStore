package com.ryanmolyneux.letsgooodatastore.pairings

open class KeyValuePairings<Key, Value>: IKeyValuePairings<Key, Value> {
    private lateinit var wrappedMap: MutableMap<Key, Value>;

    constructor(wrappedMap: MutableMap<Key, Value>) {
        setWrappedMap(wrappedMap);
    }

    fun getWrappedMap(): MutableMap<Key, Value> {
        return wrappedMap;
    }

    fun setWrappedMap(wrappedMap: MutableMap<Key, Value>) {
        this.wrappedMap = wrappedMap;
    }

    override fun createPairing(keyOfPairToBeCreated: Key, valueToBeAdded: Value) {
       if (wrappedMap.containsKey(keyOfPairToBeCreated) == false) {
           wrappedMap[keyOfPairToBeCreated] = valueToBeAdded;
       }
    }

    override fun retrievePairingsValue(keyValueIsPairedWith: Key): Value? {
        return wrappedMap[keyValueIsPairedWith];
    }

    override fun updatePairingsValue(keyValueIsPairedWith: Key, newValueToPairKeyWith: Value) {
        if (wrappedMap.containsKey(keyValueIsPairedWith)) {
            wrappedMap[keyValueIsPairedWith] = newValueToPairKeyWith;
        }
    }

    override fun deletePairing(keyValueIsPairedWith: Key) {
        wrappedMap.remove(keyValueIsPairedWith)
    }

    override fun retrieveAllPairingsValues(): List<Value> {
        return wrappedMap.values.toList();
    }
}