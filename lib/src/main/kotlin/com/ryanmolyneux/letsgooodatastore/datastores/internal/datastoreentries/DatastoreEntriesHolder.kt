package com.ryanmolyneux.letsgooodatastore.datastores.internal.datastoreentries

import com.ryanmolyneux.letsgooodatastore.datastores.datastoreentries.AbsDatastoreEntry

/**
 * Internal class to hold entries to the datastore so as to attach
 * additional information internally to each entry without the need
 * for it to be exposed to outside of the datastore.
 */
internal class DatastoreEntryHolder<Value: AbsDatastoreEntry>(
    val entry: Value,
    val order: Long = System.currentTimeMillis()
) : AbsDatastoreEntry();