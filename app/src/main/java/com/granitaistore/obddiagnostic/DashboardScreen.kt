package com.granitaistore.obddiagnostic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(navController: NavController) {

    var rpm by remember { mutableStateOf(0) }
    var speed by remember { mutableStateOf(0) }
    var coolant by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            rpm = (800..4500).random()
            speed = (0..120).random()
            coolant = (70..95).random()
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F14))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "OPEL OBD DASHBOARD",
            color = Color(0xFF00FF88),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        GaugeCard("RPM", "$rpm", "rpm", Color(0xFF00C8FF))
        GaugeCard("Speed", "$speed", "km/h", Color(0xFFE8FF00))
        GaugeCard("Coolant", "$coolant", "°C", Color(0xFFFF3B3B))

        Spacer(Modifier.height(20.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = { navController.navigate("scan") }) {
                Text("SCAN")
            }
            Button(onClick = { navController.navigate("can_raw") }) {
                Text("CAN RAW")
            }
            Button(onClick = { /* CSV later */ }) {
                Text("CSV LOG")
            }
        }
    }
}

@Composable
fun GaugeCard(title: String, value: String, unit: String, color: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151B28)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color(0xFF9AA4B2), fontSize = 14.sp)
            Text(
                text = "$value $unit",
                color = color,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
