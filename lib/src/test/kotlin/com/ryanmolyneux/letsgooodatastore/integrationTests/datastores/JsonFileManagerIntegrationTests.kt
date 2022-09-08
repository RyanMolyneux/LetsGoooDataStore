package com.ryanmolyneux.letsgooodatastore.integrationTests.datastores

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ryanmolyneux.letsgooodatastore.datastores.JsonFileManager
import com.ryanmolyneux.letsgooodatastore.datastores.datastoreentries.Record
import com.ryanmolyneux.letsgooodatastore.datastores.datastoreentries.Task
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.net.URI

class JsonFileManagerIntegrationTests {
    val jsonDatastoreUri = "file:///tmp/tempJsonDatastore.json";
    val jsonDatastorePath = URI(jsonDatastoreUri).path;

    @Before
    fun setup() {
        deleteFile(jsonDatastorePath);
    }

    @Test
    fun testCase1_coversTestPlanTestConditionNumber1() {
        val jsonDatastoreFileObject = File(jsonDatastorePath)
        var mapTypeBeingStoredInJsonDatastore = object: TypeToken<MutableMap<String, Record>> () {}.type;
        var jsonFileManagerObjectToTest: JsonFileManager<String, Record>;

        Assert.assertFalse(jsonDatastoreFileObject.exists());

        jsonFileManagerObjectToTest = JsonFileManager<String, Record>(jsonDatastoreUri, Gson(), mapTypeBeingStoredInJsonDatastore);

        Assert.assertTrue(jsonDatastoreFileObject.exists());
    }

    @Test
    fun testCase2_coversTestPlanTestConditionNumber2() {
        val fileWriterSpy = Mockito.spy(FileWriter(jsonDatastorePath));
        val mapTypeBeingStoredInJsonDatastore = object: TypeToken<MutableMap<String, Record>> () {}.type;
        val jsonFileManagerSpyToTest = Mockito.spy(JsonFileManager<String, Record>(jsonDatastoreUri, Gson(), mapTypeBeingStoredInJsonDatastore));
        val initialStringRecordPairsInJsonDataStore = HashMap<String, Record>();
        val stringRecordPairsExpectedToBeInJsonDatastoreAfterMethodWriteCalled = HashMap<String, Record>();
        val stringRecordPairsReadFromJsonDatastore: MutableMap<String, Record>;
        val day1Record = "Day1";
        val day2Record = "Day2";
        val day3Record = "Day3";

        initialStringRecordPairsInJsonDataStore[day1Record] = Record(day1Record, null)
        initialStringRecordPairsInJsonDataStore[day2Record] = Record(day2Record, null);

        stringRecordPairsExpectedToBeInJsonDatastoreAfterMethodWriteCalled[day3Record] = Record(day3Record, arrayOf(Task("Lets Gooo!!!", Task.TASK_STATUS_PENDING)));

        jsonFileManagerSpyToTest.write(initialStringRecordPairsInJsonDataStore);

        Mockito.doReturn(fileWriterSpy).`when`(jsonFileManagerSpyToTest).getNewFileWriter();

        jsonFileManagerSpyToTest.write(stringRecordPairsExpectedToBeInJsonDatastoreAfterMethodWriteCalled);

        stringRecordPairsReadFromJsonDatastore = jsonFileManagerSpyToTest.read();

        Assert.assertEquals(stringRecordPairsExpectedToBeInJsonDatastoreAfterMethodWriteCalled[day3Record]!!.name, stringRecordPairsReadFromJsonDatastore[day3Record]!!.name);
        Assert.assertEquals(stringRecordPairsExpectedToBeInJsonDatastoreAfterMethodWriteCalled[day3Record]!!.tasksOnRecord[0].name, stringRecordPairsReadFromJsonDatastore[day3Record]!!.tasksOnRecord[0].name);
        Assert.assertEquals(stringRecordPairsExpectedToBeInJsonDatastoreAfterMethodWriteCalled[day3Record]!!.tasksOnRecord[0].currentStatus, stringRecordPairsReadFromJsonDatastore[day3Record]!!.tasksOnRecord[0].currentStatus);

        Mockito.verify(fileWriterSpy, times(1)).close();
    }

    @Test
    fun testCase3_coversTestPlanTestConditionNumber3() {
        val mapTypeBeingStoredInJsonDatastore = object: TypeToken<MutableMap<String, Record>>() {}.type;
        val jsonFileManagerSpyToTest = Mockito.spy(JsonFileManager<String, Record>(jsonDatastoreUri, Gson(), mapTypeBeingStoredInJsonDatastore));
        val fileReaderSpy = Mockito.spy(FileReader(jsonDatastorePath));
        val stringRecordPairsExpectedToBeInJsonDatastore = HashMap<String, Record>();
        val stringRecordPairsReadFromJsonDatastore: MutableMap<String, Record>;
        val day4Record = "Day4";
        val day5Record = "Day5";
        val day6Record = "Day6";

        stringRecordPairsExpectedToBeInJsonDatastore[day4Record] = Record(day4Record, arrayOf(Task("Task1", Task.TASK_STATUS_COMPLETE)));
        stringRecordPairsExpectedToBeInJsonDatastore[day5Record] = Record(day5Record, null);
        stringRecordPairsExpectedToBeInJsonDatastore[day6Record] = Record(day6Record, null);

        jsonFileManagerSpyToTest.write(stringRecordPairsExpectedToBeInJsonDatastore);

        Mockito.doReturn(fileReaderSpy).`when`(jsonFileManagerSpyToTest).getNewFileReader();

        stringRecordPairsReadFromJsonDatastore = jsonFileManagerSpyToTest.read();

        Assert.assertEquals(stringRecordPairsExpectedToBeInJsonDatastore.size, stringRecordPairsReadFromJsonDatastore.size);

        Assert.assertEquals(stringRecordPairsExpectedToBeInJsonDatastore[day4Record]!!.name, stringRecordPairsReadFromJsonDatastore[day4Record]!!.name);
        Assert.assertEquals(stringRecordPairsExpectedToBeInJsonDatastore[day4Record]!!.tasksOnRecord[0].name, stringRecordPairsReadFromJsonDatastore[day4Record]!!.tasksOnRecord[0].name);
        Assert.assertEquals(stringRecordPairsExpectedToBeInJsonDatastore[day4Record]!!.tasksOnRecord[0].currentStatus, stringRecordPairsReadFromJsonDatastore[day4Record]!!.tasksOnRecord[0].currentStatus);

        Assert.assertEquals(stringRecordPairsExpectedToBeInJsonDatastore[day5Record]!!.name, stringRecordPairsExpectedToBeInJsonDatastore[day5Record]!!.name);

        Assert.assertEquals(stringRecordPairsExpectedToBeInJsonDatastore[day6Record]!!.name, stringRecordPairsReadFromJsonDatastore[day6Record]!!.name);

        Mockito.verify(fileReaderSpy, times(1)).close();
    }

    @Test
    fun testCase4_coversTestPlanTestCondition4() {
        val datastoreFileToTestWith = File(URI("file:///tmp/testJsonDatastore4.json").path);
        val datastoreDataIntegrityProtectionFileExpectedToExistOnceDatastoreFileIsCreated = File(URI("file:///tmp/testJsonDatastore4DataIntegrityProtectionDatastore.json").path);
        val jsonFileManagerToTestWith: JsonFileManager<String, Record>;
        val jsonFileManagerTypeToStore = object: TypeToken<MutableMap<String, Record>>() {}.type

        Assert.assertFalse(datastoreFileToTestWith.exists());
        Assert.assertFalse(datastoreDataIntegrityProtectionFileExpectedToExistOnceDatastoreFileIsCreated.exists())

        jsonFileManagerToTestWith = JsonFileManager<String, Record>(datastoreFileToTestWith.toURI().toString(), Gson(), jsonFileManagerTypeToStore);

        Assert.assertTrue(datastoreFileToTestWith.exists());
        Assert.assertTrue(datastoreDataIntegrityProtectionFileExpectedToExistOnceDatastoreFileIsCreated.exists());

        deleteFile(datastoreFileToTestWith.path)
        deleteFile(datastoreDataIntegrityProtectionFileExpectedToExistOnceDatastoreFileIsCreated.path)
    }

    @Test
    fun testCase5_coversTestPlanTestCondition5() {
        val datastoreFileUriToTestWith = "file:///tmp/testJsonDatastore5.json"
        val datastoreFileToTestWith = File(URI(datastoreFileUriToTestWith).path);
        val jsonFileManagerToTestWith: JsonFileManager<String, Record>;
        val typeOfDataForJsonFileManagerToStore = object: TypeToken<MutableMap<String, Record>>() {}.type
        val mapOfStringAndRecordsToTestIfTheyWillBePersisted = HashMap<String, Record>();
        val keyOfEntryOneExpectedToBePersisted = "TestEntry1";
        val keyOfEntryTwoExpectedToBePersisted = "TestEntry2";
        val valueOfEntryOneExpectedToBePersisted = Record("TestRecord1",
                                                                arrayOf(Task("TestTask1", Task.TASK_STATUS_PENDING)));
        val valueOfEntryTwoExpectedToBePersisted = Record( "TestRecord2", arrayOf());
        val actualMapOfStringRecordPairingsPersistedByAndRetrievedFromJsonFileManager: Map<String, Record>
        val actualPersistedValueOfEntryOne: Record
        val actualPersistedValueOfEntryTwo: Record

        deleteFile(datastoreFileToTestWith.path)

        mapOfStringAndRecordsToTestIfTheyWillBePersisted.put(keyOfEntryOneExpectedToBePersisted, valueOfEntryOneExpectedToBePersisted);
        mapOfStringAndRecordsToTestIfTheyWillBePersisted.put(keyOfEntryTwoExpectedToBePersisted, valueOfEntryTwoExpectedToBePersisted);

        Assert.assertFalse(datastoreFileToTestWith.exists())

        jsonFileManagerToTestWith = JsonFileManager(datastoreFileUriToTestWith, Gson(), typeOfDataForJsonFileManagerToStore);

        jsonFileManagerToTestWith.merge(mapOfStringAndRecordsToTestIfTheyWillBePersisted, listOf(keyOfEntryOneExpectedToBePersisted, keyOfEntryTwoExpectedToBePersisted));

        actualMapOfStringRecordPairingsPersistedByAndRetrievedFromJsonFileManager = jsonFileManagerToTestWith.read()


        Assert.assertTrue(actualMapOfStringRecordPairingsPersistedByAndRetrievedFromJsonFileManager.contains(keyOfEntryOneExpectedToBePersisted));
        Assert.assertTrue(actualMapOfStringRecordPairingsPersistedByAndRetrievedFromJsonFileManager.contains(keyOfEntryTwoExpectedToBePersisted));

        actualPersistedValueOfEntryOne = actualMapOfStringRecordPairingsPersistedByAndRetrievedFromJsonFileManager.get(keyOfEntryOneExpectedToBePersisted)!!;

        Assert.assertEquals(valueOfEntryOneExpectedToBePersisted.name, actualPersistedValueOfEntryOne.name)
        Assert.assertEquals(valueOfEntryOneExpectedToBePersisted.tasksOnRecord[0].name, actualPersistedValueOfEntryOne.tasksOnRecord[0].name)
        Assert.assertEquals(valueOfEntryOneExpectedToBePersisted.tasksOnRecord[0].currentStatus, actualPersistedValueOfEntryOne.tasksOnRecord[0].currentStatus)

        actualPersistedValueOfEntryTwo = actualMapOfStringRecordPairingsPersistedByAndRetrievedFromJsonFileManager.get(keyOfEntryTwoExpectedToBePersisted)!!;

        Assert.assertEquals(valueOfEntryTwoExpectedToBePersisted.name, actualPersistedValueOfEntryTwo.name);
        Assert.assertEquals(valueOfEntryTwoExpectedToBePersisted.tasksOnRecord.size, actualPersistedValueOfEntryTwo.tasksOnRecord.size);

    }

    @Test
    fun testCase6_coversTestPlanTestCondition6() {
        val datastoreUriToTestWith = "file:///tmp/testJsonDatastore6.json";
        val typeOfDataForJsonFileManagerToStore = object: TypeToken<MutableMap<String, Record>>() {}.type
        val keyOfPrexistingEntryOne = "TestEntry1";
        val valueOfPrexistingEntryOne = Record(keyOfPrexistingEntryOne, arrayOf( Task("Task1", Task.TASK_STATUS_PENDING) ) );
        val keyOfPrexistingEntryTwo = "TestEntry456";
        val valueOfPreexistingEntryTwo = Record(keyOfPrexistingEntryTwo, arrayOf( Task("Task789", Task.TASK_STATUS_COMPLETE) ));
        val mapOfPrexistingEntriesDatastoreIsExpectedToHave = mutableMapOf<String, Record>( Pair(keyOfPrexistingEntryOne, valueOfPrexistingEntryOne),
                                                                                            Pair(keyOfPrexistingEntryTwo, valueOfPreexistingEntryTwo) );
        val jsonFileManager1: JsonFileManager<String, Record>;
        val keyOfEntryThreeForJsonFileManager1ToAdd = "TestEntry101112";
        val valueOfEntryThreeForJsonFileManagerToAdd = Record(keyOfEntryThreeForJsonFileManager1ToAdd, arrayOf( Task("62", Task.TASK_STATUS_PENDING) ))
        val keyOfEntryFourForJsonFileManager1ToAdd = "TestEntry74";
        val valueOfEntryFourForJsonFileManager1ToAdd = Record(keyOfEntryFourForJsonFileManager1ToAdd, arrayOf());
        val jsonFileManager2: JsonFileManager<String, Record>;
        val updatedValueOfEntryTwoForJsonFileManager2ToAdd = Record(keyOfPrexistingEntryTwo, arrayOf( Task("Task789", Task.TASK_STATUS_COMPLETE),
                                                                                                      Task( "Task94", Task.TASK_STATUS_PENDING)  ))
        val actualPersistedPairsOfDatastoreDataPostBothMergeCalls: Map<String, Record>;
        val gson = Gson()
        deleteFile(URI(datastoreUriToTestWith).path);

        JsonFileManager<String, Record>(datastoreUriToTestWith, gson, typeOfDataForJsonFileManagerToStore).apply {
            write(mapOfPrexistingEntriesDatastoreIsExpectedToHave);
        }

        jsonFileManager1 = Mockito.spy(JsonFileManager<String, Record>(datastoreUriToTestWith, gson, typeOfDataForJsonFileManagerToStore));
        jsonFileManager2 = Mockito.spy(JsonFileManager<String, Record>(datastoreUriToTestWith, gson, typeOfDataForJsonFileManagerToStore));

        val jsonFileManager1InMemoryCopyOfDatastoreDataToMerge = mutableMapOf( Pair(keyOfPrexistingEntryOne, valueOfPrexistingEntryOne),
                                                                               Pair(keyOfPrexistingEntryTwo, valueOfPreexistingEntryTwo),
                                                                               Pair(keyOfEntryThreeForJsonFileManager1ToAdd, valueOfEntryThreeForJsonFileManagerToAdd),
                                                                               Pair(keyOfEntryFourForJsonFileManager1ToAdd, valueOfEntryFourForJsonFileManager1ToAdd)
        );



        jsonFileManager1.merge(jsonFileManager1InMemoryCopyOfDatastoreDataToMerge, listOf(keyOfEntryThreeForJsonFileManager1ToAdd, keyOfEntryFourForJsonFileManager1ToAdd));

        val jsonFileManager2InMemoryCopyOfDatatoreDataToMerge = mutableMapOf<String, Record>( Pair(keyOfPrexistingEntryTwo, updatedValueOfEntryTwoForJsonFileManager2ToAdd) );

        jsonFileManager2.merge(jsonFileManager2InMemoryCopyOfDatatoreDataToMerge, listOf(keyOfPrexistingEntryOne, keyOfPrexistingEntryTwo));

        actualPersistedPairsOfDatastoreDataPostBothMergeCalls = JsonFileManager<String, Record>(datastoreUriToTestWith, Gson(), typeOfDataForJsonFileManagerToStore).read()

        Assert.assertFalse(actualPersistedPairsOfDatastoreDataPostBothMergeCalls.contains(keyOfPrexistingEntryOne));
        Assert.assertTrue(actualPersistedPairsOfDatastoreDataPostBothMergeCalls.containsKey(keyOfPrexistingEntryTwo));

        val actualValueOfEntryTwo = actualPersistedPairsOfDatastoreDataPostBothMergeCalls.get(keyOfPrexistingEntryTwo);

        Assert.assertEquals(updatedValueOfEntryTwoForJsonFileManager2ToAdd.name, actualValueOfEntryTwo!!.name)
        Assert.assertEquals(updatedValueOfEntryTwoForJsonFileManager2ToAdd.tasksOnRecord[0].name, actualValueOfEntryTwo!!.tasksOnRecord[0].name)
        Assert.assertEquals(updatedValueOfEntryTwoForJsonFileManager2ToAdd.tasksOnRecord[0].currentStatus, actualValueOfEntryTwo!!.tasksOnRecord[0].currentStatus)
        Assert.assertEquals(updatedValueOfEntryTwoForJsonFileManager2ToAdd.tasksOnRecord[1].name, actualValueOfEntryTwo!!.tasksOnRecord[1].name)
        Assert.assertEquals(updatedValueOfEntryTwoForJsonFileManager2ToAdd.tasksOnRecord[1].currentStatus, actualValueOfEntryTwo!!.tasksOnRecord[1].currentStatus)

        val actualValueOfEntryThree = actualPersistedPairsOfDatastoreDataPostBothMergeCalls.get(keyOfEntryThreeForJsonFileManager1ToAdd);

        Assert.assertEquals(valueOfEntryThreeForJsonFileManagerToAdd.name, actualValueOfEntryThree!!.name)
        Assert.assertEquals(valueOfEntryThreeForJsonFileManagerToAdd.tasksOnRecord[0].name, actualValueOfEntryThree!!.tasksOnRecord[0].name)
        Assert.assertEquals(valueOfEntryThreeForJsonFileManagerToAdd.tasksOnRecord[0].currentStatus, actualValueOfEntryThree!!.tasksOnRecord[0].currentStatus);
        Assert.assertEquals(valueOfEntryThreeForJsonFileManagerToAdd.tasksOnRecord.size, actualValueOfEntryThree.tasksOnRecord.size);

        val actualValueOfEntryFour = actualPersistedPairsOfDatastoreDataPostBothMergeCalls.get(keyOfEntryFourForJsonFileManager1ToAdd);

        Assert.assertEquals(valueOfEntryFourForJsonFileManager1ToAdd.name, actualValueOfEntryFour!!.name);
        Assert.assertEquals(valueOfEntryFourForJsonFileManager1ToAdd.tasksOnRecord.size, actualValueOfEntryFour!!.tasksOnRecord.size)
    }

    fun deleteFile(pathToFile: String) {
        File(pathToFile).delete();
    }
}