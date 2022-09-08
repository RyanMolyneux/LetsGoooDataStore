package com.ryanmolyneux.letsgooodatastore.unitTests.pairings

import org.junit.Test;
import org.junit.Assert;

import com.ryanmolyneux.letsgooodatastore.pairings.KeyValuePairings;
import com.ryanmolyneux.letsgooodatastore.datastores.datastoreentries.Record;
import com.ryanmolyneux.letsgooodatastore.datastores.datastoreentries.Task;

class KeyValuePairingsUnitTests {

    @Test
    fun testCase1_coversTestPlanTestCondition1() {
        val keyValuePairingsObjectTesting = KeyValuePairings(HashMap<String, Record>());
        val taskToStoredInRecord = Task("I'M IN NEED OF FOOOOD!!!", Task.TASK_STATUS_PENDING);
        val recordToStoreInKeyValuePairings = Record("DayOne", arrayOf(taskToStoredInRecord));
        val recordRetrievedFromKeyValuePairingsObjectToTestAgainstOriginalForEquality: Record;

        keyValuePairingsObjectTesting.createPairing(recordToStoreInKeyValuePairings.name, recordToStoreInKeyValuePairings);

        recordRetrievedFromKeyValuePairingsObjectToTestAgainstOriginalForEquality = keyValuePairingsObjectTesting.retrievePairingsValue( recordToStoreInKeyValuePairings.name ) as Record;

        Assert.assertEquals(recordToStoreInKeyValuePairings.name, recordRetrievedFromKeyValuePairingsObjectToTestAgainstOriginalForEquality.name);
        Assert.assertEquals(recordToStoreInKeyValuePairings.tasksOnRecord[0].name, recordRetrievedFromKeyValuePairingsObjectToTestAgainstOriginalForEquality.tasksOnRecord[0].name);
        Assert.assertEquals(recordToStoreInKeyValuePairings.tasksOnRecord[0].currentStatus, recordRetrievedFromKeyValuePairingsObjectToTestAgainstOriginalForEquality.tasksOnRecord[0].currentStatus);
    }

    @Test
    fun testCase2_coversTestPlanTestCondition2() {
        val keyValuePairingsObjectTesting = KeyValuePairings(HashMap<String, Record>());
        val chosenKeyToTestWith = "GoodDaySir!!!";

        Assert.assertNull( keyValuePairingsObjectTesting.retrievePairingsValue(chosenKeyToTestWith) );
    }

    @Test
    fun testCase3_coversTestPlanTestCondition3() {
        val keyValuePairingsObjectTesting = KeyValuePairings(HashMap<String, Record>());
        val arrayOfTasksWithTwoEntriesToTestWith = arrayOf(Task("I GOTS FOOOD!!!", Task.TASK_STATUS_COMPLETE), Task("NEED MORE FOOD!!!", Task.TASK_STATUS_PENDING));
        val originalRecordAddedAsPairingToKeyValuePairingsObject = Record("Day1", null);
        val updatedRecordAddedAsPairingToKeyValuePairingsObject = Record("Day1", arrayOfTasksWithTwoEntriesToTestWith);
        var recordRetrievedFromKeyValuePairingsAfterUpdatePairingsValueMethodCalled: Record;

        keyValuePairingsObjectTesting.createPairing(originalRecordAddedAsPairingToKeyValuePairingsObject.name, originalRecordAddedAsPairingToKeyValuePairingsObject);

        keyValuePairingsObjectTesting.updatePairingsValue(updatedRecordAddedAsPairingToKeyValuePairingsObject.name, updatedRecordAddedAsPairingToKeyValuePairingsObject);

        recordRetrievedFromKeyValuePairingsAfterUpdatePairingsValueMethodCalled = keyValuePairingsObjectTesting.retrievePairingsValue( originalRecordAddedAsPairingToKeyValuePairingsObject.name ) as Record;

        Assert.assertEquals(updatedRecordAddedAsPairingToKeyValuePairingsObject.name, recordRetrievedFromKeyValuePairingsAfterUpdatePairingsValueMethodCalled.name);
        Assert.assertEquals(updatedRecordAddedAsPairingToKeyValuePairingsObject.tasksOnRecord[0].name, recordRetrievedFromKeyValuePairingsAfterUpdatePairingsValueMethodCalled.tasksOnRecord[0].name);
        Assert.assertEquals(updatedRecordAddedAsPairingToKeyValuePairingsObject.tasksOnRecord[0].currentStatus, recordRetrievedFromKeyValuePairingsAfterUpdatePairingsValueMethodCalled.tasksOnRecord[0].currentStatus);
        Assert.assertEquals(updatedRecordAddedAsPairingToKeyValuePairingsObject.tasksOnRecord[1].name, recordRetrievedFromKeyValuePairingsAfterUpdatePairingsValueMethodCalled.tasksOnRecord[1].name);
        Assert.assertEquals(updatedRecordAddedAsPairingToKeyValuePairingsObject.tasksOnRecord[1].currentStatus, recordRetrievedFromKeyValuePairingsAfterUpdatePairingsValueMethodCalled.tasksOnRecord[1].currentStatus);
    }

    @Test
    fun testCase4_coversTestPlanTestCondition4() {
        val keyValuePairingsObjectToTestWith = KeyValuePairings(HashMap<String, Record>());
        val recordToAddAsPairingToKeyValuePairingsObject = Record("Day1", null);

        keyValuePairingsObjectToTestWith.createPairing(recordToAddAsPairingToKeyValuePairingsObject.name, recordToAddAsPairingToKeyValuePairingsObject);

        keyValuePairingsObjectToTestWith.deletePairing(recordToAddAsPairingToKeyValuePairingsObject.name);

        Assert.assertNull( keyValuePairingsObjectToTestWith.retrievePairingsValue(recordToAddAsPairingToKeyValuePairingsObject.name) );
    }

    @Test
    fun testCase5_coversTestPlanTestCondition5() {
        val keyValuePairingsObjectToTestWith = KeyValuePairings(HashMap<String, Record>());
        val keyToTestWith = "Day10000 Sir!!!";

        try {
            keyValuePairingsObjectToTestWith.deletePairing(keyToTestWith);
        } catch(ex: Exception) {
            Assert.fail();
        }
    }

    @Test
    fun testCase6_coversTestPlanTestCondition6() {
        val keyValuePairingsObjectToTestWith = KeyValuePairings(HashMap<String, Record>());
        val arrayOfTasksStoreInRecord = arrayOf(Task("Do X, Y and Z", Task.TASK_STATUS_PENDING));
        val recordToBeAddedToKeyValuePairingsObject = Record("Day1", null);
        val secondRecordToBeAddedToKeyValuePairingsObject = Record("Day 2", arrayOfTasksStoreInRecord);
        val listOfRecordsRetrievedFromKeyValuePairingsObject: List<Record>;

        keyValuePairingsObjectToTestWith.createPairing(recordToBeAddedToKeyValuePairingsObject.name, recordToBeAddedToKeyValuePairingsObject);
        keyValuePairingsObjectToTestWith.createPairing(secondRecordToBeAddedToKeyValuePairingsObject.name, secondRecordToBeAddedToKeyValuePairingsObject);

        listOfRecordsRetrievedFromKeyValuePairingsObject = keyValuePairingsObjectToTestWith.retrieveAllPairingsValues();

        for (i in 0..1) {
            val recordRetrievedFromListOfRecords = listOfRecordsRetrievedFromKeyValuePairingsObject.get(i);

            if (recordRetrievedFromListOfRecords.name == recordToBeAddedToKeyValuePairingsObject.name) {
                Assert.assertNull(recordRetrievedFromListOfRecords.tasksOnRecord);
            } else {
                Assert.assertEquals(secondRecordToBeAddedToKeyValuePairingsObject.name, recordRetrievedFromListOfRecords.name);
                Assert.assertEquals(secondRecordToBeAddedToKeyValuePairingsObject.tasksOnRecord[0].name, recordRetrievedFromListOfRecords.tasksOnRecord[0].name);
                Assert.assertEquals(secondRecordToBeAddedToKeyValuePairingsObject.tasksOnRecord[0].currentStatus, recordRetrievedFromListOfRecords.tasksOnRecord[0].currentStatus);
            }
        }
    }

    @Test
    fun testCase7_coversTestPlanTestCondition7() {
        val keyValuePairings = KeyValuePairings(HashMap<String, Record>());
        val expectedSizeOfListOfValuesReturnedFromKeyValuePairingsObject = 0;

        Assert.assertEquals(expectedSizeOfListOfValuesReturnedFromKeyValuePairingsObject, keyValuePairings.retrieveAllPairingsValues().size);
    }
}