package com.ryanmolyneux.letsgooo.datastore.datastores

import com.google.gson.Gson;

import com.ryanmolyneux.letsgooo.datastore.datastores.datastoreentries.AbsDatastoreEntry

import java.io.File;
import java.io.FileReader
import java.io.FileWriter

import java.lang.reflect.Type
import java.net.URI
import java.time.Instant

open class JsonFileManager<Key, Value: AbsDatastoreEntry>: AbsFileManager {
    private val DEFAULT_LAST_MERGE_TIME_SECONDS: Long
                get() {
                    return -1;
                }
    private lateinit var gson: Gson;
    private var typeBeingStored: Type;
    private var lastMergeTimeMilliseconds: Long = DEFAULT_LAST_MERGE_TIME_SECONDS;
    private val DATA_INTEGRITY_PROTECTION_DATASTORE_SUFFIX = "DataIntegrityProtectionDatastore";

    constructor(datastoreUri: String, gson: Gson, typeBeingStored: Type): super(datastoreUri) {
        val fileToCheckForExistence = File(URI(getDatastoreUri()).path);

        setGson(gson);
        setDataIntegrityProtectionDatastoreUri("${datastoreUri.removeSuffix(".json")}$DATA_INTEGRITY_PROTECTION_DATASTORE_SUFFIX.json");

        this.typeBeingStored = typeBeingStored;

        if (fileToCheckForExistence.exists() == false) {
            val datastoreFileWriter = getNewFileWriter();
            val dataIntegrityProtectionFileWriter = FileWriter(File(URI(getDataIntegrityProtectionDatastoreUri()).path));

            datastoreFileWriter.write("{}");
            dataIntegrityProtectionFileWriter.write(gson.toJson(JsonFileManagerDataIntegrityProtectionData(DEFAULT_LAST_MERGE_TIME_SECONDS)));

            datastoreFileWriter.close();
            dataIntegrityProtectionFileWriter.close()
        }
    }

    fun getGson(): Gson {
        return gson;
    }

    fun setGson(gson: Gson) {
        this.gson = gson;
    }

     fun write(newKeyValueStoreContent: MutableMap<Key, Value>) {
        val jsonDatastoreFileWriter = getNewFileWriter();
        val newKeyValueStoreContentJsonFormat = gson.toJson(newKeyValueStoreContent);

        jsonDatastoreFileWriter.write(newKeyValueStoreContentJsonFormat);

        jsonDatastoreFileWriter.close();
    }

    fun merge(modifiedInMemoryCopyOfKeyValueDatastoreContent: Map<Key, Value>, keysWhosePairedValueHaveBeenChanged: List<Key>) {
        val jsonFileManagerDataIntegrityProtectionDatastoreURI = URI(getDataIntegrityProtectionDatastoreUri())
        val jsonFileManagerDataIntegrityProtectionDatastoreData = gson.fromJson( FileReader(jsonFileManagerDataIntegrityProtectionDatastoreURI.path),
                                                                                 JsonFileManagerDataIntegrityProtectionData::class.java);
        val jsonFileManagerDataIntegrityProtectionDatastoreFileWriter: FileWriter
        var combinedMapToBePersistedToDatastore: MutableMap<Key, Value> = modifiedInMemoryCopyOfKeyValueDatastoreContent.toMutableMap()

        if (lastMergeTimeMilliseconds != jsonFileManagerDataIntegrityProtectionDatastoreData.persistedLastMergeTimeMilliseconds) {
            combinedMapToBePersistedToDatastore = read()

            for (keyWhoseValueHasChanged in keysWhosePairedValueHaveBeenChanged) {
                if (modifiedInMemoryCopyOfKeyValueDatastoreContent.contains(keyWhoseValueHasChanged)) {
                    combinedMapToBePersistedToDatastore.put(keyWhoseValueHasChanged, modifiedInMemoryCopyOfKeyValueDatastoreContent.get(keyWhoseValueHasChanged)!!)
                } else {
                    combinedMapToBePersistedToDatastore.remove(keyWhoseValueHasChanged)
                }
            }
        }

        write(combinedMapToBePersistedToDatastore);

        lastMergeTimeMilliseconds = Instant.now().epochSecond

        jsonFileManagerDataIntegrityProtectionDatastoreFileWriter = FileWriter(jsonFileManagerDataIntegrityProtectionDatastoreURI.path)

        jsonFileManagerDataIntegrityProtectionDatastoreFileWriter.write(gson.toJson(JsonFileManagerDataIntegrityProtectionData(lastMergeTimeMilliseconds)));

        jsonFileManagerDataIntegrityProtectionDatastoreFileWriter.close()
    }

    fun read(): MutableMap<Key, Value> {
        val jsonDatastoreFileReader = getNewFileReader();
        val jsonDatastoreContentsInMapFormat: MutableMap<Key, Value>;

        jsonDatastoreContentsInMapFormat = gson.fromJson(jsonDatastoreFileReader, typeBeingStored);

        jsonDatastoreFileReader.close();

        return jsonDatastoreContentsInMapFormat;
    }

    private data class JsonFileManagerDataIntegrityProtectionData(val persistedLastMergeTimeMilliseconds: Long)
}