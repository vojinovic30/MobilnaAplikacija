package com.example.mobilneprojekat

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class AuthViewModel: ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    init {
        checkAuthStatus()
    }


    fun checkAuthStatus() {
        if(auth.currentUser == null){
            _authState.value = AuthState.Unauthenticated
        }
        else {
            _authState.value = AuthState.Authenticated
        }
    }

    fun login(email: String, password: String) {

        if(email.isEmpty() || password.isEmpty()) {
            _authState.value=AuthState.Error("Email or password can't be empty")
            return
        }

        _authState.value=AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener{task->
                if(task.isSuccessful){
                    _authState.value=AuthState.Authenticated
                }
                else{
                    _authState.value=AuthState.Error(task.exception?.message?:"Something went wrong")
                }
            }
    }

    private fun registerUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        points:Int,
        imageUri: Uri?,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        val profileData = hashMapOf(
                            "id" to userId,
                            "email" to email,
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "phoneNumber" to phoneNumber,
                            "password" to password,
                            "points" to points
                        )

                        // Čuvanje podataka u Firestore
                        db.collection("users").document(userId).set(profileData)
                            .addOnSuccessListener {
                                // Upload slike ako postoji
                                imageUri?.let { uri ->
                                    val storageRef = storage.reference.child("profile_photos/$userId.jpg")
                                    storageRef.putFile(uri)
                                        .addOnSuccessListener {
                                            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                                                db.collection("users").document(userId)
                                                    .update("photoUrl", downloadUrl.toString())
                                                    .addOnSuccessListener {
                                                        onResult(true, null)
                                                    }
                                            }
                                        }
                                        .addOnFailureListener { exception ->
                                            onResult(false, exception.message)
                                        }
                                } ?: run {
                                    onResult(true, null)
                                }
                            }
                            .addOnFailureListener { exception ->
                                onResult(false, exception.message)
                            }
                    } else {
                        onResult(false, "User ID is null")
                    }
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun register(
        email: String,
        password: String,
        phone:String,
        firstName:String,
        lastName:String,
        imageUri: Uri?
    ) {

        if(email.isEmpty() || password.isEmpty() || phone.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || imageUri == null) {
            _authState.value=AuthState.Error("Each field must not be empty")
            return
        }

        _authState.value=AuthState.Loading
        registerUser(email, password, firstName, lastName, phone, 5, imageUri) { success, message ->
            _authState.value = if (success) {
                AuthState.Authenticated
            } else {
                AuthState.Error(message?: "Something went wrong")
            }
            //.addOnCompleteListener{task->
              //  if(task.isSuccessful){
                  //  _authState.value=AuthState.Authenticated
                //}
                //else{
               //     _authState.value=AuthState.Error(task.exception?.message?:"Something went wrong")
                //}
        }
    }


    fun signOut() {
        auth.signOut()
        _authState.value=AuthState.Unauthenticated
    }

}


sealed class AuthState {
    object Authenticated: AuthState()
    object Unauthenticated: AuthState()
    object Loading: AuthState()
    data class Error(val message: String):AuthState()
}