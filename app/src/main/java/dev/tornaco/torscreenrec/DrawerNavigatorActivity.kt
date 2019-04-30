package dev.tornaco.torscreenrec

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.common.collect.ImmutableList
import dev.tornaco.torscreenrec.common.SharedExecutor
import dev.tornaco.torscreenrec.pref.SettingsProvider
import dev.tornaco.torscreenrec.ui.*
import dev.tornaco.torscreenrec.ui.widget.RecordingButton

class DrawerNavigatorActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    var cardController: FragmentController? = null
        private set

    var floatingActionButton: RecordingButton? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        floatingActionButton = findViewById(R.id.fab)

        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<View>(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)

        setupFragment()
    }

    protected fun setupFragment() {
        val cards = ImmutableList.of(
                ScreenCastFragment.create())
        cardController = FragmentController(supportFragmentManager, cards, R.id.container)
        cardController!!.setDefaultIndex(INDEX_SCREEN_CAST)
        cardController!!.setCurrent(INDEX_SCREEN_CAST)
    }

    override fun onResume() {
        super.onResume()
        val uName = SettingsProvider.get()!!.getString(SettingsProvider.Key.USR_NAME)
        val textView = findViewById<View>(R.id.user_name) as? TextView
        if (textView != null) {
            textView.text = uName
        }
    }

    override fun onBackPressed() {
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId

        when (id) {
            R.id.nav_cast -> cardController?.setCurrent(INDEX_SCREEN_CAST)
            R.id.nav_shop -> SharedExecutor.runOnUIThreadDelayed(Runnable {
                startActivity(ContainerHostActivity.getIntent(applicationContext, ShopFragment::class.java))
            }, 300)
            R.id.nav_about -> SharedExecutor.runOnUIThreadDelayed(Runnable {
                startActivity(ContainerHostActivity.getIntent(applicationContext, AboutFragment::class.java))
            }, 300)
        }

        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    companion object {

        private val INDEX_SCREEN_CAST = 0
    }
}
