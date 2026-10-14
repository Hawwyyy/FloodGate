package com.example.floodgate.ledcontrol.model

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

/** Metadata only; registering a name never writes a command or invents telemetry. */
class GateDirectoryRepository {
    private val ref = FirebaseDatabase.getInstance().getReference("eldroid/gates")
    private var listener: ValueEventListener? = null

    fun observe(onDevices: (List<GateDevice>) -> Unit, onError: () -> Unit) {
        stop()
        if (FirebaseAuth.getInstance().currentUser == null) { onError(); return }
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val devices = snapshot.children.mapNotNull { child ->
                    val id = child.key ?: return@mapNotNull null
                    val name = (child.child("name").value as? String)?.trim()
                    if (id == "gate1" || !GateDevice.validId(id) || name.isNullOrEmpty()) null
                    else GateDevice(id, name.take(60))
                }.sortedBy { it.id }
                onDevices(listOf(GateDevice.Existing) + devices)
            }
            override fun onCancelled(error: DatabaseError) = onError()
        }.also(ref::addValueEventListener)
    }

    fun add(id: String, name: String, done: (Boolean) -> Unit) {
        if (FirebaseAuth.getInstance().currentUser == null || !GateDevice.validId(id) ||
            id == "gate1" || name.isBlank() || name.length > 60) { done(false); return }
        // Do not overwrite another registered gate, including concurrent registration.
        ref.child(id).child("name").runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                if (data.value != null) return Transaction.abort()
                data.value = name.trim()
                return Transaction.success(data)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                done(error == null && committed)
            }
        })
    }

    fun stop() { listener?.let(ref::removeEventListener); listener = null }
}
