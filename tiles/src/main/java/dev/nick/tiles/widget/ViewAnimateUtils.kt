package dev.nick.tiles.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import androidx.annotation.IdRes
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView

import java.util.ArrayList

object ViewAnimateUtils {

    val COLOR_PROPERTY = "color"
    val DURATION_SHORT = 300
    val DURATION_MID = 800

    fun animateColorChange(view: View, fromColor: Int, toColor: Int, duration: Int,
                           listener: Animator.AnimatorListener?) {
        if (view.windowToken == null) {
            return
        }
        val animation = AnimatorSet()
        val colorAnimator = ObjectAnimator.ofInt(view, COLOR_PROPERTY, fromColor, toColor)
        colorAnimator.setEvaluator(ArgbEvaluator())
        colorAnimator.duration = duration.toLong()
        if (listener != null)
            animation.addListener(listener)
        animation.play(colorAnimator)
        animation.start()
    }

    fun circularHide(view: View, listener: Animator.AnimatorListener) {
        val anim = createCircularHideAnimator(view, listener)
        anim?.start()
    }

    fun createCircularHideAnimator(view: View,
                                   listener: Animator.AnimatorListener?): Animator? {
        if (view.windowToken == null || view.visibility == View.INVISIBLE)
            return null

        // get the center for the clipping circle
        val cx = (view.left + view.right) / 2
        val cy = (view.top + view.bottom) / 2

        // get the initial radius for the clipping circle
        val initialRadius = view.width

        // create the animation (the final radius is zero)
        val anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius.toFloat(), 0f)

        anim.addListener(object : AnimatorListenerAdapter() {

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                view.visibility = View.INVISIBLE
            }
        })

        if (listener != null) {
            anim.addListener(listener)
        }
        return anim
    }

    fun circularShow(view: View) {
        val anim = createCircularShowAnimator(view)
        anim?.start()
    }

    fun createCircularShowAnimator(view: View): Animator? {
        if (view.visibility == View.VISIBLE || view.windowToken == null)
            return null
        // get the center for the clipping circle
        val cx = (view.left + view.right) / 2
        val cy = (view.top + view.bottom) / 2

        // get the final radius for the clipping circle
        val finalRadius = view.width

        // create and start the animator for this view
        // (the start radius is zero)
        val anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0f, finalRadius.toFloat())
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                view.visibility = View.VISIBLE
            }
        })
        return anim
    }

    fun alphaShow(view: View) {
        if (view.windowToken == null)
            return
        view.visibility = View.VISIBLE
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        alpha.duration = DURATION_MID.toLong()
        alpha.start()
    }

    fun alphaHide(view: View, rWhenDone: Runnable?) {
        if (view.windowToken == null) {
            rWhenDone?.run()
            return
        }
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        alpha.duration = DURATION_MID.toLong()
        alpha.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                view.visibility = View.INVISIBLE
                rWhenDone?.run()
            }

            override fun onAnimationCancel(animation: Animator) {
                view.visibility = View.INVISIBLE
                rWhenDone?.run()
            }

            override fun onAnimationRepeat(animation: Animator) {

            }
        })
        alpha.start()
    }

    fun animateTextChange(view: TextView, @IdRes toText: Int,
                          rWhenEnd: Runnable?) {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        val restore = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        alpha.duration = DURATION_SHORT.toLong()
        alpha.interpolator = AccelerateDecelerateInterpolator()
        restore.duration = DURATION_SHORT.toLong()
        restore.interpolator = AccelerateDecelerateInterpolator()
        alpha.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                // Do nothing.
            }

            override fun onAnimationEnd(animation: Animator) {
                view.setText(toText)
                restore.start()
            }

            override fun onAnimationCancel(animation: Animator) {
                view.setText(toText)
            }

            override fun onAnimationRepeat(animation: Animator) {
                // Do nothing.
            }
        })
        if (rWhenEnd != null)
            restore.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {

                }

                override fun onAnimationEnd(animation: Animator) {
                    rWhenEnd.run()
                }

                override fun onAnimationCancel(animation: Animator) {
                    rWhenEnd.run()
                }

                override fun onAnimationRepeat(animation: Animator) {

                }
            })
        alpha.start()
    }

    fun animateTextChange(view: TextView, toText: String) {
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        val restore = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        alpha.duration = DURATION_SHORT.toLong()
        alpha.interpolator = AccelerateDecelerateInterpolator()
        restore.duration = DURATION_SHORT.toLong()
        restore.interpolator = AccelerateDecelerateInterpolator()
        alpha.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                // Do nothing.
            }

            override fun onAnimationEnd(animation: Animator) {
                view.text = toText
                restore.start()
            }

            override fun onAnimationCancel(animation: Animator) {
                view.text = toText
            }

            override fun onAnimationRepeat(animation: Animator) {
                // Do nothing.
            }
        })
        alpha.start()
    }

    @JvmOverloads
    fun scaleShow(view: View, rWhenEnd: Runnable? = null) {
        if (view.visibility == View.VISIBLE) {
            view.visibility = View.VISIBLE
            rWhenEnd?.run()
            return
        }
        if (view.windowToken == null) {
            view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    scaleShow(view)
                }

                override fun onViewDetachedFromWindow(v: View) {

                }
            })
        }
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f)
        val set = AnimatorSet()
        set.playTogether(scaleX, scaleY)
        set.duration = DURATION_SHORT.toLong()
        set.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                view.visibility = View.VISIBLE
                rWhenEnd?.run()
            }

            override fun onAnimationCancel(animation: Animator) {
                view.visibility = View.VISIBLE
                rWhenEnd?.run()
            }

            override fun onAnimationRepeat(animation: Animator) {

            }
        })
        set.start()
    }

    @JvmOverloads
    fun scaleHide(view: View, rWhenEnd: Runnable? = null) {
        if (view.windowToken == null) {
            view.visibility = View.INVISIBLE
            rWhenEnd?.run()
            return
        }
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0f)
        val set = AnimatorSet()
        set.playTogether(scaleX, scaleY)
        set.duration = DURATION_SHORT.toLong()
        set.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                view.visibility = View.INVISIBLE
                rWhenEnd?.run()
            }

            override fun onAnimationCancel(animation: Animator) {
                view.visibility = View.INVISIBLE
                rWhenEnd?.run()
            }

            override fun onAnimationRepeat(animation: Animator) {

            }
        })
        set.start()
    }

    fun runFlipHorizonAnimation(view: View, duration: Long, rWhenEnd: Runnable?) {
        view.alpha = 0f
        val set = AnimatorSet()
        val objectAnimator1 = ObjectAnimator.ofFloat(view,
                "rotationY", -180f, 0f)
        val objectAnimator2 = ObjectAnimator.ofFloat(view, "alpha",
                0f, 1f)
        set.duration = duration
        set.playTogether(objectAnimator1, objectAnimator2)
        if (rWhenEnd != null)
            set.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {

                }

                override fun onAnimationEnd(animation: Animator) {
                    rWhenEnd.run()
                }

                override fun onAnimationCancel(animation: Animator) {

                }

                override fun onAnimationRepeat(animation: Animator) {

                }
            })
        set.start()
    }

    /**
     * Given a coordinate relative to the descendant, find the coordinate in a parent view's
     * coordinates.
     *
     * @param descendant        The descendant to which the passed coordinate is relative.
     * @param root              The root view to make the coordinates relative to.
     * @param coord             The coordinate that we want mapped.
     * @param includeRootScroll Whether or not to account for the scroll of the descendant:
     * sometimes this is relevant as in a child's coordinates within the descendant.
     * @return The factor by which this descendant is scaled relative to this DragLayer. Caution
     * this scale factor is assumed to be equal in X and Y, and so if at any point this
     * assumption fails, we will need to return a pair of scale factors.
     */
    fun getDescendantCoordRelativeToParent(descendant: View, root: View,
                                           coord: IntArray, includeRootScroll: Boolean): Float {
        val ancestorChain = ArrayList<View>()

        val pt = floatArrayOf(coord[0].toFloat(), coord[1].toFloat())

        var v: View? = descendant
        while (v !== root && v != null) {
            ancestorChain.add(v)
            v = v.parent as View
        }
        ancestorChain.add(root)

        var scale = 1.0f
        val count = ancestorChain.size
        for (i in 0 until count) {
            val v0 = ancestorChain[i]
            // For TextViews, scroll has a meaning which relates to the text position
            // which is very strange... ignore the scroll.
            if (v0 !== descendant || includeRootScroll) {
                pt[0] -= v0.scrollX.toFloat()
                pt[1] -= v0.scrollY.toFloat()
            }

            v0.matrix.mapPoints(pt)
            pt[0] += v0.left.toFloat()
            pt[1] += v0.top.toFloat()
            scale *= v0.scaleX
        }

        coord[0] = Math.round(pt[0])
        coord[1] = Math.round(pt[1])
        return scale
    }

    fun getColorWithAlpha(alpha: Float, baseColor: Int): Int {
        val a = Math.min(255, Math.max(0, (alpha * 255).toInt())) shl 24
        val rgb = 0x00ffffff and baseColor
        return a + rgb
    }
}
