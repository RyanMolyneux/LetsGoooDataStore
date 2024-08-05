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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        clearCurrentDatastore()
        createTestDatastoreStorageDirectory()
        asyncStoredKeyValuePairings = AsyncStoredKeyValuePairings.newInstance(storeName, tempStorageDirOfStore, 4, 4)
    }

    @After
    fun teardown() {
        asyncStoredKeyValuePairings.close()
        printTempDatastoreFiles()
        clearCurrentDatastore()
    }

    @Test
    fun givenAsyncStoredKeyValuePairing_WhenMultipleAsyncWritesMade_ThenExpectEachWrittenValueIsInFactPersisted() {
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

            twoWayIteratorEmittedAllRecordsWritten = allPairingsValues.current.testCollectedBy({ currentValue ->
                if (currentValue != null) {
                    val value = currentValue as List<Record>
                    val result = value.exists { it.name == expectedRecordWritten1.name }
                              && value.exists {
                                it.name == expectedRecordWritten2.name
                                && it.tasksOnRecord!!.exists { (it.name == expectedTaskWritten1.name && it.currentStatus == expectedTaskWritten1.currentStatus) }
                              }
                              && value.exists { it.name == expectedRecordWritten3.name }

                    return@testCollectedBy result
                } else {
                    return@testCollectedBy false
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
            val stateFlowExpectedToReturnNullValue = MutableStateFlow<Record?>(null)
            val bgCollection = launch(Dispatchers.IO) {
                flowExpectedToReturnNullValue.collect {
                    stateFlowExpectedToReturnNullValue.value = it
                }
            }

            deleteCallCompletedSuccessfully = stateFlowExpectedToReturnNullValue.testNotCollectedBy({
                return@testNotCollectedBy ( it != null && it is Record )
            }, 1000L)

            bgCollection.cancel()
        }

        Assert.assertTrue(deleteCallCompletedSuccessfully)
    }

    @Test
    fun givenAsyncStoredKeyValuePairings_WhenExistingEntryDeleted_ThenExpectPostDeleteOpCallDeletePersisted() {
        var deleteCallCompletedSuccessfully = false
        val recordExpectedToBeExisting = Record("existing", emptyArray())

        runBlocking {
            asyncStoredKeyValuePairings.createPairing(recordExpectedToBeExisting.name, recordExpectedToBeExisting)

            asyncStoredKeyValuePairings.retrievePairingsValue(recordExpectedToBeExisting.name).testCollectedBy({
                return@testCollectedBy (it is Record && it.name == recordExpectedToBeExisting.name)
            })

            val flowExpectedToReturnNullValue = asyncStoredKeyValuePairings.deletePairing(recordExpectedToBeExisting.name)

            deleteCallCompletedSuccessfully = flowExpectedToReturnNullValue.testCollectedBy({
                return@testCollectedBy (it is Record && it.name == recordExpectedToBeExisting.name)
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
            val stateFlowExpectedToReturnNullValue = MutableStateFlow<Record?>(null)
            val stateFlowExpectedToNeverRetrieveValue = MutableStateFlow<Record?>(null)
            val bgCollectionJobs = mutableListOf<Job>()
            bgCollectionJobs += launch(Dispatchers.IO) {
                flowExpectedToReturnNullValue.collect {
                    stateFlowExpectedToReturnNullValue.value = it
                }
            }
            bgCollectionJobs += launch(Dispatchers.IO) {
                flowExpectedNeverToRetrieveValue.collect {
                    stateFlowExpectedToNeverRetrieveValue.value = it
                }
            }

            allWriteCallsWhereInFactNoOp = stateFlowExpectedToReturnNullValue.testNotCollectedBy({
                                            return@testNotCollectedBy (it != null && it is Record)
                                       }, 1000L)
                                       && stateFlowExpectedToNeverRetrieveValue.testNotCollectedBy({
                                            return@testNotCollectedBy (it is Record && it.name == recordExpectedNotBeBePersisted.name)
                                       }, 1000L)
            bgCollectionJobs.forEach { it.cancel() }
        }

        Assert.assertTrue(allWriteCallsWhereInFactNoOp)
    }

    @Test
    fun givenAsyncStoredKeyValuePairing_WhenSingleAsyncWriteMade_ThenExpectValueInFactPersisted() {
        var writeOpSuccessful = false
        val recordExpectedToBePersisted = Record("TestRecord", arrayOf(Task("Task1", Task.TASK_STATUS_COMPLETE)))

        runBlocking {
            asyncStoredKeyValuePairings.createPairing(recordExpectedToBePersisted.name, recordExpectedToBePersisted)
            val retrieveWrittenRecordFlow = asyncStoredKeyValuePairings.retrievePairingsValue(recordExpectedToBePersisted.name)
            val retrieveWrittenRecordStateFlow = MutableStateFlow<Record?>(null)
            val bgWriteOpResultCollection = launch(Dispatchers.IO) {
                retrieveWrittenRecordFlow.collect {
                    retrieveWrittenRecordStateFlow.value = it
                }
            }

            writeOpSuccessful = retrieveWrittenRecordStateFlow.testCollectedBy({
                val record = it as? Record
                val task = record?.tasksOnRecord?.first()
                record?.name == "TestRecord" && task?.name == "Task1" && task.currentStatus == Task.TASK_STATUS_COMPLETE
            })

            bgWriteOpResultCollection.cancel()
        }

        Assert.assertTrue(writeOpSuccessful)
    }

    @Test
    fun givenAsyncStoredKeyValuePairing_WhenExistingValueUpdated_ThenExpectUpdatedValuePersisted() {
        var initialWriteOpSuccessful = false
        var updateSuccessfullyPersisted = false
        val initialRecordPairingValue = Record("TestRecord1", arrayOf())
        val updatedRecordPairingValue = Record(initialRecordPairingValue.name, arrayOf(Task("TestTask1", Task.TASK_STATUS_PENDING)))

        runBlocking {
            asyncStoredKeyValuePairings.createPairing(initialRecordPairingValue.name, initialRecordPairingValue)
            val retrievedWrittenRecordFlow = asyncStoredKeyValuePairings.retrievePairingsValue(initialRecordPairingValue.name)
            val retrievedWrittenRecordStateFlow = MutableStateFlow<Record?>(null)
            val bgWriteOpResultCollection = launch(Dispatchers.IO) {
                retrievedWrittenRecordFlow.collect {
                    retrievedWrittenRecordStateFlow.value = it
                }
            }

            initialWriteOpSuccessful = retrievedWrittenRecordStateFlow.testCollectedBy({
                val record = it as? Record
                val tasks = record?.tasksOnRecord

                record?.name == "TestRecord1"&& tasks?.size == 0
            })

            bgWriteOpResultCollection.cancel()
        }

        Assert.assertTrue(initialWriteOpSuccessful)

        runBlocking {
            asyncStoredKeyValuePairings.createPairing(initialRecordPairingValue.name, updatedRecordPairingValue)
            val retrieveUpdatedRecordFlow = asyncStoredKeyValuePairings.retrievePairingsValue(updatedRecordPairingValue.name)
            val retrieveUpdatedRecordStateFlow = MutableStateFlow<Record?>(null)
            val bgWriteOpResultCollection = launch(Dispatchers.IO) {
                retrieveUpdatedRecordFlow.collect {
                    retrieveUpdatedRecordStateFlow.value = it
                }
            }

            updateSuccessfullyPersisted = retrieveUpdatedRecordStateFlow.testCollectedBy({
                val record = it as? Record
                val tasks = record?.tasksOnRecord
                val task = tasks?.first()

                record?.name == "TestRecord1" && tasks!!.isNotEmpty() && task?.name == "TestTask1" && task.currentStatus == Task.TASK_STATUS_PENDING
            })
            bgWriteOpResultCollection.cancel()
        }

        Assert.assertTrue(updateSuccessfullyPersisted)
    }

    @Test
    fun givenAsyncStoredKeyValuePairings_WhenNumOfItemsInStoredUpdated_ThenExpectEntryCountUpdatedToReflectCorrectCount() {
        val expectedNumOfEntriesDuringFirstCheck = 4L
        val expectedNumOfEntriesDuringSecondCheck = 2L
        val expectedNumOfEntriesDuringThirdCheck = 3L
        val expectedNumOfEntriesDuringLastCheck = 0L

        runBlocking {
            asyncStoredKeyValuePairings.createPairing("TestRecord1", Record("TestRecord1", null))
            asyncStoredKeyValuePairings.createPairing("TestRecord2", Record("TestRecord2", null))
            asyncStoredKeyValuePairings.createPairing("TestRecord3", Record("TestRecord3", null))
            asyncStoredKeyValuePairings.createPairing("TestRecord4", Record("TestRecord4", null))
            delay(500)
        }

        Assert.assertEquals(expectedNumOfEntriesDuringFirstCheck, asyncStoredKeyValuePairings.currentNumberOfEntries)

        runBlocking {
            asyncStoredKeyValuePairings.deletePairing("TestRecord2")
            asyncStoredKeyValuePairings.deletePairing("TestRecord3")
            delay(500)
        }

        Assert.assertEquals(expectedNumOfEntriesDuringSecondCheck, asyncStoredKeyValuePairings.currentNumberOfEntries)

        runBlocking {
            asyncStoredKeyValuePairings.createPairing("TestRecord62", Record("TestRecord62", arrayOf(Task("TestTask1", Task.TASK_STATUS_PENDING))))
            delay(500)
        }

        Assert.assertEquals(expectedNumOfEntriesDuringThirdCheck, asyncStoredKeyValuePairings.currentNumberOfEntries)

        runBlocking {
            asyncStoredKeyValuePairings.deletePairing("TestRecord1")
            asyncStoredKeyValuePairings.deletePairing("TestRecord4")
            asyncStoredKeyValuePairings.deletePairing("TestRecord62")
            delay(500)
        }

        Assert.assertEquals(expectedNumOfEntriesDuringLastCheck, asyncStoredKeyValuePairings.currentNumberOfEntries)
    }

    @Test
    fun givenMaxEntriesSetToSixteen_WhenOverTwentyFourValueWritesAttempted_ThenExpectNoOpForExtraWrites() {
        val maxEntries = 24
        val mutableStateFlows = mutableListOf<MutableStateFlow<Record?>>()

        runBlocking {
            for (i in 0..maxEntries) {
                asyncStoredKeyValuePairings.createPairing("TestRecord$i", Record("TestRecord$i", null))
            }
        }

        runBlocking {
            val bgCollections = mutableListOf<Job>()

            for (i in 0 .. maxEntries) {
                mutableStateFlows.add(MutableStateFlow(null))
            }

            for (i in 0..maxEntries) {
                bgCollections += launch(Dispatchers.IO) {
                    asyncStoredKeyValuePairings.retrievePairingsValue("TestRecord$i").collect {
                        mutableStateFlows[i].value = it
                    }
                }
            }

            delay(4000L)

            for (i in 0 .. maxEntries) {
                bgCollections[i].cancel()
            }
        }
        println(mutableStateFlows.map { it.value })
        for (i in 0 .. 15) {
            Assert.assertTrue(mutableStateFlows[i].value != null)
        }

        for (i in 16 .. 24) {
            Assert.assertEquals(null, mutableStateFlows[i].value)
        }
    }

    @Test
    fun givenHundredEntriesAttemptedCreation_WhenMaxEntries16AndPartitions4_ThenExpect4PartitionsAnd16Entries() {
        val maxPartitions = 4

        runBlocking {
            for (i in 0 .. 100) {
                asyncStoredKeyValuePairings.createPairing("TestRecord$i", Record("TestRecord$i", null))
            }
            delay(4000L)
        }

        /**
         * Must divide & remove one entry in order to ensure data integrity files and
         * async partition tracking datastore are not included in the check for whether
         * or not only the max amount of partitions expected to be created are.
         */
        Assert.assertEquals(maxPartitions, (File(tempStorageDirOfStore).listFiles().filterNot { it.name.contains("DataIntegrityProtectionDatastore") }.size - 1));
        Assert.assertEquals(16, asyncStoredKeyValuePairings.currentNumberOfEntries)
    }

    fun createTestDatastoreStorageDirectory() {
        println("Datastore test storage directory creation successful?: ${File(tempStorageDirOfStore).mkdir()}")
    }

    fun clearCurrentDatastore() {
        println("Datastore test storage directory deletion successful?: ${File(tempStorageDirOfStore).deleteRecursively()}")
    }

    fun printTempDatastoreFiles() {
        val fileList = File(tempStorageDirOfStore).listFiles()
        if (fileList != null) {
            for (i in 0..(fileList.size - 1)) {
                val file = fileList[i]
                println("${i+1} File(${file.name}) contents: ")
                println(fileList[i].readText())
            }
        }
    }

    fun <T> Array<T>.exists(predicate: (T) -> Boolean): Boolean {
        return (find(predicate) != null)
    }

    fun <T> Iterable<T>.exists(predicate: (T) -> Boolean): Boolean {
        return (find(predicate) != null)
    }

    suspend fun Flow<*>.testCollectedBy(checkValue: (currentValue: Any?) -> Boolean, timeoutMillis: Long = 2000L): Boolean {
        var timeoutNotHit = true
        val recheckDelayMillis = 100L
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

    suspend fun StateFlow<*>.testNotCollectedBy(checkValue: (currentValue: Any?) -> Boolean, timeoutMillis: Long = 2000L): Boolean {
        var timeoutNotHit = true
        val recheckDelayMillis = 100L
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