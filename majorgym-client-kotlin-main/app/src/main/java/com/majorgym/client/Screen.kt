package com.majorgym.client

/** Mirrors the owner app's manual sealed-class navigation (Screen.kt) —
 *  no Navigation-Compose dependency, just a state variable in MainActivity. */
sealed class Screen {
    data object Home : Screen()
    data object Attendance : Screen()
    data object ScanProfile : Screen()
    data object ScanAttendance : Screen()
}
