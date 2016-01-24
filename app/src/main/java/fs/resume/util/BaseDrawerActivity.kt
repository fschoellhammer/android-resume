package fs.resume.util

import android.content.Intent
import fs.resume.R
import android.support.annotation.LayoutRes
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.view.MenuItem
import fs.resume.education.EducationActivity
import fs.resume.timeline.TimelineActivity
import kotlinx.android.synthetic.main.base_drawer_activity.*

/**
 * Created by flo.schoellhammer on 1/24/16.
 */
 abstract class BaseDrawerActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    @LayoutRes
    override val baseLayout = R.layout.base_drawer_activity

    protected abstract val selectedMenuId : Int

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)

        // modify toolbar
        supportActionBar.setHomeAsUpIndicator(getDrawableWithTint(R.drawable.ic_menu_black_24dp, R.color.white))
        // setup drawer
        navigator.setNavigationItemSelectedListener(this)
        navigator.menu.findItem(selectedMenuId)?.setChecked(true)
        navigator.menu.findItem(selectedMenuId)?.setCheckable(true)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val intent = when(item.itemId) {
            R.id.menu_education -> Intent(this, EducationActivity::class.java)
            R.id.menu_timeline -> Intent(this, TimelineActivity::class.java)
            else -> null
        }
        if(intent != null) {
            closeDrawer()
            runOnUi(delay = 200) {
                startActivity(intent)
                finish()
                overridePendingTransition(0, 0)
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                drawer_layout.openDrawer(GravityCompat.START)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            closeDrawer()
        } else {
            super.onBackPressed()
        }
    }

    private fun closeDrawer() {
        drawer_layout.closeDrawer(GravityCompat.START)
    }
}