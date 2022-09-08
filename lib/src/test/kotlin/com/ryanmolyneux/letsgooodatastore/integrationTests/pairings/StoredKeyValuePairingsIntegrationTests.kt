package com.ryanmolyneux.letsgooodatastore.integrationTests.pairings

import org.junit.Before;
import org.junit.Test
import org.junit.Assert;

import com.ryanmolyneux.letsgooodatastore.pairings.StoredKeyValuePairings;
import com.ryanmolyneux.letsgooodatastore.datastores.JsonFileManager;
import com.ryanmolyneux.letsgooodatastore.datastores.datastoreentries.Record;
import com.ryanmolyneux.letsgooodatastore.datastores.datastoreentries.Task;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken

import java.io.File;
import java.net.URI

class StoredKeyValuePairingsIntegrationTests {
    val jsonDatastoreUri = "file:///tmp/tmpJsonDatastore.json";

    @Before
    fun setUp() {
        deleteDatastore(jsonDatastoreUri);
    }

    @Test
    fun testCase1_coversTestPlanTestCondition1() {
        val typeOfDataBeingStored = object: TypeToken<MutableMap<String, Record>>() {}.type;
        val jsonFileManager = JsonFileManager<String, Record>(jsonDatastoreUri, Gson(), typeOfDataBeingStored);
        val storedKeyValuePairingsToTest: StoredKeyValuePairings<String, Record>;
        val recordExpectedToHaveBeenStoredAndRetrievedFromJsonDatastore = Record("Day1", null);

        createJsonFileManagerWithDatastoreWhichHoldsXStringRecordPairs(jsonDatastoreUri, recordExpectedToHaveBeenStoredAndRetrievedFromJsonDatastore);

        storedKeyValuePairingsToTest = StoredKeyValuePairings(mutableMapOf<String, Record>(), jsonFileManager);

        Assert.assertEquals(recordExpectedToHaveBeenStoredAndRetrievedFromJsonDatastore.name, storedKeyValuePairingsToTest.retrievePairingsValue("Day1")!!.name);
        Assert.assertNull(storedKeyValuePairingsToTest.retrievePairingsValue("Day1")!!.tasksOnRecord);
    }

    @Test
    fun testCase2_coversTestPlanTestCondition2() {
        val arrayOfRecordsExpectedToBeRetrievedFromJsonDatastore = arrayOf(
            Record("Day1", null),
                Record("Day2", null),
                Record("Day3", null)
        );
        val actualArrayOfRecordsRetrievedFromJsonDatastore: Array<Record>;
        val typeOfDataBeingStored = object: TypeToken<MutableMap<String, Record>>() {}.type;
        val jsonFileManager = JsonFileManager<String, Record>(jsonDatastoreUri, Gson(), typeOfDataBeingStored);
        val storedKeyValuePairingsToTest: StoredKeyValuePairings<String, Record>;

        createJsonFileManagerWithDatastoreWhichHoldsXStringRecordPairs( jsonDatastoreUri,
                                                                        arrayOfRecordsExpectedToBeRetrievedFromJsonDatastore[0],
                                                                        arrayOfRecordsExpectedToBeRetrievedFromJsonDatastore[1],
                                                                        arrayOfRecordsExpectedToBeRetrievedFromJsonDatastore[2]);

        storedKeyValuePairingsToTest = StoredKeyValuePairings(mutableMapOf<String, Record>(), jsonFileManager);

        actualArrayOfRecordsRetrievedFromJsonDatastore =  storedKeyValuePairingsToTest.retrieveAllPairingsValues().toTypedArray();

        for ( actualRecordRetrieved in actualArrayOfRecordsRetrievedFromJsonDatastore ) {

            var actualRecordRetrievedIsEqualToAtleastOneOfTheExpectedRecords = false;

            for ( expectedRecordToBeRetrieved in arrayOfRecordsExpectedToBeRetrievedFromJsonDatastore ) {

                if ( actualRecordRetrieved.name == expectedRecordToBeRetrieved.name ) {
                    actualRecordRetrievedIsEqualToAtleastOneOfTheExpectedRecords = true;
                    break;
                }

            }

            Assert.assertTrue(actualRecordRetrievedIsEqualToAtleastOneOfTheExpectedRecords);
        }
    }

    @Test
    fun testCase3_coversTestPlanTestCondition3() {
        val typeOfDataBeingStored = object: TypeToken<MutableMap<String, Record>>() {}.type;
        val jsonFileManager = JsonFileManager<String, Record>(jsonDatastoreUri, Gson(), typeOfDataBeingStored);
        val storedKeyValuePairings = StoredKeyValuePairings(mutableMapOf<String, Record>(), jsonFileManager);

        Assert.assertTrue(storedKeyValuePairings.retrieveAllPairingsValues().isEmpty());
    }

    @Test
    fun testCase4_coversTestPlanTestCondition4() {
        val typeOfDataBeingStored = object: TypeToken<MutableMap<String, Record>>() {}.type;
        val jsonFileManager = JsonFileManager<String, Record>(jsonDatastoreUri, Gson(), typeOfDataBeingStored);
        val storedKeyValuePairings = StoredKeyValuePairings(mutableMapOf<String, Record>(), jsonFileManager);
        val recordExpectedToBeAddedToJsonDatstore = Record("Day1", arrayOf(Task("Task1", Task.TASK_STATUS_COMPLETE)));
        val recordActuallyAddedToJsonDatastore: Record?;

        Assert.assertTrue(storedKeyValuePairings.retrieveAllPairingsValues().isEmpty());

        storedKeyValuePairings.createPairing(recordExpectedToBeAddedToJsonDatstore.name, recordExpectedToBeAddedToJsonDatstore);

        recordActuallyAddedToJsonDatastore = jsonFileManager.read()[recordExpectedToBeAddedToJsonDatstore.name];

        Assert.assertEquals(recordExpectedToBeAddedToJsonDatstore.name, recordActuallyAddedToJsonDatastore!!.name);
        Assert.assertEquals(recordExpectedToBeAddedToJsonDatstore.tasksOnRecord[0].name, recordActuallyAddedToJsonDatastore!!.tasksOnRecord[0].name);
        Assert.assertEquals(recordExpectedToBeAddedToJsonDatstore.tasksOnRecord[0].currentStatus, recordActuallyAddedToJsonDatastore!!.tasksOnRecord[0].currentStatus);
    }

    @Test
    fun testCase5_coversTestPlanTestCondition5() {
        val typeOfDataBeingStored = object: TypeToken<MutableMap<String, Record>>() {}.type;
        val jsonFileManager = JsonFileManager<String, Record>(jsonDatastoreUri, Gson(), typeOfDataBeingStored);
        val storedKeyValuePairings = StoredKeyValuePairings<String, Record>(mutableMapOf<String, Record>(), jsonFileManager);
        val recordExpectedToBeUpdatedInJsonDatastore = Record("Day1", null);
        val recordPairingsMapBeforeUpdate: MutableMap<String, Record>;
        val recordPairingsMapAfterUpdate: MutableMap<String, Record>;

        storedKeyValuePairings.createPairing(recordExpectedToBeUpdatedInJsonDatastore.name, recordExpectedToBeUpdatedInJsonDatastore);

        recordPairingsMapBeforeUpdate = jsonFileManager.read();

        Assert.assertEquals(recordExpectedToBeUpdatedInJsonDatastore.name, recordPairingsMapBeforeUpdate[recordExpectedToBeUpdatedInJsonDatastore.name]?.name);
        Assert.assertNull(recordPairingsMapBeforeUpdate[recordExpectedToBeUpdatedInJsonDatastore.name]?.tasksOnRecord)

        recordExpectedToBeUpdatedInJsonDatastore.tasksOnRecord = arrayOf(Task("Task123", Task.TASK_STATUS_PENDING));

        storedKeyValuePairings.updatePairingsValue(recordExpectedToBeUpdatedInJsonDatastore.name, recordExpectedToBeUpdatedInJsonDatastore);

        recordPairingsMapAfterUpdate = jsonFileManager.read();

        Assert.assertEquals(recordExpectedToBeUpdatedInJsonDatastore.name, recordPairingsMapAfterUpdate[recordExpectedToBeUpdatedInJsonDatastore.name]!!.name);
        Assert.assertEquals(recordExpectedToBeUpdatedInJsonDatastore.tasksOnRecord[0].name, recordPairingsMapAfterUpdate[recordExpectedToBeUpdatedInJsonDatastore.name]!!.tasksOnRecord[0].name);
        Assert.assertEquals(recordExpectedToBeUpdatedInJsonDatastore.tasksOnRecord[0].currentStatus, recordPairingsMapAfterUpdate[recordExpectedToBeUpdatedInJsonDatastore.name]!!.tasksOnRecord[0].currentStatus);
    }

    @Test
    fun testCase6_coversTestPlanTestCondition6() {
        val typeOfDataBeingStored = object: TypeToken<Map<String, Record>>() {}.type;
        val jsonFileManager = JsonFileManager<String, Record>(jsonDatastoreUri, Gson(), typeOfDataBeingStored);
        val storedKeyValuePairings = StoredKeyValuePairings<String, Record>(mutableMapOf<String, Record>(), jsonFileManager);
        val recordOneInitiallyStoredInJsonDatastore = Record("Day1", null);
        val recordTwoInitiallyStoredInJsonDatastore = Record("Day2", null);
        val recordPairingsMapBeforeDeleteCalledOnRecordTwo: MutableMap<String, Record>;
        val recordPairingsMapAfterDeleteCalledOnRecordTwo: MutableMap<String, Record>;

        storedKeyValuePairings.createPairing(recordOneInitiallyStoredInJsonDatastore.name, recordOneInitiallyStoredInJsonDatastore);
        storedKeyValuePairings.createPairing(recordTwoInitiallyStoredInJsonDatastore.name, recordTwoInitiallyStoredInJsonDatastore);

        recordPairingsMapBeforeDeleteCalledOnRecordTwo = jsonFileManager.read();

        Assert.assertEquals(2, recordPairingsMapBeforeDeleteCalledOnRecordTwo.size);
        Assert.assertNotNull(recordPairingsMapBeforeDeleteCalledOnRecordTwo[recordOneInitiallyStoredInJsonDatastore.name]);
        Assert.assertNotNull(recordPairingsMapBeforeDeleteCalledOnRecordTwo[recordTwoInitiallyStoredInJsonDatastore.name]);

        storedKeyValuePairings.deletePairing(recordTwoInitiallyStoredInJsonDatastore.name);

        recordPairingsMapAfterDeleteCalledOnRecordTwo = jsonFileManager.read();

        Assert.assertEquals(1, recordPairingsMapAfterDeleteCalledOnRecordTwo.size);
        Assert.assertNotNull(recordPairingsMapAfterDeleteCalledOnRecordTwo[recordOneInitiallyStoredInJsonDatastore.name]);
        Assert.assertNull(recordPairingsMapAfterDeleteCalledOnRecordTwo[recordTwoInitiallyStoredInJsonDatastore.name]);
    }

    fun createJsonFileManagerWithDatastoreWhichHoldsXStringRecordPairs(datastoreUri: String, vararg recordsToBeStored: Record) {
        val typeOfDataBeingStored = object: TypeToken<Map<String, Record>>() {}.type;
        val jsonFileManager = JsonFileManager<String, Record>(datastoreUri, Gson(), typeOfDataBeingStored);
        val jsonDatastoreMap = HashMap<String, Record>();

        for (record: Record in recordsToBeStored) {
            jsonDatastoreMap[record.name] = record;
        }

        jsonFileManager.write(jsonDatastoreMap);
    }

    fun deleteDatastore(datastoreUri: String) {
        File(URI(datastoreUri).path).delete();
    }
}