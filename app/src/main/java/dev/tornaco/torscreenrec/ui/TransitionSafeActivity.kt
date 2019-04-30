package dev.tornaco.torscreenrec.ui

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import android.view.View

import dev.tornaco.torscreenrec.R

open class TransitionSafeActivity : AppCompatActivity() {

    protected var mShowingFragment: Fragment? = null

    var isDestroyedCompat: Boolean = false
        private set

    @JvmOverloads
    protected fun setupToolbar(resId: Int = R.id.toolbar) {
        val toolbar = findView<Toolbar>(resId)
        setSupportActionBar(toolbar)
    }

    protected fun showHomeAsUp() {
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    protected fun <T : View> findView(@IdRes resId: Int): T {
        return findViewById<View>(resId) as T
    }

    protected fun <T : View> findView(root: View, @IdRes resId: Int): T {
        return root.findViewById<View>(resId) as T
    }

    /**
     * Show fragment page by replaceV4 the given containerId, if you have data to set
     * give a bundle.
     *
     * @param containerId The id to replaceV4.
     * @param fragment    The fragment to show.
     * @param bundle      The data of the fragment if it has.
     */
    protected fun replaceV4(containerId: Int,
                            fragment: Fragment, bundle: Bundle): Boolean {
        return replaceV4(containerId, fragment, bundle, true)
    }

    /**
     * Show fragment page by replaceV4 the given containerId, if you have data to set
     * give a bundle.
     *
     * @param containerId The id to replaceV4.
     * @param f           The fragment to show.
     * @param bundle      The data of the fragment if it has.
     * @param animate     True if you want to animate the fragment.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected fun replaceV4(containerId: Int,
                            f: Fragment?, bundle: Bundle?, animate: Boolean): Boolean {

        if (isDestroyedCompat || f == null) {
            return false
        }

        if (bundle != null) {
            f.arguments = bundle
        }

        if (!animate) {
            supportFragmentManager.beginTransaction()
                    .replace(containerId, f).commit()
        } else {
            supportFragmentManager.beginTransaction()
                    .replace(containerId, f)
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .commit()
        }
        mShowingFragment = f
        return true
    }

    /**
     * Remove a fragment that is attached, with animation.
     *
     * @param f The fragment to removeV4.
     * @return True if successfully removed.
     * @see .removeV4
     */
    protected fun removeV4(f: Fragment): Boolean {
        return removeV4(f, true)
    }

    /**
     * Remove a fragment that is attached.
     *
     * @param f       The fragment to removeV4.
     * @param animate True if you want to animate the fragment.
     * @return True if successfully removed.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected fun removeV4(f: Fragment?, animate: Boolean): Boolean {

        if (!isDestroyed || f == null) {
            return false
        }

        if (!animate) {
            supportFragmentManager.beginTransaction().remove(f).commitAllowingStateLoss()
        } else {
            supportFragmentManager.beginTransaction()
                    .remove(f)
                    .commitAllowingStateLoss()//TODO Ignore the result?
        }
        mShowingFragment = null
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            if (!interruptHomeOption()) {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    protected fun interruptHomeOption(): Boolean {
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        isDestroyedCompat = true
    }
}
