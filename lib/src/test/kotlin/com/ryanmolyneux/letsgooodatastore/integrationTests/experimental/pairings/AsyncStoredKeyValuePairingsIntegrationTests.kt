package com.ryanmolyneux.letsgooodatastore.integrationTests.experimental.pairings

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ryanmolyneux.letsgooodatastore.datastores.JsonFileManager
import com.ryanmolyneux.letsgooodatastore.datastores.datastoreentries.Record
import com.ryanmolyneux.letsgooodatastore.datastores.datastoreentries.Task
import com.ryanmolyneux.letsgooodatastore.datastores.internal.datastoreentries.DatastoreEntryHolder
import com.ryanmolyneux.letsgooodatastore.experimental.pairings.AbsAsyncStoredKeyValuePairings
import com.ryanmolyneux.letsgooodatastore.experimental.pairings.AsyncStoredKeyValuePairings
import com.ryanmolyneux.letsgooodatastore.experimental.pairings.TwoWayIterator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.timeout
import org.junit.Before
import org.junit.Test
import org.junit.After
import org.junit.Assert
import java.io.File
import java.net.URI
import kotlin.time.Duration.Companion.milliseconds

class AsyncStoredKeyValuePairingsIntegrationTests {
    private val storeName = "asyncDatastore"
    private val tempStorageDirOfStore = URI("file:///tmp/asyncDatastoreTestingDir/").path
    private lateinit var asyncStoredKeyValuePairings: AbsAsyncStoredKeyValuePairings<String, Record>

    @Before
    fun setup() {
        clearCurrentDatastore()
        createTestDatastoreStorageDirectory()
        asyncStoredKeyValuePairings = AsyncStoredKeyValuePairings.newInstance(storeName, tempStorageDirOfStore, 4, 16)
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
        val expectedRecordWritten4 = Record("exp-record-4", emptyArray())
        val expectedRecordWritten5 = Record("exp-record-5", emptyArray())
        val expectedRecordWritten6 = Record("exp-record-6", emptyArray())
        val expectedRecordWritten7 = Record("exp-record-7", emptyArray())
        val expectedRecordWritten8 = Record("exp-record-8", emptyArray())
        val expectedRecordWritten9 = Record("exp-record-9", emptyArray())
        val expectedRecordWritten10 = Record("exp-record-10", emptyArray())

        val twoWayIteratorEmittedAllRecordsWrittenInPartition1: Boolean
        val twoWayIteratorEmittedAllRecordsWrittenInPartition2: Boolean
        val twoWayIteratorEmittedAllRecordsWrittenInPartition3: Boolean
        val twoWayIteratorEmittedAllRecordsWrittenInPartition4: Boolean
        val partitionDatastoreMetadataTypeToken = object: TypeToken<MutableMap<String, AsyncStoredKeyValuePairings.StringEntry>>() {}.type
        val recordDatastoreTypeToken =  AsyncStoredKeyValuePairings.StoreKeyValueType.newInstance<String, Record>().type
        val partitionDatastoreMetadataJsonFileManager = JsonFileManager<String, AsyncStoredKeyValuePairings.StringEntry>("$tempStorageDirOfStore$storeName-Metadata.json", Gson(), partitionDatastoreMetadataTypeToken)
        val orderedListOfAllPartitionIds: List<String>
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
            ioDatastoreWriteJobs += launch(Dispatchers.IO) {
                asyncStoredKeyValuePairings.createPairing(
                    expectedRecordWritten4.name,
                    expectedRecordWritten4
                )
            }
            ioDatastoreWriteJobs += launch(Dispatchers.IO) {
                asyncStoredKeyValuePairings.createPairing(
                    expectedRecordWritten5.name,
                    expectedRecordWritten5
                )
            }
            ioDatastoreWriteJobs += launch(Dispatchers.IO) {
                asyncStoredKeyValuePairings.createPairing(
                    expectedRecordWritten6.name,
                    expectedRecordWritten6
                )
            }
            ioDatastoreWriteJobs += launch(Dispatchers.IO) {
                asyncStoredKeyValuePairings.createPairing(
                    expectedRecordWritten7.name,
                    expectedRecordWritten7
                )
            }
            ioDatastoreWriteJobs += launch(Dispatchers.IO) {
                asyncStoredKeyValuePairings.createPairing(
                    expectedRecordWritten8.name,
                    expectedRecordWritten8
                )
            }
            ioDatastoreWriteJobs += launch(Dispatchers.IO) {
                asyncStoredKeyValuePairings.createPairing(
                    expectedRecordWritten9.name,
                    expectedRecordWritten9
                )
            }
            ioDatastoreWriteJobs += launch(Dispatchers.IO) {
                asyncStoredKeyValuePairings.createPairing(
                    expectedRecordWritten10.name,
                    expectedRecordWritten10
                )
            }

            twoWayIteratorEmittedAllRecordsWrittenInPartition1 = allPairingsValues.current.testCollectedBy({ currentValue ->
                if (currentValue != null) {
                    val value = currentValue as List<Record>
                    val result = value.exists { it.name == expectedRecordWritten10.name }
                              && value.exists { it.name == expectedRecordWritten9.name }
                              && value.exists { it.name == expectedRecordWritten8.name }
                              && value.exists { it.name == expectedRecordWritten7.name }
                    return@testCollectedBy result
                } else {
                    return@testCollectedBy false
                }
            })
            allPairingsValues.next()
            twoWayIteratorEmittedAllRecordsWrittenInPartition2 = allPairingsValues.current.testCollectedBy({ currentValue ->
                if (currentValue != null) {
                    val value = currentValue as List<Record>
                    val result = value.exists { it.name == expectedRecordWritten6.name }
                            && value.exists { it.name == expectedRecordWritten5.name }
                    return@testCollectedBy result
                } else {
                    return@testCollectedBy false
                }
            })
            allPairingsValues.next()
            twoWayIteratorEmittedAllRecordsWrittenInPartition3 = allPairingsValues.current.testCollectedBy({ currentValue ->
                if (currentValue != null) {
                    val value = currentValue as List<Record>
                    val result = value.exists { it.name == expectedRecordWritten4.name }
                            && value.exists { it.name == expectedRecordWritten3.name }
                    return@testCollectedBy result
                } else {
                    return@testCollectedBy false
                }
            })
            allPairingsValues.next()
            twoWayIteratorEmittedAllRecordsWrittenInPartition4 = allPairingsValues.current.testCollectedBy({ currentValue ->
                if (currentValue != null) {
                    val value = currentValue as List<Record>
                    val result = value.exists { it.name == expectedRecordWritten1.name }
                                 && value.exists { it.name == expectedRecordWritten2.name && it.tasksOnRecord!!.exists { (it.name == expectedTaskWritten1.name && it.currentStatus == expectedTaskWritten1.currentStatus) }
                    }
                    return@testCollectedBy result
                } else {
                    return@testCollectedBy false
                }
            })

            ioDatastoreWriteJobs.forEach {
                it.cancel()
            }
        }

        orderedListOfAllPartitionIds = partitionDatastoreMetadataJsonFileManager.read()["PARTITION_ORDERING"]!!.value.split(",").filter { it.isNotEmpty() }.map { it.trim() }

        val partitionOneRecordJsonFileManager = JsonFileManager<String, DatastoreEntryHolder<Record>>("$tempStorageDirOfStore${orderedListOfAllPartitionIds[0]}.json", Gson(), recordDatastoreTypeToken)
        val partitionTwoRecordJsonFileManager = JsonFileManager<String, DatastoreEntryHolder<Record>>("$tempStorageDirOfStore${orderedListOfAllPartitionIds[1]}.json", Gson(), recordDatastoreTypeToken)
        val partitionThreeRecordJsonFileManager = JsonFileManager<String, DatastoreEntryHolder<Record>>("$tempStorageDirOfStore${orderedListOfAllPartitionIds[2]}.json", Gson(), recordDatastoreTypeToken)
        val partitionFourRecordJsonFileManager = JsonFileManager<String, DatastoreEntryHolder<Record>>("$tempStorageDirOfStore${orderedListOfAllPartitionIds[3]}.json", Gson(), recordDatastoreTypeToken)

        mapOfAllPersistedRecords = partitionOneRecordJsonFileManager.read().mapValues { it.value.entry }.toMutableMap().apply {
            putAll(partitionTwoRecordJsonFileManager.read().mapValues { it.value.entry })
            putAll(partitionThreeRecordJsonFileManager.read().mapValues { it.value.entry })
            putAll(partitionFourRecordJsonFileManager.read().mapValues { it.value.entry })
        }

        Assert.assertTrue(twoWayIteratorEmittedAllRecordsWrittenInPartition1)
        Assert.assertTrue(twoWayIteratorEmittedAllRecordsWrittenInPartition2)
        Assert.assertTrue(twoWayIteratorEmittedAllRecordsWrittenInPartition3)
        Assert.assertTrue(twoWayIteratorEmittedAllRecordsWrittenInPartition4)
        Assert.assertTrue(mapOfAllPersistedRecords.containsKey(expectedRecordWritten1.name))
        Assert.assertEquals(expectedRecordWritten1.name, mapOfAllPersistedRecords[expectedRecordWritten1.name]!!.name)
        Assert.assertTrue(mapOfAllPersistedRecords.containsKey(expectedRecordWritten2.name))
        Assert.assertEquals(expectedRecordWritten2.name, mapOfAllPersistedRecords[expectedRecordWritten2.name]!!.name)
        Assert.assertTrue(mapOfAllPersistedRecords[expectedRecordWritten2.name]!!.tasksOnRecord.exists { it.name == expectedTaskWritten1.name && it.currentStatus == expectedTaskWritten1.currentStatus })
        Assert.assertTrue(mapOfAllPersistedRecords.containsKey(expectedRecordWritten3.name))
        Assert.assertEquals(expectedRecordWritten3.name, mapOfAllPersistedRecords[expectedRecordWritten3.name]!!.name)
        Assert.assertEquals(expectedRecordWritten4.name, mapOfAllPersistedRecords[expectedRecordWritten4.name]!!.name)
        Assert.assertEquals(expectedRecordWritten5.name, mapOfAllPersistedRecords[expectedRecordWritten5.name]!!.name)
        Assert.assertEquals(expectedRecordWritten6.name, mapOfAllPersistedRecords[expectedRecordWritten6.name]!!.name)
        Assert.assertEquals(expectedRecordWritten7.name, mapOfAllPersistedRecords[expectedRecordWritten7.name]!!.name)
        Assert.assertEquals(expectedRecordWritten8.name, mapOfAllPersistedRecords[expectedRecordWritten8.name]!!.name)
        Assert.assertEquals(expectedRecordWritten9.name, mapOfAllPersistedRecords[expectedRecordWritten9.name]!!.name)
        Assert.assertEquals(expectedRecordWritten10.name, mapOfAllPersistedRecords[expectedRecordWritten10.name]!!.name)
    }

    @Test
    fun givenAsyncStoredKeyValuePairing_WhenMultipleAsyncWritesMade_ThenExpectEachWrittenValueIsInFactPersistedInOrder() {
        val expectedRecordWritten1 = Record("exp-record-1", emptyArray())
        val expectedTaskWritten1 = Task("exp-task-1", Task.TASK_STATUS_COMPLETE)
        val expectedRecordWritten2 = Record("exp-record-2", arrayOf(expectedTaskWritten1))
        val expectedRecordWritten3 = Record("exp-record-3", emptyArray())
        val expectedRecordWritten4 = Record("exp-record-4", emptyArray())
        val expectedRecordWritten5 = Record("exp-record-5", emptyArray())
        val expectedRecordWritten6 = Record("exp-record-6", emptyArray())
        val expectedRecordWritten7 = Record("exp-record-7", emptyArray())
        val expectedRecordWritten8 = Record("exp-record-8", emptyArray())
        val expectedRecordWritten9 = Record("exp-record-9", emptyArray())
        val expectedRecordWritten10 = Record("exp-record-10", emptyArray())
        val expectedRecordWrittenToMidPartition1 = Record("exp-record-11", emptyArray())
        val expectedRecordWrittenToMidPartition2 = Record("exp-record-12", emptyArray())
        val expectedRecordWrittenToMidPartition3 = Record("exp-record-13", emptyArray())

        val twoWayIteratorEmittedAllRecordsWrittenInPartition1: Boolean
        val twoWayIteratorEmittedAllRecordsWrittenInPartition2: Boolean
        val twoWayIteratorEmittedAllRecordsWrittenInPartition3: Boolean
        val twoWayIteratorEmittedAllRecordsWrittenInPartition4: Boolean
        val twoWayIteratorEmittedAllRecordsWrittenInPartition5: Boolean
        val partitionDatastoreMetadataTypeToken = object: TypeToken<MutableMap<String, AsyncStoredKeyValuePairings.StringEntry>>() {}.type
        val recordDatastoreTypeToken =  AsyncStoredKeyValuePairings.StoreKeyValueType.newInstance<String, Record>().type
        val partitionDatastoreMetadataJsonFileManager = JsonFileManager<String, AsyncStoredKeyValuePairings.StringEntry>("$tempStorageDirOfStore$storeName-Metadata.json", Gson(), partitionDatastoreMetadataTypeToken)
        val orderedListOfAllPartitionIds: List<String>
        var iteratorEntries1stSet: List<Record> = listOf()
        var iteratorEntries2ndSet: List<Record> = listOf()
        var iteratorEntries3rdSet: List<Record> = listOf()
        var iteratorEntries4thSet: List<Record> = listOf()
        var iteratorEntries5thSet: List<Record> = listOf()

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
            ioDatastoreWriteJobs += launch(Dispatchers.IO) {
                asyncStoredKeyValuePairings.createPairing(
                    expectedRecordWritten4.name,
                    expectedRecordWritten4
                )
            }
            ioDatastoreWriteJobs += launch(Dispatchers.IO) {
                asyncStoredKeyValuePairings.createPairing(
                    expectedRecordWritten5.name,
                    expectedRecordWritten5
                )
            }
            ioDatastoreWriteJobs += launch(Dispatchers.IO) {
                asyncStoredKeyValuePairings.createPairing(
                    expectedRecordWritten6.name,
                    expectedRecordWritten6
                )
            }
            ioDatastoreWriteJobs += launch(Dispatchers.IO) {
                asyncStoredKeyValuePairings.createPairing(
                    expectedRecordWritten7.name,
                    expectedRecordWritten7
                )
            }
            ioDatastoreWriteJobs += launch(Dispatchers.IO) {
                asyncStoredKeyValuePairings.createPairing(
                    expectedRecordWritten8.name,
                    expectedRecordWritten8
                )
            }
            ioDatastoreWriteJobs += launch(Dispatchers.IO) {
                asyncStoredKeyValuePairings.createPairing(
                    expectedRecordWritten9.name,
                    expectedRecordWritten9
                )
            }
            ioDatastoreWriteJobs += launch(Dispatchers.IO) {
                asyncStoredKeyValuePairings.createPairing(
                    expectedRecordWritten10.name,
                    expectedRecordWritten10
                )
            }

            twoWayIteratorEmittedAllRecordsWrittenInPartition1 = allPairingsValues.current.testCollectedBy({ currentValue ->
                if (currentValue != null) {
                    val value = currentValue as List<Record>
                    val result = value.exists { it.name == expectedRecordWritten10.name }
                            && value.exists { it.name == expectedRecordWritten9.name }
                            && value.exists { it.name == expectedRecordWritten8.name }
                            && value.exists { it.name == expectedRecordWritten7.name }

                    iteratorEntries1stSet = value
                    return@testCollectedBy result
                } else {
                    return@testCollectedBy false
                }
            })
            allPairingsValues.next()
            twoWayIteratorEmittedAllRecordsWrittenInPartition2 = allPairingsValues.current.testCollectedBy({ currentValue ->
                if (currentValue != null) {
                    val value = currentValue as List<Record>
                    val result = value.exists { it.name == expectedRecordWritten6.name }
                            && value.exists { it.name == expectedRecordWritten5.name }

                    iteratorEntries2ndSet = value
                    return@testCollectedBy result
                } else {
                    return@testCollectedBy false
                }
            })
            allPairingsValues.next()
            ioDatastoreWriteJobs += launch(Dispatchers.IO) {
                asyncStoredKeyValuePairings.createPairing(
                    allPairingsValues.currentIndex,
                    expectedRecordWrittenToMidPartition1.name,
                    expectedRecordWrittenToMidPartition1
                )
                asyncStoredKeyValuePairings.createPairing(
                    allPairingsValues.currentIndex,
                    expectedRecordWrittenToMidPartition2.name,
                    expectedRecordWrittenToMidPartition2
                )
                asyncStoredKeyValuePairings.createPairing(
                    allPairingsValues.currentIndex,
                    expectedRecordWrittenToMidPartition3.name,
                    expectedRecordWrittenToMidPartition3
                )
            }
            twoWayIteratorEmittedAllRecordsWrittenInPartition3 = allPairingsValues.current.testCollectedBy({ currentValue ->
                if (currentValue != null) {
                    val value = currentValue as List<Record>
                    val result = value.exists { it.name == expectedRecordWrittenToMidPartition1.name }
                            && value.exists { it.name == expectedRecordWrittenToMidPartition2.name }
                            && value.exists { it.name == expectedRecordWrittenToMidPartition3.name }


                    iteratorEntries3rdSet = value
                    return@testCollectedBy result
                } else {
                    return@testCollectedBy false
                }
            })
            allPairingsValues.next()
            twoWayIteratorEmittedAllRecordsWrittenInPartition4 = allPairingsValues.current.testCollectedBy({ currentValue ->
                if (currentValue != null) {
                    val value = currentValue as List<Record>
                    val result = value.exists { it.name == expectedRecordWritten4.name }
                            && value.exists { it.name == expectedRecordWritten3.name }

                    iteratorEntries4thSet = value
                    return@testCollectedBy result
                } else {
                    return@testCollectedBy false
                }
            })
            allPairingsValues.next()
            twoWayIteratorEmittedAllRecordsWrittenInPartition5 = allPairingsValues.current.testCollectedBy({ currentValue ->
                if (currentValue != null) {
                    val value = currentValue as List<Record>
                    val result = value.exists { it.name == expectedRecordWritten1.name }
                            && value.exists { it.name == expectedRecordWritten2.name && it.tasksOnRecord!!.exists { (it.name == expectedTaskWritten1.name && it.currentStatus == expectedTaskWritten1.currentStatus) }
                    }

                    iteratorEntries5thSet = value
                    return@testCollectedBy result
                } else {
                    return@testCollectedBy false
                }
            })


            ioDatastoreWriteJobs.forEach {
                it.cancel()
            }
        }

        orderedListOfAllPartitionIds = partitionDatastoreMetadataJsonFileManager.read()["PARTITION_ORDERING"]!!.value.split(",").filter { it.isNotEmpty() }.map { it.trim() }

        val partitionOneRecordJsonFileManager = JsonFileManager<String, DatastoreEntryHolder<Record>>("$tempStorageDirOfStore${orderedListOfAllPartitionIds[0]}.json", Gson(), recordDatastoreTypeToken)
        val partitionTwoRecordJsonFileManager = JsonFileManager<String, DatastoreEntryHolder<Record>>("$tempStorageDirOfStore${orderedListOfAllPartitionIds[1]}.json", Gson(), recordDatastoreTypeToken)
        val partitionThreeRecordJsonFileManager = JsonFileManager<String, DatastoreEntryHolder<Record>>("$tempStorageDirOfStore${orderedListOfAllPartitionIds[2]}.json", Gson(), recordDatastoreTypeToken)
        val partitionFourRecordJsonFileManager = JsonFileManager<String, DatastoreEntryHolder<Record>>("$tempStorageDirOfStore${orderedListOfAllPartitionIds[3]}.json", Gson(), recordDatastoreTypeToken)
        val partitionFiveRecordJsonFileManager = JsonFileManager<String, DatastoreEntryHolder<Record>>("$tempStorageDirOfStore${orderedListOfAllPartitionIds[4]}.json", Gson(), recordDatastoreTypeToken)

        Assert.assertTrue(twoWayIteratorEmittedAllRecordsWrittenInPartition1)
        Assert.assertTrue(twoWayIteratorEmittedAllRecordsWrittenInPartition2)
        Assert.assertTrue(twoWayIteratorEmittedAllRecordsWrittenInPartition3)
        Assert.assertTrue(twoWayIteratorEmittedAllRecordsWrittenInPartition4)
        Assert.assertTrue(twoWayIteratorEmittedAllRecordsWrittenInPartition5)

        // Verify ordering is behaving as expected.
        val partitionOneMapOfAllDatastoreEntryHolders = partitionOneRecordJsonFileManager.read()
        val partitionOneAllDatastoreEntryHolders = partitionOneMapOfAllDatastoreEntryHolders.map { it.value }.sortedByDescending { it.order }
        val partitionTwoMapOfAllDatastoreEntryHolders = partitionTwoRecordJsonFileManager.read()
        val partitionTwoAllDatastoreEntryHolders = partitionTwoMapOfAllDatastoreEntryHolders.map { it.value }.sortedByDescending { it.order }
        val partitionThreeMapOfAllDatastoreEntryHolders = partitionThreeRecordJsonFileManager.read()
        val partitionThreeAllDatastoreEntryHolders = partitionThreeMapOfAllDatastoreEntryHolders.map { it.value }.sortedByDescending { it.order }
        val partitionFourMapOfAllDatastoreEntryHolders = partitionFourRecordJsonFileManager.read()
        val partitionFourAllDatastoreEntryHolders = partitionFourMapOfAllDatastoreEntryHolders.map { it.value }.sortedByDescending { it.order }
        val partitionFiveMapOfAllDatastoreEntryHolders = partitionFiveRecordJsonFileManager.read()
        val partitionFiveAllDatastoreEntryHolders = partitionFiveMapOfAllDatastoreEntryHolders.map { it.value }.sortedByDescending { it.order }

        Assert.assertEquals(expectedRecordWritten10.name, iteratorEntries1stSet[0].name)
        Assert.assertEquals(expectedRecordWritten9.name, iteratorEntries1stSet[1].name)
        Assert.assertEquals(expectedRecordWritten8.name, iteratorEntries1stSet[2].name)
        Assert.assertEquals(expectedRecordWritten7.name, iteratorEntries1stSet[3].name)
        Assert.assertEquals(expectedRecordWritten6.name, iteratorEntries2ndSet[0].name)
        Assert.assertEquals(expectedRecordWritten5.name, iteratorEntries2ndSet[1].name)
        Assert.assertEquals(expectedRecordWrittenToMidPartition3.name, iteratorEntries3rdSet[0].name)
        Assert.assertEquals(expectedRecordWrittenToMidPartition2.name, iteratorEntries3rdSet[1].name)
        Assert.assertEquals(expectedRecordWrittenToMidPartition1.name, iteratorEntries3rdSet[2].name)
        Assert.assertEquals(expectedRecordWritten4.name, iteratorEntries4thSet[0].name)
        Assert.assertEquals(expectedRecordWritten3.name, iteratorEntries4thSet[1].name)
        Assert.assertEquals(expectedRecordWritten1.name, iteratorEntries5thSet[0].name)
        Assert.assertEquals(expectedRecordWritten2.name, iteratorEntries5thSet[1].name)
        Assert.assertEquals(iteratorEntries1stSet[0].name, partitionOneAllDatastoreEntryHolders[0].entry.name)
        Assert.assertEquals(iteratorEntries1stSet[1].name, partitionOneAllDatastoreEntryHolders[1].entry.name)
        Assert.assertEquals(iteratorEntries1stSet[2].name, partitionOneAllDatastoreEntryHolders[2].entry.name)
        Assert.assertEquals(iteratorEntries1stSet[3].name, partitionOneAllDatastoreEntryHolders[3].entry.name)
        Assert.assertEquals(iteratorEntries2ndSet[0].name, partitionTwoAllDatastoreEntryHolders[0].entry.name)
        Assert.assertEquals(iteratorEntries2ndSet[1].name, partitionTwoAllDatastoreEntryHolders[1].entry.name)
        Assert.assertEquals(iteratorEntries3rdSet[0].name, partitionThreeAllDatastoreEntryHolders[0].entry.name)
        Assert.assertEquals(iteratorEntries3rdSet[1].name, partitionThreeAllDatastoreEntryHolders[1].entry.name)
        Assert.assertEquals(iteratorEntries3rdSet[2].name, partitionThreeAllDatastoreEntryHolders[2].entry.name)
        Assert.assertEquals(iteratorEntries4thSet[0].name, partitionFourAllDatastoreEntryHolders[0].entry.name)
        Assert.assertEquals(iteratorEntries4thSet[1].name, partitionFourAllDatastoreEntryHolders[1].entry.name)
        Assert.assertEquals(iteratorEntries5thSet[0].name, partitionFiveAllDatastoreEntryHolders[0].entry.name)
        Assert.assertEquals(iteratorEntries5thSet[1].name, partitionFiveAllDatastoreEntryHolders[1].entry.name)
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

            delay(8000L) // TODO address slow flaky test case.

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
    fun givenHundredEntriesAttemptedCreation_WhenMaxEntries16_ThenExpect16Entries() {

        runBlocking {
            for (i in 0 .. 100) {
                asyncStoredKeyValuePairings.createPairing("TestRecord$i", Record("TestRecord$i", null))
            }
            delay(4000L)
        }

        Assert.assertEquals(16, asyncStoredKeyValuePairings.currentNumberOfEntries)
    }

    @Test
    fun givenFiveEntries_WhenReopenDatastore_ThenExpectSuccessfullyReloaded() {
        val expectedRecordWritten1 = Record("exp-record-1", emptyArray())
        val expectedRecordWritten2 = Record("exp-record-2", emptyArray())
        val expectedRecordWritten3 = Record("exp-record-3", emptyArray())
        val expectedRecordWritten4 = Record("exp-record-4", emptyArray())
        val expectedRecordWritten5 = Record("exp-record-5", emptyArray())
        val allRecordsReadPreReopen = mutableListOf<Boolean>()
        val allRecordsReadPostReopen = mutableListOf<Boolean>()
        var preReopenIterator: TwoWayIterator<List<Record>>
        var postReopenIterator: TwoWayIterator<List<Record>>


        runBlocking {
            asyncStoredKeyValuePairings.createPairing(
                expectedRecordWritten1.name,
                expectedRecordWritten1
            )
            asyncStoredKeyValuePairings.createPairing(
                expectedRecordWritten2.name,
                expectedRecordWritten2
            )
            asyncStoredKeyValuePairings.createPairing(
                expectedRecordWritten3.name,
                expectedRecordWritten3
            )
            asyncStoredKeyValuePairings.createPairing(
                expectedRecordWritten4.name,
                expectedRecordWritten4
            )
            asyncStoredKeyValuePairings.createPairing(
                expectedRecordWritten5.name,
                expectedRecordWritten5
            )

            preReopenIterator = asyncStoredKeyValuePairings.retrieveAllPairingsValues()

            allRecordsReadPreReopen.add(preReopenIterator.current.testCollectedBy({ currentValue ->
                if (currentValue != null) {
                    val value = currentValue as List<Record>
                    val result = value.exists { it.name == expectedRecordWritten3.name }
                            && value.exists { it.name == expectedRecordWritten4.name }
                            && value.exists { it.name == expectedRecordWritten5.name }

                    return@testCollectedBy result
                } else {
                    return@testCollectedBy false
                }
            }))
            preReopenIterator.next()
            allRecordsReadPreReopen.add(preReopenIterator.current.testCollectedBy({ currentValue ->
                if (currentValue != null) {
                    val value = currentValue as List<Record>
                    val result = value.exists { it.name == expectedRecordWritten1.name }
                            && value.exists { it.name == expectedRecordWritten2.name }

                    return@testCollectedBy result
                } else {
                    return@testCollectedBy false
                }
            }))
        }

        asyncStoredKeyValuePairings.close()
        val reopenedDatastore = AsyncStoredKeyValuePairings.newInstance<String, Record>(storeName, tempStorageDirOfStore, 4, 16)

        runBlocking {
            postReopenIterator = reopenedDatastore.retrieveAllPairingsValues()

            allRecordsReadPostReopen.add(postReopenIterator.current.testCollectedBy({ currentValue ->
                if (currentValue != null) {
                    val value = currentValue as List<Record>
                    val result = value.exists { it.name == expectedRecordWritten3.name }
                            && value.exists { it.name == expectedRecordWritten4.name }
                            && value.exists { it.name == expectedRecordWritten5.name }

                    return@testCollectedBy result
                } else {
                    return@testCollectedBy false
                }
            }))
            postReopenIterator.next()
            allRecordsReadPostReopen.add(postReopenIterator.current.testCollectedBy({ currentValue ->
                if (currentValue != null) {
                    val value = currentValue as List<Record>
                    val result = value.exists { it.name == expectedRecordWritten1.name }
                            && value.exists { it.name == expectedRecordWritten2.name }
                    return@testCollectedBy result
                } else {
                    return@testCollectedBy false
                }
            }))
        }

        Assert.assertTrue(allRecordsReadPreReopen.all { it })
        Assert.assertTrue(allRecordsReadPostReopen.all { it })
    }

    @Test
    fun givenTenEntries_WhenEntriesPersistedThenDatastoreCleared_ThenExpectZeroEntriesLeft() {
        val expectedRecordWritten1 = Record("exp-record-1", emptyArray())
        val expectedRecordWritten2 = Record("exp-record-2", emptyArray())
        val expectedRecordWritten3 = Record("exp-record-3", emptyArray())
        val expectedRecordWritten4 = Record("exp-record-4", emptyArray())
        val expectedRecordWritten5 = Record("exp-record-5", emptyArray())
        val expectedRecordWritten6 = Record("exp-record-6", emptyArray())
        val expectedRecordWritten7 = Record("exp-record-7", emptyArray())
        val expectedRecordWritten8 = Record("exp-record-8", emptyArray())
        val expectedRecordWritten9 = Record("exp-record-9", emptyArray())
        val expectedRecordWritten10 = Record("exp-record-10", emptyArray())

        runBlocking {
            asyncStoredKeyValuePairings.apply {
                createPairing(expectedRecordWritten1.name, expectedRecordWritten1)
                createPairing(expectedRecordWritten2.name, expectedRecordWritten2)
                createPairing(expectedRecordWritten3.name, expectedRecordWritten3)
                createPairing(expectedRecordWritten4.name, expectedRecordWritten4)
                createPairing(expectedRecordWritten5.name, expectedRecordWritten5)
                createPairing(expectedRecordWritten6.name, expectedRecordWritten6)
                createPairing(expectedRecordWritten7.name, expectedRecordWritten7)
                createPairing(expectedRecordWritten8.name, expectedRecordWritten8)
                createPairing(expectedRecordWritten9.name, expectedRecordWritten9)
                createPairing(expectedRecordWritten10.name, expectedRecordWritten10)
            }
        }

        runBlocking {
            asyncStoredKeyValuePairings.apply {
                deletePairing(expectedRecordWritten1.name).timeout(200.milliseconds).catch {}.firstOrNull()
                deletePairing(expectedRecordWritten2.name).timeout(200.milliseconds).catch {}.firstOrNull()
                deletePairing(expectedRecordWritten3.name).timeout(200.milliseconds).catch {}.firstOrNull()
                deletePairing(expectedRecordWritten4.name).timeout(200.milliseconds).catch {}.firstOrNull()
                deletePairing(expectedRecordWritten5.name).timeout(200.milliseconds).catch {}.firstOrNull()
                deletePairing(expectedRecordWritten6.name).timeout(200.milliseconds).catch {}.firstOrNull()
                deletePairing(expectedRecordWritten7.name).timeout(200.milliseconds).catch {}.firstOrNull()
                deletePairing(expectedRecordWritten8.name).timeout(200.milliseconds).catch {}.firstOrNull()
                deletePairing(expectedRecordWritten9.name).timeout(200.milliseconds).catch {}.firstOrNull()
                deletePairing(expectedRecordWritten10.name).timeout(200.milliseconds).catch {}.firstOrNull()
            }
        }

        Assert.assertEquals("Expected all entries to be deleted.", 0, asyncStoredKeyValuePairings.currentNumberOfEntries)
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