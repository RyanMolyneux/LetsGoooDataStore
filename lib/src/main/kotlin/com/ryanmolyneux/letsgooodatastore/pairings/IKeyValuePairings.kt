package com.ryanmolyneux.letsgooodatastore.pairings

interface IKeyValuePairings<Key, Value>: IPairings<Key, Value> {
    override fun createPairing(key: Key, value: Value);
    fun retrievePairingsValue(keyValueIsPairedWith: Key): Value?;
    fun updatePairingsValue(keyValueIsPairedWith: Key, newValueToPairKeyWith: Value): Unit;
    fun deletePairing(keyValueIsPairedWith: Key): Unit;
    fun retrieveAllPairingsValues(): Collection<Value>;
}