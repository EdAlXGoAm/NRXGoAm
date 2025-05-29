package com.edalxgoam.nrxgoam.repository

import android.content.Context
import android.content.Intent
import com.edalxgoam.nrxgoam.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {
    
    private val auth = FirebaseAuth.getInstance()
    private val googleSignInClient: GoogleSignInClient
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    
    private val _isAuthenticated = MutableStateFlow(auth.currentUser != null)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    init {
        // Configurar Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(context, gso)
        
        // Escuchar cambios en el estado de autenticación
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            _isAuthenticated.value = firebaseAuth.currentUser != null
        }
    }
    
    /**
     * Obtiene el Intent para Google Sign-In
     */
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }
    
    /**
     * Maneja el resultado del Google Sign-In
     */
    suspend fun handleSignInResult(data: Intent): Result<FirebaseUser> {
        return try {
            println("AuthRepository: Handling sign-in result...")
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            
            println("AuthRepository: Got Google account - Email: ${account.email}, ID Token: ${account.idToken != null}")
            
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            println("AuthRepository: Created Firebase credential")
            
            val authResult = auth.signInWithCredential(credential).await()
            println("AuthRepository: Firebase auth completed")
            
            val user = authResult.user
            
            if (user != null) {
                println("AuthRepository: Firebase user created - UID: ${user.uid}, Email: ${user.email}")
                Result.success(user)
            } else {
                println("AuthRepository: Firebase user is null")
                Result.failure(Exception("Usuario nulo después de la autenticación"))
            }
        } catch (e: ApiException) {
            println("AuthRepository: Google Sign-In API Exception - Code: ${e.statusCode}, Message: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            println("AuthRepository: General exception - ${e.javaClass.simpleName}: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Cierra sesión del usuario
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            googleSignInClient.signOut().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene el usuario actual
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
    
    /**
     * Obtiene el UID del usuario actual
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * Verifica si hay un usuario autenticado
     */
    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }
    
    /**
     * Obtiene información del usuario actual
     */
    fun getUserInfo(): UserInfo? {
        val user = auth.currentUser
        return if (user != null) {
            UserInfo(
                uid = user.uid,
                email = user.email ?: "",
                displayName = user.displayName ?: "",
                photoUrl = user.photoUrl?.toString()
            )
        } else {
            null
        }
    }
}

/**
 * Clase de datos para información del usuario
 */
data class UserInfo(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null
)

/**
 * Estados posibles de autenticación
 */
sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object NotAuthenticated : AuthState()
    data class Error(val message: String) : AuthState()
} 