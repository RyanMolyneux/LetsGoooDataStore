package com.ryanmolyneux.letsgooodatastore.experimental.pairings

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ryanmolyneux.letsgooodatastore.datastores.JsonFileManager
import com.ryanmolyneux.letsgooodatastore.datastores.datastoreentries.AbsDatastoreEntry
import com.ryanmolyneux.letsgooodatastore.datastores.internal.datastoreentries.DatastoreEntryHolder
import com.ryanmolyneux.letsgooodatastore.experimental.pairings.IAsyncMutableKeyValuePairings.MutabilityState.CLOSED
import com.ryanmolyneux.letsgooodatastore.experimental.pairings.IAsyncMutableKeyValuePairings.MutabilityState.OPEN
import com.ryanmolyneux.letsgooodatastore.pairings.StoredKeyValuePairings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import java.io.File
import java.lang.ref.WeakReference
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.typeOf

/** TODO
 *  1. Explore possibility of need to refactor JsonFileWriter to make it suspend during merge calls
 *  to prevent merge from failing when being used concurrently by 2 or more coroutines.
 *  2. Move max fifo queue entries up to policy so that all implementations are aware.
 */

/**
 * A type of mutable key value pairings instance which is intended to be used to persist the pairings
 * past the lifetime of the object instance to the disk at the given datastore directory with the
 * datastore name being used for the partition file name whom will keep track of all the
 * data stores persisted data.
 */
abstract class AbsAsyncStoredKeyValuePairings<Key, Value: AbsDatastoreEntry>: IAsyncMutableKeyValuePairings<Key, Value> {
    /**
     * field holding name of datastore.
     */
    abstract val nameOfDatastore: String

    /**
     * field holding path to/directory datastore is inside.
     */
    abstract val directoryOfDatastore: String
}

/**
 * Collection of mutable key value pairings, where each key inside the collection uniquely
 * identifies a given pairing, and where all read and write operations are performed asynchronously
 * upon a background thread, with all write operations being thread safe.
 */
interface IAsyncMutableKeyValuePairings<Key, Value: AbsDatastoreEntry>: IAsyncKeyValuePairings<Key, Value> {
    /**
     * field holding the current number of key->value entries that the given mutable key value pairings holds.
     */
    val currentNumberOfEntries: Long

    /**
     * field holding max number of key->value entries that can be held by the given mutable key value pairings.
     */
    val maxEntries: Long

    /**
     * field holding current mutability state of key value pairings instance.
     */
    val mutabilityState: StateFlow<MutabilityState>

    /**
     * To be used to create a key uniquely identifiable key to value pairings in the pair collection
     * updating an existing pairs value if one already exists, the prior operations ability to occur
     * will vary based on what mutability state the key value pairings is currently in and whether
     * or not max entries has been reached, any non mutable state resulting in calls during that
     * time expected to result in no-op, suspending to account for scenarios where long running
     * blocking operations can occur during writes ops.
     */
    suspend fun createPairing(key: Key, value: Value): Unit

    /**
     * Behaves the same as [createPairing] only that the entry will target to be stored in a
     * particular index of the data stores values collection, key point of note here will be
     * that existing key updates will always take priority over index, meaning that a index
     * will not allow a key to become duplicated in the collection & when using [TwoWayIterator]
     * you should ensure that the value being written is not overwriting one which you do
     * not expect it to, this can be done by first ensuring the pairing is non existent first.
     */
    suspend fun createPairing(index: Int, key: Key, value: Value)

    /**
     * To be used to remove a key uniquely identified key to value pairing in the pairing collection
     * if it already exists, if not it will return null, the prior operations ability to occur
     * will vary based on what mutability state the key value pairings is currently in, any non
     * mutable state resulting in calls during that time expected to result in no-op which in this
     * case will mean the flow returned will always result in null value, suspending
     * to account for scenarios where long running blocking operations can occur during writes ops.
     */
    suspend fun deletePairing(key: Key): Flow<Value?>

    /**
     * To called whenever an instance of a given key value pairings is no longer required to be mutable
     * to move it to the closed state so that no new data can no longer be written to
     * it, any write method calls after a given instance is in closed state will result in
     * no-op/nothing happening, while those that occurred prior or just before are expected to finish
     * there write operations.
     */
    fun close();

    /**
     * States that stored key value pairings instance will move between during its lifecycle.
     *
     * - [OPEN], Open to mutating method calls.
     * - [CLOSED], Closed to any new mutation method calls.
     */
    enum class MutabilityState {
        OPEN,
        CLOSED
    }
}

/**
 * Collection of immutable key value pairings, where each key inside the collection uniquely
 * identifies a given pairing, and where all read operations are performed asynchronously
 * upon a background thread.
 */
interface IAsyncKeyValuePairings<Key, Value: AbsDatastoreEntry> {

    /**
     * To be used to retrieve a key uniquely identified value in the pairing collection
     * emitted by the returned flow if and or once it exists.
     */
    fun retrievePairingsValue(key: Key): Flow<Value>

    /**
     * To be used to retrieve all datastore entries in a paged manner, where 255 entries will be
     * made available per iteration.
     */
    fun retrieveAllPairingsValues(): TwoWayIterator<List<Value>>
}

/**
 * Type of iterator which can be used to navigate both forwards and backwards through a given
 * collection of values in a async manner.
 */
interface TwoWayIterator<Value> {
    /**
     * SharedFlow which is emitting the current value of the two way iterator
     * has traversed to from the collection, if this value is updated
     * during flows collection the latest value will be re-emitted,
     * note this is a shared flow of capacity size 1.
     */
    val current: SharedFlow<Value>


    /**
     * Current index the iterator has traversed two over the collection
     * of values.
     */
    val currentIndex: Int

    /**
     * To be used to check whether there is a previous value in the collection
     * to iterate backwards to.
     */
    fun hasPrevious(): Boolean;

    /**
     * To be used to traverse backwards to the previous value in the collection
     * if one exists it will emit through the current shared flow field
     * else no-op is expected.
     */
    suspend fun previous();

    /**
     * To be used to check whether there is a next value in the collection to
     * iterate forward to.
     */
    fun hasNext(): Boolean;

    /**
     * To be used to traverse forwards to the next value in the collection if one
     * exists it will be emit through the current shared flow else no-op will occur.
     */
    suspend fun next();
}

/**
 * Experimental revision of stored key value pairings with this new revision instead performing its
 * read/write operations asynchronously on background threads.
 */
class AsyncStoredKeyValuePairings<Key, Value: AbsDatastoreEntry>: AbsAsyncStoredKeyValuePairings<Key, Value> {
    private val setupComplete = MutableStateFlow<Boolean>(false)
    private val READ_OP_NUM_OF_THREADS_IN_POOL = 4
    private val WRITE_OP_NUM_OF_THREADS_IN_POOL = 1 // TODO update with larger capacity once JsonFileWriter.merge is made thread safe.
    private val WRITE_OP_PARTITION_RESIZE_RATIO = 0.20
    private val writeOpQueueMaxCapacity: Int
    private var _currentNumberOfEntries = AtomicLong()
    override val currentNumberOfEntries: Long
        get() = _currentNumberOfEntries.get()
    private val _maxEntries: Long
    private val maxPartitionEntries: Long
    private val resizePartitionEntries
        get () = if ((maxPartitionEntries * WRITE_OP_PARTITION_RESIZE_RATIO).toInt() >= 2) (maxPartitionEntries * WRITE_OP_PARTITION_RESIZE_RATIO).toInt() else 2
    override val maxEntries: Long
        get() = _maxEntries
    override val mutabilityState: StateFlow<IAsyncMutableKeyValuePairings.MutabilityState>
        get() = _state
    private val _state = MutableStateFlow(IAsyncMutableKeyValuePairings.MutabilityState.OPEN)
    override val nameOfDatastore: String
    override val directoryOfDatastore: String
    private val valueType: Type
    private val fifoPartitionWriteOpQueue: BlockingQueue<WriteOp<Key, Value>>
    private val partitionChangedFlow = MutableSharedFlow<Pair<Partition, StoredKeyValuePairings<Key, DatastoreEntryHolder<Value>>>>(
        replay = 25,
        extraBufferCapacity = 25,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val partitionWriteOpResultFlow = MutableSharedFlow<WriteOpResult<Key, Value>>(
        replay = 25,
        extraBufferCapacity = 25,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val storeBackgroundReadOpDispatcher = newFixedThreadPoolContext(READ_OP_NUM_OF_THREADS_IN_POOL, "stored-kv-read-pool-IO")
    private val storeBackgroundReadOpScope = CoroutineScope(storeBackgroundReadOpDispatcher)
    private val storeBackgroundWriteOpDispatcher = newFixedThreadPoolContext(WRITE_OP_NUM_OF_THREADS_IN_POOL, "stored-kv-write-pool-IO")
    private val storeBackgroundWriteOpScope = CoroutineScope(storeBackgroundWriteOpDispatcher)
    private val partitionsDatastore: StoredKeyValuePairings<String, Partition>
    private val partitionsMetadataDatastore: StoredKeyValuePairings<String, StringEntry>
    private lateinit var writePartition: Pair<Partition, StoredKeyValuePairings<Key, DatastoreEntryHolder<Value>>>
    private val partitionsRefs by lazy {
        ArrayDeque<Pair<Partition, StoredKeyValuePairingMutableWeakReference>>()
    }

    constructor(nameOfDatastore: String,
                directoryOfDatastore: String,
                storeKeyValueType: IStoreKeyValueType,
                maxPartitionEntries: Long = DEFAULT_PARTITION_MAX_ENTRIES,
                maxEntries: Long = DEFAULT_MAX_ENTRIES,
                writeOpQueueMaxCapacity: Int = DEFAULT_WRITE_OP_QUEUE_MAX_CAPACITY) {
        this.valueType = storeKeyValueType.type
        this.nameOfDatastore = nameOfDatastore
        this.directoryOfDatastore = directoryOfDatastore
        this.maxPartitionEntries = maxPartitionEntries
        this._maxEntries = maxEntries
        this.writeOpQueueMaxCapacity = writeOpQueueMaxCapacity
        this.fifoPartitionWriteOpQueue = ArrayBlockingQueue(writeOpQueueMaxCapacity)

        assert(maxPartitionEntries >= 4) { "Must have at least 4 or more entries for efficient operations." }

        val partitionType = object: TypeToken<Map<String, Partition>>() {}.type
        val partitionMetadataType = object: TypeToken<Map<String, StringEntry>>() {}.type

        partitionsDatastore = StoredKeyValuePairings(mutableMapOf(), JsonFileManager((toStoragePath(nameOfDatastore)), Gson(), partitionType))
        partitionsMetadataDatastore = StoredKeyValuePairings(mutableMapOf(), JsonFileManager((toStoragePath("$nameOfDatastore-Metadata")), Gson(), partitionMetadataType))

        setup()
    }

    private fun setup() {
        storeBackgroundWriteOpScope.launch {
            val partitions = partitionsDatastore.retrieveAllPairingsValues()

            if (partitions.isEmpty()) {
                var firstPartition = Partition()
                while (File(toStoragePath(firstPartition.id)).exists()) {
                    firstPartition = Partition()
                }
                writePartition = firstPartition to StoredKeyValuePairings(mutableMapOf(), JsonFileManager(toStoragePath(firstPartition.id), Gson(), valueType))
                partitionsDatastore.createPairing(firstPartition.id, firstPartition)
                partitionsMetadataDatastore.createPairing(METADATA_KEY_PARTITION_ORDERING, firstPartition.id.stringEntry)
                partitionsRefs.add(writePartition.first to StoredKeyValuePairingMutableWeakReference(writePartition.second))
            } else {
                partitionsRefs.addAll(partitionsMetadataDatastore.retrievePairingsValue(METADATA_KEY_PARTITION_ORDERING)!!.value.split(",").filter { it.isNotEmpty() }.map { Partition(it.trim()) to StoredKeyValuePairingMutableWeakReference(null) })
                val partitionRefsMap = partitionsRefs.associate { it.first.id to it.second }
                for(partition in partitions) {
                    val storedKeyValuePairings = StoredKeyValuePairings<Key, DatastoreEntryHolder<Value>>(
                        mutableMapOf(),
                        JsonFileManager(toStoragePath(partition.id), Gson(), valueType)
                    )

                    _currentNumberOfEntries.getAndAdd(
                        storedKeyValuePairings.retrieveAllPairingsValues().count().toLong()
                    )
                    partitionRefsMap[partition.id]!!.update(storedKeyValuePairings)
                    partitionChangedFlow.emit(partition to storedKeyValuePairings)
                }

                writePartition = partitionsRefs.first().first to partitionsRefs.first().second.get()!!
            }
            partitionChangedFlow.emit(writePartition)

            suspend fun untilCancelledDoUsPartWriteOps() {
                while (true) {
                    fifoPartitionWriteOpQueue.take()?.let {
                        // We always wanna point to the first partition as write partition only changing when necessary based on key & indexes specified
                        if (writePartition.first.id != partitionsRefs.first().first.id) {
                            val firstPartition = partitionsRefs.first().first
                            val firstPartitionStore = partitionsRefs.first().second.get()
                            writePartition = if (firstPartitionStore != null) {
                                firstPartition to firstPartitionStore
                            } else {
                                firstPartition to StoredKeyValuePairings(mutableMapOf(), JsonFileManager(toStoragePath(firstPartition.id), Gson(), valueType))
                            }
                        }
                        val writeOpResult: WriteOpResult<Key, Value>
                        var partitionToWriteTo = findPartition(it.key)

                        if (it is WriteOp.UpdateOp) {
                            writePartition = if (partitionToWriteTo == null && it.index?.let { findPartition(it) } != null) it.index?.let { findPartition(it) }!! else writePartition // TODO: cleanup, for now this is necessary when index is being used to ensure the new write partition is resized when necessary.
                            partitionToWriteTo = partitionToWriteTo?: it.index?.let { findPartition(it) }  ?: writePartition
                            val writeInvoked: Boolean
                            val retrievedEntry = partitionToWriteTo.second.retrievePairingsValue(it.key)

                            writeInvoked = if (retrievedEntry == null && _currentNumberOfEntries.get() < maxEntries) {
                                _currentNumberOfEntries.incrementAndGet()
                                partitionToWriteTo.second.createPairing(it.key, DatastoreEntryHolder(it.value!!))
                                true
                            } else if (retrievedEntry != null) {
                                partitionToWriteTo.second.updatePairingsValue(it.key, DatastoreEntryHolder(it.value!!, retrievedEntry.order))
                                true
                            } else {
                                false
                            }

                            writeOpResult = if (writeInvoked)  {
                                WriteOpResult.Completed(it, it.value!!, partitionToWriteTo.first, partitionToWriteTo.second)
                            } else {
                                WriteOpResult.NoOp(it)
                            }
                        } else {
                            if (partitionToWriteTo == null) {
                                writeOpResult = WriteOpResult.NoOp(it)
                            } else {
                                val currentValue = partitionToWriteTo.second.retrievePairingsValue(it.key)
                                partitionToWriteTo.second.deletePairing(it.key)

                                if (partitionsRefs.count() > 1 && partitionToWriteTo.second.retrieveAllPairingsValues().isEmpty()) {
                                    partitionsDatastore.deletePairing(partitionToWriteTo.first.id)
                                    partitionsRefs.removeIf { it.first.id == partitionToWriteTo.first.id }
                                    partitionsMetadataDatastore.updatePairingsValue(
                                        METADATA_KEY_PARTITION_ORDERING,
                                        partitionsRefs.joinToString(",") { it.first.id }.stringEntry
                                    )
                                    partitionToWriteTo.second.deleteStoreFiles()
                                    if (writePartition == partitionToWriteTo) {
                                        val replacementWritePartition = partitionsRefs.first()
                                        if (replacementWritePartition.second.get() == null) {
                                            val partitionRefToStoredKeyValueParing = StoredKeyValuePairings<Key, DatastoreEntryHolder<Value>>(mutableMapOf(), JsonFileManager(toStoragePath(replacementWritePartition.first.id), Gson(), valueType))
                                            replacementWritePartition.second.update(partitionRefToStoredKeyValueParing)
                                        }
                                        writePartition = Partition(replacementWritePartition.first.id) to replacementWritePartition.second.get()!!
                                    }
                                }

                                _currentNumberOfEntries.decrementAndGet()
                                writeOpResult = WriteOpResult.Completed(it,  currentValue!!.entry, partitionToWriteTo.first, partitionToWriteTo.second)
                            }
                        }

                        if (writePartition.second.retrieveAllPairingsValues().count() > maxPartitionEntries) {
                            var nextVacantResizeRatioPartition: Pair<Partition?, StoredKeyValuePairings<Key, DatastoreEntryHolder<Value>>?> = null to null

                            partitionsRefs.getOrNull(partitionsRefs.indexOfFirst { it.first.id == writePartition.first.id } + 1)?.let { ref ->
                                if (ref.second.get() == null) {
                                    val partitionRefToStoredKeyValueParing = StoredKeyValuePairings<Key, DatastoreEntryHolder<Value>>(mutableMapOf(), JsonFileManager(toStoragePath(ref.first.id), Gson(), valueType))
                                    ref.second.update(partitionRefToStoredKeyValueParing)
                                }
                                ref.second.get()?.let { map ->
                                    if (nextVacantResizeRatioPartition.first == null && map.retrieveAllPairingsValues().count() < (maxPartitionEntries - resizePartitionEntries)) {
                                        nextVacantResizeRatioPartition = ref.first to map
                                    }
                                }
                            }

                            if (nextVacantResizeRatioPartition.first == null) {
                                var newPartition = Partition()
                                while (partitionsRefs.find { it.first.id == newPartition.id } != null && File(toStoragePath(newPartition.id)).exists()) {
                                    newPartition = Partition()
                                }
                                val storedKeyValuePairings = StoredKeyValuePairings<Key, DatastoreEntryHolder<Value>>(mutableMapOf(), JsonFileManager(toStoragePath(newPartition.id), Gson(), valueType))
                                partitionsDatastore.createPairing(newPartition.id, newPartition)
                                partitionsRefs.add(partitionsRefs.indexOfFirst { it.first.id == writePartition.first.id } + 1, newPartition to StoredKeyValuePairingMutableWeakReference(storedKeyValuePairings))
                                partitionsMetadataDatastore.updatePairingsValue(METADATA_KEY_PARTITION_ORDERING, partitionsRefs.map { it.first.id }.joinToString().stringEntry)
                                nextVacantResizeRatioPartition = newPartition to storedKeyValuePairings
                            }

                            if (nextVacantResizeRatioPartition.first != null) {
                                val writePartitionKeysList = writePartition.second.getWrappedMap().map { it.key to it.value.order }.sortedByDescending { it.second }
                                writePartitionKeysList.subList(writePartitionKeysList.size - resizePartitionEntries, writePartitionKeysList.size)
                                .sortedBy { it.second } // Resort before writing because we want batches to not cause re-arrangements in there ordering priority.
                                .let {
                                    it.forEach { keyOrderPair ->
                                        val (key, _) = keyOrderPair
                                        writePartition.second.retrievePairingsValue(key)?.let { value ->
                                            /**
                                             * Ordering is newest first to oldest, so if we shift it over to a new partition we want to ensure
                                             * its ordering is the highest value in that partition.
                                             */
                                            nextVacantResizeRatioPartition.second?.createPairing(key, DatastoreEntryHolder(value.entry))
                                        }
                                        writePartition.second.deletePairing(key)
                                    }
                                }
                                partitionChangedFlow.emit(nextVacantResizeRatioPartition.first!! to nextVacantResizeRatioPartition.second!!)
                            }
                        }

                        partitionToWriteTo?.let {
                            partitionChangedFlow.emit(it)
                        }
                        partitionWriteOpResultFlow.emit(writeOpResult)
                    }
                    delay(50L)
                }
            }

            setupComplete.value = true
            untilCancelledDoUsPartWriteOps()
        }
    }

    override suspend fun createPairing(key: Key, value: Value) {
        if (isOpen()) {
            fifoPartitionWriteOpQueue.put(WriteOp.UpdateOp(key = key, value = value))
        }
    }

    override suspend fun createPairing(index: Int, key: Key, value: Value) {
        if (isOpen()) {
            fifoPartitionWriteOpQueue.put(WriteOp.UpdateOp(index, key, value))
        }
    }

    override suspend fun deletePairing(key: Key): Flow<Value?> {
        return if (isOpen()) {
            fifoPartitionWriteOpQueue.put(WriteOp.DeleteOp(key))

            partitionWriteOpResultFlow.filter {
                (it.op.key == key && it.op is WriteOp.DeleteOp)
            }.map {
                if (it is WriteOpResult.Completed) {
                    it.latestValue
                } else {
                    null
                }
            }.flowOn(storeBackgroundReadOpDispatcher)
        } else {
            flow { emit(null) }
        }
    }

    override fun retrievePairingsValue(key: Key) =
        combine(
            flow {
                     while (!setupComplete.value) { delay(50) } // TODO: create confirmation test for this retrieval to quickly from non fully setup async store k-v pairing & usage of writePartition.
                     emit(findPartitionWeak(key))
                 },
            partitionChangedFlow
        ) { initialPartition, lastPartitionRefreshed ->
            val initialPartitionValueRetrieved: Value? =
                initialPartition?.second?.get()?.retrievePairingsValue(key)?.entry
            val latestPartitionRefreshedValueRetrieved: Value? =
                lastPartitionRefreshed.second.retrievePairingsValue(key)?.entry
            var valueToReturn: Value? = null

            if (latestPartitionRefreshedValueRetrieved != null) {
                valueToReturn = latestPartitionRefreshedValueRetrieved
            } else if (initialPartitionValueRetrieved != null) {
                valueToReturn = initialPartitionValueRetrieved
            }

            valueToReturn
        }.filterNotNull()
            .distinctUntilChanged()
            .flowOn(storeBackgroundReadOpDispatcher)

    override fun retrieveAllPairingsValues(): TwoWayIterator<List<Value>> {
        return StoredKeyValuePairingV2Iterator()
    }

    /**
     * Returns partition which holds the given key, pairing it with a given value and returns
     * null if none of the partitions hold a pairing to the given key.
     */
    private fun findPartition(key: Key): Pair<Partition, StoredKeyValuePairings<Key, DatastoreEntryHolder<Value>>>? {
        var partition: Partition? = null
        var storedKeyValuePairings: StoredKeyValuePairings<Key, DatastoreEntryHolder<Value>>? = null

        if (writePartition.second.retrievePairingsValue(key) != null) {
            partition = writePartition.first
            storedKeyValuePairings = writePartition.second
        } else {
            for (ref in partitionsRefs) {
                val currentPartition = ref.first
                var currentStoredKeyValuePairings = ref.second.get()

                if (currentStoredKeyValuePairings == null) {
                    currentStoredKeyValuePairings = StoredKeyValuePairings(mutableMapOf(), JsonFileManager(toStoragePath(ref.first.id), Gson(), valueType))
                    ref.second.update(currentStoredKeyValuePairings)
                }

                if (currentStoredKeyValuePairings.retrievePairingsValue(key) != null) {
                    partition = currentPartition
                    storedKeyValuePairings = currentStoredKeyValuePairings
                    break;
                }
            }
        }

        return if (partition != null && storedKeyValuePairings != null) {
            partition to storedKeyValuePairings
        } else {
            null
        }
    }

    private fun findPartition(index: Int): Pair<Partition, StoredKeyValuePairings<Key, DatastoreEntryHolder<Value>>>? {
        val ref = partitionsRefs.getOrNull(index)
        val currentPartition = ref?.first
        var currentStoredKeyValuePairings = ref?.second?.get()

        return if (currentPartition != null && currentStoredKeyValuePairings == null) {
            currentStoredKeyValuePairings = StoredKeyValuePairings(mutableMapOf(), JsonFileManager(toStoragePath(ref.first.id), Gson(), valueType))
            ref.second.update(currentStoredKeyValuePairings)
            currentPartition to currentStoredKeyValuePairings
        } else if (currentPartition != null && currentStoredKeyValuePairings != null) {
            currentPartition to currentStoredKeyValuePairings
        } else {
            null
        }
    }

    private fun findPartitionWeak(key: Key): Pair<Partition, StoredKeyValuePairingMutableWeakReference>? {
        val strongRef = findPartition(key)

        return strongRef?.run {
            first to StoredKeyValuePairingMutableWeakReference(second)
        }
    }

    override fun close() {
        _state.value = IAsyncMutableKeyValuePairings.MutabilityState.CLOSED
    }

    /**
     * Partition of store containing the part of all the entries the given store key value pairings
     * has.
     */
    internal data class Partition(val id: String = UUID.randomUUID().toString()): AbsDatastoreEntry()
    internal data class StringEntry(val value: String): AbsDatastoreEntry()

    private val String.stringEntry
        get() = StringEntry(this)

    /**
     * All types of write operations that can be placed fifo write queue to be performed on the underlying
     * datastores partitions
     */
    private sealed class WriteOp<Key, Value: AbsDatastoreEntry>(val key: Key, val value: Value? = null) {
        class DeleteOp<Key, Value: AbsDatastoreEntry>(key: Key): WriteOp<Key, Value>(key)
        class UpdateOp<Key, Value: AbsDatastoreEntry>(val index: Int? = null, key: Key, value: Value): WriteOp<Key, Value>(key, value)
    }

    /**
     * Data class designed to hold all the possible resulting outcomes a given write operation can
     * have.
     */
    private sealed class WriteOpResult<Key, Value: AbsDatastoreEntry>(val op: WriteOp<Key, Value>) {
        class Completed<Key, Value: AbsDatastoreEntry>(op: WriteOp<Key, Value>,
                                                       val latestValue: Value,
                                                       val partition: Partition,
                                                       val storedKeyValuePairings: StoredKeyValuePairings<Key, DatastoreEntryHolder<Value>>): WriteOpResult<Key, Value>(op)
        class NoOp<Key, Value: AbsDatastoreEntry>(op: WriteOp<Key, Value>): WriteOpResult<Key, Value>(op)
    }

    private inner class StoredKeyValuePairingMutableWeakReference {
        private var ref: WeakReference<StoredKeyValuePairings<Key, DatastoreEntryHolder<Value>>?>

        constructor(storedKeyValuePairing: StoredKeyValuePairings<Key, DatastoreEntryHolder<Value>>?) {
            ref = WeakReference(storedKeyValuePairing)
        }

        fun get(): StoredKeyValuePairings<Key, DatastoreEntryHolder<Value>>? {
            return ref.get()
        }

        fun update(storeKeyValueParing: StoredKeyValuePairings<Key, DatastoreEntryHolder<Value>>) {
            ref = WeakReference(storeKeyValueParing)
        }
    }

    private inner class StoredKeyValuePairingV2Iterator: TwoWayIterator<List<Value>> {
        private var currentPartitionIndex = AtomicInteger(0)
        private val _current = MutableSharedFlow<List<Value>>(1, 0)
        override val current: SharedFlow<List<Value>>
            get() = _current

        override val currentIndex: Int
            get() = currentPartitionIndex.get()

        init {
            storeBackgroundReadOpScope.launch {
                if (::writePartition.isInitialized) {
                    _current.emit(writePartition.second.retrieveAllPairingsValues().sortedByDescending { it.order }.map { it.entry })
                }
                partitionChangedFlow.collect { lastRefreshedPartition ->
                    if (partitionsRefs.size > 0) {
                        if (partitionsRefs.getOrNull(currentPartitionIndex.get())?.first?.id.equals(
                                lastRefreshedPartition.first.id
                            )
                        ) {
                            _current.emit(lastRefreshedPartition.second.retrieveAllPairingsValues().sortedByDescending { it.order }.map { it.entry })
                        }
                    }
                }
            }
        }

        override fun hasPrevious(): Boolean {
            return currentPartitionIndex.get() > 0
        }

        override suspend fun previous() = runBlocking(storeBackgroundReadOpDispatcher) {
            if (hasPrevious()) {
                _current.emit(getNewCurrent(currentPartitionIndex.decrementAndGet()))
            }
        }

        override fun hasNext(): Boolean {
            return (currentPartitionIndex.get() < (partitionsRefs.size - 1))
        }

        override suspend fun next() = runBlocking(storeBackgroundReadOpDispatcher) {
            if (hasNext()) {
                _current.emit(getNewCurrent(currentPartitionIndex.incrementAndGet()))
            }
        }

        private fun getNewCurrent(index: Int): List<Value> {
            val partition = partitionsRefs[index]
            var partitionRefToStoredKeyValueParing: StoredKeyValuePairings<Key, DatastoreEntryHolder<Value>>? = partition.second.get()

            if (partitionRefToStoredKeyValueParing == null) {
                partitionRefToStoredKeyValueParing = StoredKeyValuePairings<Key, DatastoreEntryHolder<Value>>(mutableMapOf(), JsonFileManager(toStoragePath(partition.first.id), Gson(), valueType))
                partition.second.update(partitionRefToStoredKeyValueParing)
            }
            return (partitionRefToStoredKeyValueParing.retrieveAllPairingsValues().sortedByDescending { it.order }.map { it.entry })
        }
    }

    private fun toStoragePath(fileName: String): String {
        return directoryOfDatastore + "$fileName.json"
    }

    private fun isOpen(): Boolean {
        return (mutabilityState.value == IAsyncMutableKeyValuePairings.MutabilityState.OPEN)
    }

    companion object {
        val DEFAULT_MAX_ENTRIES = 1000L
        val DEFAULT_PARTITION_MAX_ENTRIES = 255L
        val DEFAULT_WRITE_OP_QUEUE_MAX_CAPACITY = 100
        private val METADATA_KEY_PARTITION_ORDERING = "PARTITION_ORDERING"

        /**
         * Factory method to construct and return a instance of AsyncStoredKeyValuePairings which is in the opened state.
         */
        inline fun <reified Key, reified Value: AbsDatastoreEntry> newInstance(datastoreName: String, datastoreDirectory: String, maxPartitionEntries: Long = DEFAULT_PARTITION_MAX_ENTRIES, maxEntries: Long = DEFAULT_MAX_ENTRIES, writeOpQueueMaxCapacity: Int = DEFAULT_WRITE_OP_QUEUE_MAX_CAPACITY): AsyncStoredKeyValuePairings<Key, Value> {
            val storeKeyValueType = StoreKeyValueType.newInstance<Key, Value>()
            val newlyCreatedAsyncStoredKeyValuePairings = AsyncStoredKeyValuePairings<Key, Value>(datastoreName, datastoreDirectory, storeKeyValueType, maxPartitionEntries, maxEntries, writeOpQueueMaxCapacity)

            return newlyCreatedAsyncStoredKeyValuePairings
        }
    }

    /**
     * Key value type mapping to be stored within our datastore
     */
    interface IStoreKeyValueType {
        // Key value type being stored.
        val type: Type
    }

    /**
     * Key Value type mapping to be stored within our datastore.
     */
    abstract class StoreKeyValueType constructor(private val keyType: Type, private val valueType: Type): IStoreKeyValueType {
        override val type: Type
            get()  {
                val datastoreEntryHolderParameterizedType = TypeToken.getParameterized(DatastoreEntryHolder::class.java, valueType)
                return TypeToken.getParameterized(MutableMap::class.java, keyType, datastoreEntryHolderParameterizedType.type).type
            }
        companion object {
            inline fun <reified Key, reified Value: AbsDatastoreEntry> newInstance(): IStoreKeyValueType {
                return object: StoreKeyValueType(Key::class.java, Value::class.java) {}
            }
        }
    }
}
