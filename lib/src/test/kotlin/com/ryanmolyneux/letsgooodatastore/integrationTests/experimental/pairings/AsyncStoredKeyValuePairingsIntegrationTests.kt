package com.ryanmolyneux.letsgooodatastore.integrationTests.experimental.pairings

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ryanmolyneux.letsgooodatastore.datastores.JsonFileManager
import com.ryanmolyneux.letsgooodatastore.datastores.datastoreentries.Record
import com.ryanmolyneux.letsgooodatastore.datastores.datastoreentries.Task
import com.ryanmolyneux.letsgooodatastore.experimental.pairings.AbsAsyncStoredKeyValuePairings
import com.ryanmolyneux.letsgooodatastore.experimental.pairings.AsyncStoredKeyValuePairings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.junit.Before
import org.junit.Test
import org.junit.After
import org.junit.Assert
import java.io.File
import java.net.URI

class AsyncStoredKeyValuePairingsIntegrationTests {
    private val storeName = "asyncDatastore"
    private val tempStorageDirOfStore = URI("file:///tmp/asyncDatastoreTestingDir/").path
    private lateinit var asyncStoredKeyValuePairings: AbsAsyncStoredKeyValuePairings<String, Record>

    @Before
    fun setup() {
        createTestDatastoreStorageDirectory()
        asyncStoredKeyValuePairings = AsyncStoredKeyValuePairings.newInstance(storeName, tempStorageDirOfStore)
    }

    @After
    fun teardown() {
        asyncStoredKeyValuePairings.close()
        clearCurrentDatastore()
    }

    @Test
    fun givenAsuncStoredKeyValuePairing_WhenMultipleAsyncWritesMade_ThenExpectEachWrittenValueIsInFactPersisted() {
        val expectedRecordWritten1 = Record("exp-record-1", emptyArray())
        val expectedTaskWritten1 = Task("exp-task-1", Task.TASK_STATUS_COMPLETE)
        val expectedRecordWritten2 = Record("exp-record-2", arrayOf(expectedTaskWritten1))
        val expectedRecordWritten3 = Record("exp-record-3", emptyArray())
        val expectedNumberOfPartitionsCreated = 1
        val twoWayIteratorEmittedAllRecordsWritten: Boolean
        val partitionDatastoreTypeToken = object: TypeToken<MutableMap<String, AsyncStoredKeyValuePairings.Partition>>() {}.type
        val recordDatastoreTypeToken = object : TypeToken<MutableMap<String, Record>>() {}.type
        val partitionDatastoreJsonFileManager = JsonFileManager<String, AsyncStoredKeyValuePairings.Partition>("$tempStorageDirOfStore$storeName.json", Gson(), partitionDatastoreTypeToken)
        val mapOfAllPartitions: MutableMap<String, AsyncStoredKeyValuePairings.Partition>
        val mapOfAllPersistedRecords: MutableMap<String, Record>

        runBlocking {
            val allPairingsValues = asyncStoredKeyValuePairings.retrieveAllPairingsValues()
            val ioDatastoreWriteJobs = mutableListOf<Job>()

            ioDatastoreWriteJobs += launch(Dispatchers.IO) {
                asyncStoredKeyValuePairings.createPairing(
                    expectedRecordWritten2.name,
                    expectedRecordWritten2
                )
            }
            ioDatastoreWriteJobs += launch(Dispatchers.IO) {
                asyncStoredKeyValuePairings.createPairing(
                    expectedRecordWritten1.name,
                    expectedRecordWritten1
                )
            }
            ioDatastoreWriteJobs += launch(Dispatchers.IO) {
                asyncStoredKeyValuePairings.createPairing(
                    expectedRecordWritten3.name,
                    expectedRecordWritten3
                )
            }

            twoWayIteratorEmittedAllRecordsWritten = allPairingsValues.current.testCollectBy({ currentValue ->
                if (currentValue != null) {
                    val value = currentValue as List<Record>
                    val result = value.exists { it.name == expectedRecordWritten1.name }
                              && value.exists {
                                it.name == expectedRecordWritten2.name
                                && it.tasksOnRecord!!.exists { (it.name == expectedTaskWritten1.name && it.currentStatus == expectedTaskWritten1.currentStatus) }
                              }
                              && value.exists { it.name == expectedRecordWritten3.name }

                    return@testCollectBy result
                } else {
                    return@testCollectBy false
                }
            })

            ioDatastoreWriteJobs.forEach {
                it.cancel()
            }

        }

        mapOfAllPartitions = partitionDatastoreJsonFileManager.read()

        val partitionOneRecordJsonFileManager = JsonFileManager<String, Record>("$tempStorageDirOfStore${mapOfAllPartitions.values.first().id}.json", Gson(), recordDatastoreTypeToken)

        mapOfAllPersistedRecords = partitionOneRecordJsonFileManager.read()

        Assert.assertTrue(twoWayIteratorEmittedAllRecordsWritten)
        Assert.assertEquals(expectedNumberOfPartitionsCreated, mapOfAllPartitions.values.count())
        Assert.assertTrue(mapOfAllPersistedRecords.containsKey(expectedRecordWritten1.name))
        Assert.assertEquals(expectedRecordWritten1.name, mapOfAllPersistedRecords[expectedRecordWritten1.name]!!.name)
        Assert.assertTrue(mapOfAllPersistedRecords.containsKey(expectedRecordWritten2.name))
        Assert.assertEquals(expectedRecordWritten2.name, mapOfAllPersistedRecords[expectedRecordWritten2.name]!!.name)
        Assert.assertTrue(mapOfAllPersistedRecords[expectedRecordWritten2.name]!!.tasksOnRecord.exists { it.name == expectedTaskWritten1.name && it.currentStatus == expectedTaskWritten1.currentStatus })
        Assert.assertTrue(mapOfAllPersistedRecords.containsKey(expectedRecordWritten3.name))
        Assert.assertEquals(expectedRecordWritten3.name, mapOfAllPersistedRecords[expectedRecordWritten3.name]!!.name)
    }

    @Test
    fun givenAsyncStoredKeyValuePairing_WhenDeleteNonExistingEntry_ThenExpectNoOpToOccur() {
        var deleteCallCompletedSuccessfully = false

        runBlocking {
            val flowExpectedToReturnNullValue = asyncStoredKeyValuePairings.deletePairing("non-existant-entry")

            deleteCallCompletedSuccessfully = flowExpectedToReturnNullValue.testNotCollectedBy({
                return@testNotCollectedBy ( it != null  && it is Record )
            }, 3000L)
        }

        Assert.assertTrue(deleteCallCompletedSuccessfully)
    }

    @Test
    fun givenAsyncStoredKeyValuePairings_WhenExistingEntryDeleted_ThenExpectPostDeleteOpCallDeletePersisted() {
        var deleteCallCompletedSuccessfully = false
        val recordExpectedToBeExisting = Record("existing", emptyArray())

        runBlocking {
            asyncStoredKeyValuePairings.createPairing(recordExpectedToBeExisting.name, recordExpectedToBeExisting)

            asyncStoredKeyValuePairings.retrievePairingsValue(recordExpectedToBeExisting.name).testCollectBy({
                return@testCollectBy (it is Record && it.name == recordExpectedToBeExisting.name)
            })

            val flowExpectedToReturnNullValue = asyncStoredKeyValuePairings.deletePairing(recordExpectedToBeExisting.name)

            deleteCallCompletedSuccessfully = flowExpectedToReturnNullValue.testCollectBy({
                return@testCollectBy (it is Record && it.name == recordExpectedToBeExisting.name)
            })
        }

        Assert.assertTrue(deleteCallCompletedSuccessfully)
    }

    @Test
    fun givenAsyncStoredKeyValuePairings_WhenWritesClosed_ThenExpectAllFollowWriteOpsAreNoOp() {
        var allWriteCallsWhereInFactNoOp = false

        asyncStoredKeyValuePairings.close()

        runBlocking {
            val recordExpectedNotBeBePersisted = Record("recordName", emptyArray())
            val flowExpectedToReturnNullValue = asyncStoredKeyValuePairings.deletePairing("name")
            val flowExpectedNeverToRetrieveValue: Flow<Record>

            asyncStoredKeyValuePairings.createPairing(recordExpectedNotBeBePersisted.name, recordExpectedNotBeBePersisted)

            flowExpectedNeverToRetrieveValue = asyncStoredKeyValuePairings.retrievePairingsValue(recordExpectedNotBeBePersisted.name)

            allWriteCallsWhereInFactNoOp = flowExpectedToReturnNullValue.testNotCollectedBy({
                                            return@testNotCollectedBy (it != null && it is Record)
                                       }, 4000L)
                                       && flowExpectedNeverToRetrieveValue.testNotCollectedBy({
                                            return@testNotCollectedBy (it is Record && it.name == recordExpectedNotBeBePersisted.name)
                                       }, 4000L)
        }

        Assert.assertTrue(allWriteCallsWhereInFactNoOp)
    }

    fun givenAsyncStoredKeyValuePairing_WhenSingleAsyncWriteMade_ThenExpectValueInFactPersisted() {
        TODO()
    }

    fun givenAsyncStoredKeyValuePairing_WhenExistingValueUpdated_ThenExpectUpdatedValuePersisted() {
        TODO()
    }

    fun givenAsyncStoredKeyValuePairings_WhenNumOfItemsInStoredUpdated_ThenExpectEntryCountUpdatedToReflectCorrectCount() {
        TODO()
    }

    fun createTestDatastoreStorageDirectory() {
        println("Datastore test storage directory creation successful?: ${File(tempStorageDirOfStore).mkdir()}")
    }

    fun clearCurrentDatastore() {
        println("Datastore test storage directory deletion successful?: ${File(tempStorageDirOfStore).deleteRecursively()}")
    }

    fun <T> Array<T>.exists(predicate: (T) -> Boolean): Boolean {
        return (find(predicate) != null)
    }

    fun <T> Iterable<T>.exists(predicate: (T) -> Boolean): Boolean {
        return (find(predicate) != null)
    }

    suspend fun Flow<*>.testCollectBy(checkValue: (currentValue: Any?) -> Boolean, timeoutMillis: Long = 5000L): Boolean {
        var timeoutNotHit = true
        val recheckDelayMillis = 1000L
        var recheckAttempts = 0
        while (timeoutNotHit) {
            val currentValue = firstOrNull()
            if (checkValue(currentValue))  {
                return true
            } else {
                if (timeoutMillis < (recheckAttempts * recheckDelayMillis)) {
                    timeoutNotHit = false
                } else {
                    recheckAttempts++
                    delay(recheckDelayMillis)
                }
            }
        }
        return false
    }

    suspend fun Flow<*>.testNotCollectedBy(checkValue: (currentValue: Any?) -> Boolean, timeoutMillis: Long = 5000L): Boolean {
        var timeoutNotHit = true
        val recheckDelayMillis = 1000L
        var recheckAttempts = 0
        while (timeoutNotHit) {
            val currentValue = firstOrNull()
            if (checkValue(currentValue))  {
                return false
            } else {
                if (timeoutMillis < (recheckAttempts * recheckDelayMillis)) {
                    timeoutNotHit = false
                } else {
                    recheckAttempts++
                    delay(recheckDelayMillis)
                }
            }
        }
        return true
    }
}