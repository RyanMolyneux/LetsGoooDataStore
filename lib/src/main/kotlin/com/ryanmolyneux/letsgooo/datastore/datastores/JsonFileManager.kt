package com.ryanmolyneux.letsgooo.datastore.datastores

import com.google.gson.Gson;

import com.ryanmolyneux.letsgooo.datastore.datastores.datastoreentries.AbsDatastoreEntry

import java.io.File;

import java.lang.reflect.Type
import java.net.URI

open class JsonFileManager<Key, Value: AbsDatastoreEntry>: AbsFileManager {
    private lateinit var gson: Gson;
    private var typeBeingStored: Type;

    constructor(datastoreUri: String, gson: Gson, typeBeingStored: Type): super(datastoreUri) {
        val fileToCheckForExistence = File(URI(getDatastoreUri()).path);

        setGson(gson);

        this.typeBeingStored = typeBeingStored;

        if (fileToCheckForExistence.exists() == false) {
            val fileWriter = getNewFileWriter();

            fileWriter.write("{}");

            fileWriter.close();
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

    fun read(): MutableMap<Key, Value> {
        val jsonDatastoreFileReader = getNewFileReader();
        val jsonDatastoreContentsInMapFormat: MutableMap<Key, Value>;

        jsonDatastoreContentsInMapFormat = gson.fromJson(jsonDatastoreFileReader, typeBeingStored);

        jsonDatastoreFileReader.close();

        return jsonDatastoreContentsInMapFormat;
    }
}