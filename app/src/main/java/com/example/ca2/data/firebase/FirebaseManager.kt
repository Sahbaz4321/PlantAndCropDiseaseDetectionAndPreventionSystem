package com.example.ca2.data.firebase

import com.example.ca2.data.model.Prediction
import com.example.ca2.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri

class FirebaseManager {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference.apply { 
        keepSynced(true) // Enable Offline Persistence for faster data access
    }
    private val storage = FirebaseStorage.getInstance().reference

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun saveUser(user: User, onComplete: (Boolean) -> Unit) {
        database.child("users").child(user.userId).setValue(user)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun getUser(userId: String, onResult: (User?) -> Unit) {
        // Use a listener to get data instantly from cache if available
        database.child("users").child(userId).addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                onResult(snapshot.getValue(User::class.java))
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                onResult(null)
            }
        })
    }

    fun savePrediction(prediction: Prediction, onComplete: (Boolean) -> Unit) {
        val id = database.child("predictions").child(prediction.userId).push().key ?: return
        val finalPrediction = prediction.copy(predictionId = id)
        database.child("predictions").child(prediction.userId).child(id).setValue(finalPrediction)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun saveOrUpdatePrediction(prediction: Prediction, onComplete: (Boolean, Prediction) -> Unit) {
        if (prediction.userId.isBlank()) {
            onComplete(false, prediction)
            return
        }

        val predictionId = prediction.predictionId.ifBlank {
            database.child("predictions").child(prediction.userId).push().key.orEmpty()
        }
        if (predictionId.isBlank()) {
            onComplete(false, prediction)
            return
        }

        val finalPrediction = prediction.copy(predictionId = predictionId)
        database.child("predictions")
            .child(prediction.userId)
            .child(predictionId)
            .setValue(finalPrediction)
            .addOnCompleteListener { onComplete(it.isSuccessful, finalPrediction) }
    }

    fun getPredictions(userId: String, onResult: (List<Prediction>) -> Unit) {
        database.child("predictions").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val predictions = snapshot.children.mapNotNull { it.getValue(Prediction::class.java) }
                onResult(predictions.sortedByDescending { it.createdAt })
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun deletePrediction(userId: String, predictionId: String, onComplete: (Boolean) -> Unit) {
        database.child("predictions")
            .child(userId)
            .child(predictionId)
            .removeValue()
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun uploadProfileImage(userId: String, imageUri: Uri, contentResolver: android.content.ContentResolver, onResult: (String?, String?) -> Unit) {
        val ref = storage.child("profile_images").child("$userId.jpg")
        
        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                onResult(null, "Could not open image file")
                return
            }

            val uploadTask = ref.putStream(inputStream)
            
            // Use continueWithTask to ensure downloadUrl is fetched only after successful upload
            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    onResult(downloadUri.toString(), null)
                } else {
                    onResult(null, task.exception?.message ?: "Upload failed")
                }
            }
        } catch (e: Exception) {
            onResult(null, e.message)
        }
    }

    fun logout() {
        auth.signOut()
    }
}
