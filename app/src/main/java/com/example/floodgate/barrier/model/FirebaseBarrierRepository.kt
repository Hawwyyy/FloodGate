package com.example.floodgate.barrier.model

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FirebaseBarrierRepository : BarrierRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()
    private val device = db.getReference("devices/floodgate01")
    private val subscriptions = mutableListOf<Pair<DatabaseReference, ValueEventListener>>()
    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var offset = 0L

    override fun observe(onTelemetry: (BarrierTelemetry) -> Unit, onConnection: (Boolean) -> Unit,
                         onError: (BarrierError) -> Unit, onSignedOut: () -> Unit) {
        stop()
        val uid = auth.currentUser?.uid ?: run { onSignedOut(); return }
        authListener = FirebaseAuth.AuthStateListener {
            if (it.currentUser?.uid != uid) { stop(); onSignedOut() }
        }.also(auth::addAuthStateListener)
        watch(db.getReference(".info/serverTimeOffset"), {
            offset = (it.value as? Long) ?: 0L
        }, onError)
        watch(db.getReference(".info/connected"), { onConnection(it.value == true) }, {
            onConnection(false); onError(it)
        })
        watch(device, { onTelemetry(BarrierTelemetry.parse(it.value)) }, onError)
    }

    private fun watch(ref: DatabaseReference, changed: (DataSnapshot) -> Unit, failed: (BarrierError) -> Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) = changed(snapshot)
            override fun onCancelled(error: DatabaseError) = failed(mapError(error))
        }
        subscriptions.add(ref to listener)
        ref.addValueEventListener(listener)
    }

    override fun send(action: BarrierAction, id: String, complete: (BarrierError?) -> Unit) {
        if (auth.currentUser == null) { complete(BarrierError.PERMISSION); return }
        // Update only the mailbox children; never overwrite telemetry or clear the command.
        device.child("command").updateChildren(action.children(id)) { error, _ ->
            complete(error?.let(::mapError))
        }
    }

    override fun serverTime() = System.currentTimeMillis() + offset
    override fun stop() {
        subscriptions.forEach { (ref, listener) -> ref.removeEventListener(listener) }
        subscriptions.clear()
        authListener?.let(auth::removeAuthStateListener)
        authListener = null
    }

    private fun mapError(error: DatabaseError) = when (error.code) {
        DatabaseError.PERMISSION_DENIED -> BarrierError.PERMISSION
        DatabaseError.DISCONNECTED, DatabaseError.NETWORK_ERROR -> BarrierError.NETWORK
        else -> BarrierError.OTHER
    }
}
