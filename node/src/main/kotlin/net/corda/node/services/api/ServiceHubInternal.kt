package net.corda.node.services.api

import com.google.common.annotations.VisibleForTesting
import com.google.common.util.concurrent.ListenableFuture
import net.corda.core.flows.*
import net.corda.core.messaging.SingleMessageRecipient
import net.corda.core.node.NodeInfo
import net.corda.core.node.PluginServiceHub
import net.corda.core.node.services.IdentityServiceInternal
import net.corda.core.node.services.NetworkMapCache
import net.corda.core.node.services.TxWritableStorageService
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.loggerFor
import net.corda.node.internal.ServiceFlowInfo
import net.corda.node.services.messaging.MessagingService
import net.corda.node.services.statemachine.FlowStateMachineImpl

interface NetworkMapCacheInternal : NetworkMapCache {
    /**
     * Deregister from updates from the given map service.
     * @param net the network messaging service.
     * @param service the network map service to fetch current state from.
     */
    fun deregisterForUpdates(net: MessagingService, service: NodeInfo): ListenableFuture<Unit>

    /**
     * Add a network map service; fetches a copy of the latest map from the service and subscribes to any further
     * updates.
     * @param net the network messaging service.
     * @param networkMapAddress the network map service to fetch current state from.
     * @param subscribe if the cache should subscribe to updates.
     * @param ifChangedSinceVer an optional version number to limit updating the map based on. If the latest map
     * version is less than or equal to the given version, no update is fetched.
     */
    fun addMapService(net: MessagingService, networkMapAddress: SingleMessageRecipient,
                      subscribe: Boolean, ifChangedSinceVer: Int? = null): ListenableFuture<Unit>

    /** Adds a node to the local cache (generally only used for adding ourselves). */
    fun addNode(node: NodeInfo)

    /** Removes a node from the local cache. */
    fun removeNode(node: NodeInfo)

    /** For testing where the network map cache is manipulated marks the service as immediately ready. */
    @VisibleForTesting
    fun runWithoutMapService()

}

interface FlowLogicRefFactoryInternal : FlowLogicRefFactory {
    val flowWhitelist: Map<String, Set<String>>
    fun toFlowLogic(ref: FlowLogicRef): FlowLogic<*>
}

@CordaSerializable
sealed class NetworkCacheError : Exception() {
    /** Indicates a failure to deregister, because of a rejected request from the remote node */
    class DeregistrationFailed : NetworkCacheError()
}

abstract class ServiceHubInternal : PluginServiceHub {
    companion object {
        private val log = loggerFor<ServiceHubInternal>()
    }

    abstract val monitoringService: MonitoringService
    abstract val flowLogicRefFactory: FlowLogicRefFactoryInternal
    abstract val schemaService: SchemaService
    abstract override val networkMapCache: NetworkMapCacheInternal
    abstract val schedulerService: SchedulerService

    abstract val networkService: MessagingService
    override abstract val identityService: IdentityServiceInternal

    /**
     * Given a list of [SignedTransaction]s, writes them to the given storage for validated transactions and then
     * sends them to the vault for further processing. This is intended for implementations to call from
     * [recordTransactions].
     *
     * @param txs The transactions to record.
     */
    internal fun recordTransactionsInternal(writableStorageService: TxWritableStorageService, txs: Iterable<SignedTransaction>) {
        val stateMachineRunId = FlowStateMachineImpl.currentStateMachine()?.id
        val recordedTransactions = txs.filter { writableStorageService.validatedTransactions.addTransaction(it) }
        if (stateMachineRunId != null) {
            recordedTransactions.forEach {
                storageService.stateMachineRecordedTransactionMapping.addMapping(stateMachineRunId, it.id)
            }
        } else {
            log.warn("Transactions recorded from outside of a state machine")
        }
        vaultService.notifyAll(recordedTransactions.map { it.tx })
    }

    /**
     * Starts an already constructed flow. Note that you must be on the server thread to call this method. [FlowInitiator]
     * defaults to [FlowInitiator.RPC] with username "Only For Testing".
     */
    // TODO Move it to test utils.
    @VisibleForTesting
    fun <T> startFlow(logic: FlowLogic<T>): FlowStateMachine<T> = startFlow(logic, FlowInitiator.RPC("Only For Testing"))

    /**
     * Starts an already constructed flow. Note that you must be on the server thread to call this method.
     * @param flowInitiator indicates who started the flow, see: [FlowInitiator].
     */
    abstract fun <T> startFlow(logic: FlowLogic<T>, flowInitiator: FlowInitiator): FlowStateMachineImpl<T>


    /**
     * Will check [logicType] and [args] against a whitelist and if acceptable then construct and initiate the flow.
     * Note that you must be on the server thread to call this method. [flowInitiator] points how flow was started,
     * See: [FlowInitiator].
     *
     * @throws net.corda.core.flows.IllegalFlowLogicException or IllegalArgumentException if there are problems with the
     * [logicType] or [args].
     */
    fun <T : Any> invokeFlowAsync(
            logicType: Class<out FlowLogic<T>>,
            flowInitiator: FlowInitiator,
            vararg args: Any?): FlowStateMachineImpl<T> {
        val logicRef = flowLogicRefFactory.create(logicType, *args)
        @Suppress("UNCHECKED_CAST")
        val logic = flowLogicRefFactory.toFlowLogic(logicRef) as FlowLogic<T>
        return startFlow(logic, flowInitiator)
    }

    abstract fun getServiceFlowFactory(clientFlowClass: Class<out FlowLogic<*>>): ServiceFlowInfo?
}