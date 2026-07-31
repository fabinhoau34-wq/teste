package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PhotoshopHeader
import com.example.ui.screens.Tab1InspectionScreen
import com.example.ui.screens.Tab2CalculatorScreen
import com.example.ui.screens.Tab3MeetingAgendaScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.CalculatorViewModel
import com.example.ui.viewmodel.InspectionViewModel
import com.example.ui.viewmodel.MeetingViewModel

class MainActivity : ComponentActivity() {

    private val inspectionViewModel: InspectionViewModel by viewModels()
    private val calculatorViewModel: CalculatorViewModel by viewModels()
    private val meetingViewModel: MeetingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VistoriaTheme {
                var selectedTabIndex by remember { mutableIntStateOf(0) }

                val tabTitles = listOf("FJGeren", "Calc. Engenharia", "Agenda Reuniões")

                Scaffold(
                    topBar = {
                        PhotoshopHeader(
                            title = tabTitles[selectedTabIndex],
                            subtitle = "SISTEMA DE VISTORIA & ENGENHARIA CIVIL",
                            activeTab = selectedTabIndex
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurface,
                            contentColor = Color.White,
                            tonalElevation = 8.dp,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            NavigationBarItem(
                                selected = selectedTabIndex == 0,
                                onClick = { selectedTabIndex = 0 },
                                icon = { Icon(Icons.Default.Assignment, contentDescription = "1ª Aba: Vistoria") },
                                label = { Text("1ª Vistoria", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = OrangePrimary,
                                    indicatorColor = OrangePrimary,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )

                            NavigationBarItem(
                                selected = selectedTabIndex == 1,
                                onClick = { selectedTabIndex = 1 },
                                icon = { Icon(Icons.Default.Build, contentDescription = "2ª Aba: Calculadora") },
                                label = { Text("2ª Calculadora", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = OrangePrimary,
                                    indicatorColor = OrangePrimary,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )

                            NavigationBarItem(
                                selected = selectedTabIndex == 2,
                                onClick = { selectedTabIndex = 2 },
                                icon = { Icon(Icons.Default.CalendarToday, contentDescription = "3ª Aba: Agenda") },
                                label = { Text("3ª Agenda", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = OrangePrimary,
                                    indicatorColor = OrangePrimary,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(DarkBackground)
                    ) {
                        when (selectedTabIndex) {
                            0 -> Tab1InspectionScreen(viewModel = inspectionViewModel)
                            1 -> Tab2CalculatorScreen(viewModel = calculatorViewModel)
                            2 -> Tab3MeetingAgendaScreen(viewModel = meetingViewModel)
                        }
                    }
                }
            }
        }
    }
}

