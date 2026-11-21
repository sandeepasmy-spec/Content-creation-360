package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ai.FirebaseAiPromptRepository
import com.example.firebase.FirestoreWorkspaceRepository
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.firebase.signOutGoogle
import com.example.ui.MainShell
import com.example.ui.screens.GoogleAuthScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.StudioFlowTheme
import com.example.viewmodel.StudioFlowViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            StudioFlowTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val credentialManager = remember(context) { CredentialManager.create(context) }
                val firebaseUser by remember { Firebase.auth.authStateFlow() }
                    .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

                val activeUser = firebaseUser
                if (activeUser == null) {
                    GoogleAuthScreen(onAuthSuccess = { /* authStateFlow updates automatically */ })
                } else {
                    key(activeUser.uid) {
                        val vm: StudioFlowViewModel = viewModel(
                            key = activeUser.uid,
                            factory = viewModelFactory {
                                initializer {
                                    val app = checkNotNull(this[APPLICATION_KEY])
                                    val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                                    StudioFlowViewModel(
                                        repo = FirestoreWorkspaceRepository(db),
                                        aiRepo = FirebaseAiPromptRepository(),
                                        currentUserId = activeUser.uid,
                                        userEmail = activeUser.email,
                                        userDisplayName = activeUser.displayName
                                    )
                                }
                            }
                        )
                        val state by vm.uiState.collectAsStateWithLifecycle()

                        when {
                            state.isInitializing -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                            state.currentUser == null -> {
                                LoginScreen(
                                    googleEmail = state.googleEmail,
                                    users = state.users,
                                    isBusy = state.isBusy,
                                    authError = state.authError ?: state.firestoreError,
                                    bannerMessage = state.bannerMessage,
                                    onSelectPersona = vm::selectRolePersona,
                                    onSignOutGoogle = {
                                        signOutGoogle(credentialManager, scope)
                                    },
                                    onClearError = vm::clearAuthError
                                )
                            }
                            else -> {
                                MainShell(
                                    state = state,
                                    viewModel = vm,
                                    onSignOutGoogle = {
                                        signOutGoogle(credentialManager, scope)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
