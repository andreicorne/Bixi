package com.example.bixi.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.appcompat.widget.ActionMenuView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.bixi.R
import com.example.bixi.databinding.ActivityMainBinding
import com.example.bixi.fragments.TasksFragment
import com.example.bixi.helper.BackgroundStylerService
import com.example.bixi.helper.LocaleHelper
import com.example.bixi.services.DialogService
import com.example.bixi.services.RetrofitClient
import com.example.bixi.services.SecureStorageService
import com.google.android.material.navigation.NavigationView

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)

        val item = menu.findItem(R.id.action_custom)
        val actionView = item.actionView
        menuItemView = actionView!!.findViewById(R.id.menu_add_icon)

        // Poți seta click direct aici (sau în onOptionsItemSelected)
        menuItemView?.setOnClickListener {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            val currentFragment =
                navHostFragment?.childFragmentManager?.primaryNavigationFragment

            if (currentFragment is TasksFragment) {
                currentFragment.createTask(menuItemView)
            }
        }

        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupLoadingOverlay()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupNavigationDrawer()
        setupNavigationDrawerHeader()
        setToolbarMenuVisibility()
        setStyles()
    }

    lateinit var menuItemView: View

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.action_custom -> {
//                val navHostFragment =
//                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
//                val currentFragment =
//                    navHostFragment?.childFragmentManager?.primaryNavigationFragment
//
//                if (currentFragment is TasksFragment) {
//                    currentFragment.createTask(menuItemView)
//                }
//
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }

    private fun setupNavigationDrawer(){
        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.tasksFragment, R.id.timekeepingFragment),
            binding.drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.logout -> {
                    showLogoutConfirmationDialog()
                    true
                }
                else -> {
                    NavigationUI.onNavDestinationSelected(menuItem, navController)
                    binding.drawerLayout.closeDrawers()
                    true
                }
            }
        }
    }

    private fun setToolbarMenuVisibility(){
        setSupportActionBar(binding.toolbar)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showButton = destination.id == R.id.tasksFragment
            binding.toolbar.menu.findItem(R.id.action_custom)?.isVisible = showButton
        }
    }

    private fun setupNavigationDrawerHeader(){
        val headerView = findViewById<NavigationView>(R.id.nav_view).getHeaderView(0)
        val userName = headerView.findViewById<TextView>(R.id.nav_header_title)
        userName.text = "Bine ai venit, Andrei"
    }

    private fun setStyles(){
    }

    private fun showLogoutConfirmationDialog() {
        DialogService.showConfirmationDialog(
            context = this,
            title = getString(R.string.logout_confirmation_title),
            message = getString(R.string.logout_confirmation_details),
//            iconResId = R.drawable.ic_logout,
            positiveText = getString(R.string.yes),
            negativeText = getString(R.string.no),
            onConfirmed = {
                performLogout()
            },
            onCancelled = {
            }
        )
    }

    private fun performLogout(){
        RetrofitClient.logout()
        SecureStorageService.clearAll(this)
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish() // Omoară și activity-ul curent ca fallback
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}