package com.granitaistore.obddiagnostic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class CanFrame(
    val time: String,
    val id: String,
    val data: String
)

@Composable
fun CanRawScreen(navController: NavController) {

    val frames = remember { mutableStateListOf<CanFrame>() }
    val sdf = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        while (true) {
            val frame = CanFrame(
                time = sdf.format(Date()),
                id = "7E8",
                data = List(8) { "%02X".format((0..255).random()) }.joinToString(" ")
            )
            frames.add(0, frame)
            if (frames.size > 200) frames.removeLast()
            delay(200)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F14))
            .padding(12.dp)
    ) {

        Text(
            text = "CAN RAW FRAMES",
            color = Color(0xFF00FF88),
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(frames) { frame ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(frame.time, color = Color.Gray, modifier = Modifier.width(90.dp))
                    Text(frame.id, color = Color(0xFF00C8FF), modifier = Modifier.width(50.dp))
                    Text(
                        frame.data,
                        color = Color(0xFFE8FF00),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
