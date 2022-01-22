package com.ryanmolyneux.letsgooo.datastore.integrationTests.datastores

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ryanmolyneux.letsgooo.datastore.datastores.JsonFileManager
import com.ryanmolyneux.letsgooo.datastore.datastores.datastoreentries.Record
import com.ryanmolyneux.letsgooo.datastore.datastores.datastoreentries.Task
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

    fun deleteFile(pathToFile: String) {
        File(pathToFile).delete();
    }
}