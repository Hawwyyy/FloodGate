package com.example.floodgate.ledcontrol.model

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FirebaseLedControlRepository : LedControlRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val ref = database.getReference("eldroid/ledControl")
    private var listener: ValueEventListener? = null
    private var offsetListener: ValueEventListener? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var offset = 0L

    override fun observe(onState: (LedControlState) -> Unit, onError: (String) -> Unit, onAuthLost: () -> Unit) {
        stop()
        if (auth.currentUser == null) { onAuthLost(); return }
        authListener = FirebaseAuth.AuthStateListener {
            if (it.currentUser == null) { stop(); onAuthLost() }
        }.also(auth::addAuthStateListener)
        offsetListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { offset = snapshot.value as? Long ?: 0L }
            override fun onCancelled(error: DatabaseError) { offset = 0L }
        }.also { database.getReference(".info/serverTimeOffset").addValueEventListener(it) }
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onState(LedControlState(
                    command = (snapshot.child("command").value as? String)?.takeIf { it == "ON" || it == "OFF" },
                    actualState = snapshot.child("actualState").value as? Boolean,
                    lastUpdate = (snapshot.child("device/lastUpdate").value as? Long)?.takeIf { it > 0 },
                    heartbeat = (snapshot.child("device/heartbeat").value as? Long)?.takeIf { it >= 0 }
                ))
            }
            override fun onCancelled(error: DatabaseError) {
                onError("Unable to read LED status. Check your connection and database permissions.")
            }
        }.also(ref::addValueEventListener)
    }

    override fun send(command: String, onComplete: (Boolean) -> Unit) {
        if (auth.currentUser == null || command !in listOf("ON", "OFF")) { onComplete(false); return }
        ref.child("command").setValue(command).addOnCompleteListener { onComplete(it.isSuccessful) }
    }
    override fun serverTime() = System.currentTimeMillis() + offset
    override fun stop() {
        listener?.let(ref::removeEventListener)
        offsetListener?.let { database.getReference(".info/serverTimeOffset").removeEventListener(it) }
        authListener?.let(auth::removeAuthStateListener)
        listener = null; offsetListener = null; authListener = null
    }
}
