package com.example.ca2

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.navOptions
import com.example.ca2.data.firebase.FirebaseManager
import com.example.ca2.databinding.ActivityMainBinding
import com.example.ca2.databinding.NavDrawerHeaderBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val firebaseManager = FirebaseManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topAppBar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.scanFragment, R.id.historyFragment, R.id.profileFragment),
            binding.drawerLayout
        )

        binding.topAppBar.setupWithNavController(navController, appBarConfiguration)
        binding.bottomNavigation.setupWithNavController(navController)
        binding.bottomNavigation.setOnItemReselectedListener { }

        setupDrawer()
        refreshDrawerHeader()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val chromeHiddenDestinations = setOf(
                R.id.splashFragment,
                R.id.loginFragment,
                R.id.signupFragment,
                R.id.analysisFragment
            )
            val topLevelDestinationIds = appBarConfiguration.topLevelDestinations
            val isTopLevel = destination.id in topLevelDestinationIds
            val showChrome = destination.id !in chromeHiddenDestinations

            binding.topAppBar.visibility = if (showChrome) View.VISIBLE else View.GONE
            binding.bottomNavigation.visibility = if (showChrome && isTopLevel) View.VISIBLE else View.GONE
            binding.drawerLayout.setDrawerLockMode(
                if (showChrome && isTopLevel) DrawerLayout.LOCK_MODE_UNLOCKED
                else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
            )

            val checkedItem = when (destination.id) {
                R.id.scanFragment -> R.id.menu_import_scan
                else -> destination.id
            }
            binding.navigationView.setCheckedItem(checkedItem)
            refreshDrawerHeader()
        }
    }

    private fun setupDrawer() {
        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_import_scan -> {
                    if (navController.currentDestination?.id != R.id.scanFragment) {
                        navController.navigate(R.id.scanFragment)
                    }
                }
                R.id.menu_logout -> {
                    firebaseManager.logout()
                    navController.navigate(R.id.loginFragment, null, navOptions {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    })
                }
                else -> {
                    if (navController.currentDestination?.id != item.itemId) {
                        navController.navigate(item.itemId)
                    }
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun refreshDrawerHeader() {
        val headerBinding = NavDrawerHeaderBinding.bind(binding.navigationView.getHeaderView(0))
        val userId = firebaseManager.getCurrentUserId()
        if (userId == null) {
            headerBinding.tvDrawerName.text = getString(R.string.app_name)
            headerBinding.tvDrawerEmail.text = getString(R.string.tagline)
            return
        }

        firebaseManager.getUser(userId) { user ->
            runOnUiThread {
                headerBinding.tvDrawerName.text = user?.name?.ifBlank { getString(R.string.app_name) }
                    ?: getString(R.string.app_name)
                headerBinding.tvDrawerEmail.text = user?.email?.ifBlank { getString(R.string.tagline) }
                    ?: getString(R.string.tagline)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
