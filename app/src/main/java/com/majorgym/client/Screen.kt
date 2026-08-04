package com.majorgym.client

/** Just two real pages in this app — everything else (QR scanning for
 *  membership or attendance) happens as a full-screen overlay dialog on top
 *  of one of these two, not as a separate navigable page. */
sealed class Screen {
    data object Home : Screen()
    data object Attendance : Screen()
}
