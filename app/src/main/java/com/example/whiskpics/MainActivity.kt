package com.example.whiskpics

import android.content.res.Configuration
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.whiskpics.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Ordered set of images shown by the viewer.
    private val images = listOf(
        R.drawable.photo_banh_mi,
        R.drawable.photo_pasta,
        R.drawable.photo_salad,
        R.drawable.photo_tiramisu,
        R.drawable.photo_latte,
        R.drawable.photo_steak
    )

    // Index into [images] for the photo currently on screen.
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        applyPersistedThemeMode()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentIndex = savedInstanceState?.getInt(KEY_CURRENT_INDEX) ?: currentIndex

        binding.buttonBack.setOnClickListener { showPreviousImage() }
        binding.buttonNext.setOnClickListener { showNextImage() }
        binding.buttonThemeToggle.setOnClickListener { toggleThemeMode() }

        updateThemeToggleIcon()
        displayImage(animate = false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_INDEX, currentIndex)
    }

    /** Steps back one image, if not already at the first, and refreshes the UI. */
    private fun showPreviousImage() {
        if (currentIndex == 0) return
        currentIndex--
        displayImage(animate = true)
    }

    /** Steps forward one image, if not already at the last, and refreshes the UI. */
    private fun showNextImage() {
        if (currentIndex == images.lastIndex) return
        currentIndex++
        displayImage(animate = true)
    }

    /**
     * Renders the image at [currentIndex], crossfading into it when [animate] is true,
     * then keeps the position label and button states in sync.
     */
    private fun displayImage(animate: Boolean) {
        if (animate) {
            val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) = Unit
                override fun onAnimationRepeat(animation: Animation?) = Unit
                override fun onAnimationEnd(animation: Animation?) {
                    setImageContent()
                    binding.imageView.startAnimation(
                        AnimationUtils.loadAnimation(this@MainActivity, R.anim.fade_in)
                    )
                }
            })
            binding.imageView.startAnimation(fadeOut)
        } else {
            setImageContent()
        }
        updatePositionIndicator()
        updateButtonStates()
    }

    /** Applies the current image's drawable and accessibility description to the ImageView. */
    private fun setImageContent() {
        binding.imageView.setImageResource(images[currentIndex])
        binding.imageView.contentDescription =
            getString(R.string.image_content_description, currentIndex + 1, images.size)
    }

    /** Updates the "Image X / Y" label to reflect the current position. */
    private fun updatePositionIndicator() {
        binding.textPosition.text =
            getString(R.string.image_position_format, currentIndex + 1, images.size)
    }

    /** Disables and dims Back/Next at the first/last image so navigation cannot wrap or crash. */
    private fun updateButtonStates() {
        val isFirstImage = currentIndex == 0
        val isLastImage = currentIndex == images.lastIndex

        binding.buttonBack.isEnabled = !isFirstImage
        binding.buttonBack.alpha = if (isFirstImage) DISABLED_ALPHA else ENABLED_ALPHA

        binding.buttonNext.isEnabled = !isLastImage
        binding.buttonNext.alpha = if (isLastImage) DISABLED_ALPHA else ENABLED_ALPHA
    }

    /**
     * Applies a previously chosen light/dark override, if any, before the theme resolves for
     * this activity so the correct mode renders from the very first frame (no flash).
     */
    private fun applyPersistedThemeMode() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (!prefs.contains(KEY_DARK_MODE_ENABLED)) return
        AppCompatDelegate.setDefaultNightMode(
            if (prefs.getBoolean(KEY_DARK_MODE_ENABLED, false)) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )
    }

    /** Flips light/dark mode, persists the choice, and lets AppCompatDelegate recreate the activity. */
    private fun toggleThemeMode() {
        val switchingToDark = !isCurrentlyInDarkMode()
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_MODE_ENABLED, switchingToDark)
            .apply()
        AppCompatDelegate.setDefaultNightMode(
            if (switchingToDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    /** Reports whether the UI is currently rendering in dark mode, however that mode was chosen. */
    private fun isCurrentlyInDarkMode(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    /** Syncs the toggle button's icon and content description with the currently active theme. */
    private fun updateThemeToggleIcon() {
        if (isCurrentlyInDarkMode()) {
            binding.buttonThemeToggle.setIconResource(R.drawable.ic_theme_moon)
            binding.buttonThemeToggle.contentDescription =
                getString(R.string.cd_theme_toggle_to_light)
        } else {
            binding.buttonThemeToggle.setIconResource(R.drawable.ic_theme_sun)
            binding.buttonThemeToggle.contentDescription =
                getString(R.string.cd_theme_toggle_to_dark)
        }
    }

    companion object {
        private const val ENABLED_ALPHA = 1.0f
        private const val DISABLED_ALPHA = 0.4f

        private const val PREFS_NAME = "whisk_pics_prefs"
        private const val KEY_DARK_MODE_ENABLED = "dark_mode_enabled"
        private const val KEY_CURRENT_INDEX = "current_index"
    }
}
