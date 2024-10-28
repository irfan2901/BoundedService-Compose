package com.example.boundedservicecompose

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val myService = remember { mutableStateOf<MyService?>(null) }
    val isBound = remember { mutableStateOf(false) }

    val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MyService.LocalBinder
            myService.value = binder.getService()
            isBound.value = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            myService.value = null
            isBound.value = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            bindService(context, connection)
        } else {
            Toast.makeText(context, "Permission denied...", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        checkAndRequestPermission(context, permissionLauncher, connection)
    }

    DisposableEffect(Unit) {
        onDispose {
            unbindMusicService(context, connection, isBound)
        }
    }

    Column(
        modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {
            startMyService(myService.value, isBound.value)
        }) {
            Text("Play Music")
        }
        Spacer(modifier.height(10.dp))
        Button(onClick = {
            stopMyService(myService.value, isBound.value)
        }) {
            Text("Stop Music")
        }
    }
}

fun checkAndRequestPermission(
    context: Context,
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    connection: ServiceConnection
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            bindService(context, connection)
        }
    } else {
        bindService(context, connection)
    }
}

fun bindService(context: Context, connection: ServiceConnection) {
    val intent = Intent(context, MyService::class.java)
    context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
}

fun startMyService(service: MyService?, isBound: Boolean) {
    if (isBound) {
        service?.playMusic()
    }
}

fun stopMyService(service: MyService?, isBound: Boolean) {
    if (isBound) {
        service?.pauseMusic()
    }
}

fun unbindMusicService(
    context: Context,
    connection: ServiceConnection,
    isBound: MutableState<Boolean>
) {
    if (isBound.value) {
        context.unbindService(connection)
        isBound.value = false
    }
}
