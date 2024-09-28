package com.anurupjaiswal.ajmediapicker.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.anurupjaiswal.ajmediapicker.R
import kotlinx.coroutines.*

// **Splash Screen Activity** - Displays a splash screen for a specified duration before transitioning to the main activity.


class SplashscreenActivity : AppCompatActivity() {

    // **Splash Screen Duration** - Time (in milliseconds) the splash screen will be displayed.
    private val splashTimeOut: Long = 2000



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // **Set Content View** - Apply the layout resource for the splash screen.
        setContentView(R.layout.activity_splashscreen)

        // **Coroutine Scope** - Launch a coroutine to manage the delay for the splash screen.
        CoroutineScope(Dispatchers.Main).launch {
            // **Delay** - Pause the coroutine for the duration of the splash screen.
            delay(splashTimeOut)

            startActivity(Intent(this@SplashscreenActivity, MainActivity::class.java))

            // Finish SplashscreenActivity** - Close the splash screen activity to prevent returning to it.

            finish()
        }
    }
}
