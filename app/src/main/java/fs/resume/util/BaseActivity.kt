package fs.resume.util

import fs.resume.R
import android.support.annotation.LayoutRes
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.ViewStub
import kotlinx.android.synthetic.main.base_activity.*
import kotlinx.android.synthetic.main.base_drawer_activity.*

abstract class BaseActivity : AppCompatActivity() {

    @LayoutRes
    open val baseLayout = R.layout.base_activity

    override fun setContentView(@LayoutRes layoutResID: Int) {
        super.setContentView(baseLayout)
        (content_stub as ViewStub).apply {
            layoutResource = layoutResID
            inflate()
        }
        setSupportActionBar(findViewById(R.id.toolbar) as Toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }
}
