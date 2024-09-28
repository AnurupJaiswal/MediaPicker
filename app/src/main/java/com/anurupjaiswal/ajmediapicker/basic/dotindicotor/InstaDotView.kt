package com.anurupjaiswal.ajmediapicker.basic.dotindicotor


/**
 * InstaDotView.kt
 *
 * Created by: Anurup Jaiswal
 * Created on: 27th August 2024
 * Purpose: This custom view represents a dot indicator for showcasing the
 * current position in a media carousel or slider. It provides visual feedback
 * to the user regarding their current selection.
 */

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.anurupjaiswal.ajmediapicker.R




class InstaDotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes) {

    // Constants defining minimum and default visible dot count
    private val MIN_VISIBLE_DOT_COUNT = 6
    private val DEFAULT_VISIBLE_DOTS_COUNT = MIN_VISIBLE_DOT_COUNT

    // Dot sizes and margin
    private var activeDotSize: Int = 0
    private var inactiveDotSize: Int = 0
    private var mediumDotSize: Int = 0
    private var smallDotSize: Int = 0
    private var dotMargin: Int = 0

    // Paint objects for active and inactive dots
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    // Dot positions and animation variables
    private var startPosX: Int = 0
    private var posY: Int = 0
    private var previousPage: Int = 0
    private var currentPage: Int = 0

    private var translationAnim: ValueAnimator? = null

    // List to store dot states
    private var dotsList: MutableList<Dot> = mutableListOf()

    // Number of pages and visible dot count
    private var noOfPages: Int = 0
    private var visibleDotCounts: Int = DEFAULT_VISIBLE_DOTS_COUNT

    init {
        setup(context, attrs) // Initialize view attributes
    }

    private fun setup(context: Context, attributeSet: AttributeSet?) {
        val resources: Resources = resources

        attributeSet?.let {
            val ta: TypedArray = context.obtainStyledAttributes(it, R.styleable.InstaDotView)
            activePaint.color = ta.getColor(
                R.styleable.InstaDotView_dot_activeColor, resources.getColor(
                    R.color.active))
            inactivePaint.color = ta.getColor(
                R.styleable.InstaDotView_dot_inactiveColor, resources.getColor(
                    R.color.inactive))
            activeDotSize = ta.getDimensionPixelSize(
                R.styleable.InstaDotView_dot_activeSize, resources.getDimensionPixelSize(
                    R.dimen.dot_active_size))
            inactiveDotSize = ta.getDimensionPixelSize(
                R.styleable.InstaDotView_dot_inactiveSize, resources.getDimensionPixelSize(
                    R.dimen.dot_inactive_size))
            mediumDotSize = ta.getDimensionPixelSize(
                R.styleable.InstaDotView_dot_mediumSize, resources.getDimensionPixelSize(
                    R.dimen.dot_medium_size))
            smallDotSize = ta.getDimensionPixelSize(
                R.styleable.InstaDotView_dot_smallSize, resources.getDimensionPixelSize(
                    R.dimen.dot_small_size))
            dotMargin = ta.getDimensionPixelSize(
                R.styleable.InstaDotView_dot_margin, resources.getDimensionPixelSize(
                    R.dimen.dot_margin))
            setVisibleDotCounts(ta.getInteger(R.styleable.InstaDotView_dots_visible, DEFAULT_VISIBLE_DOTS_COUNT))

            ta.recycle()
        }

        posY = activeDotSize / 2

        initCircles()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (activeDotSize + dotMargin) * (dotsList.size + 1)
        val desiredHeight = activeDotSize

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width: Int = when {
            widthMode == MeasureSpec.EXACTLY -> widthSize
            widthMode == MeasureSpec.AT_MOST -> minOf(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val height: Int = when {
            heightMode == MeasureSpec.EXACTLY -> heightSize
            heightMode == MeasureSpec.AT_MOST -> minOf(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCircles(canvas)
    }

    private fun initCircles() {
        val viewCount = minOf(noOfPages, visibleDotCounts)
        if (viewCount < 1) return

        setStartPosX(if (noOfPages > visibleDotCounts) getSmallDotStartX() else 0)

        dotsList = MutableList(viewCount) { Dot() }
        for (i in 0 until viewCount) {
            val dot = dotsList[i]
            val state: Dot.State = when {
                noOfPages > visibleDotCounts -> when (i) {
                    visibleDotCounts - 1 -> Dot.State.SMALL
                    visibleDotCounts - 2 -> Dot.State.MEDIUM
                    0 -> Dot.State.ACTIVE
                    else -> Dot.State.INACTIVE
                }
                else -> if (i == 0) Dot.State.ACTIVE else Dot.State.INACTIVE
            }

            dot.state = state
        }

        invalidate()
    }

    /**
     * Draws the circles (dots) on the canvas.
     *
     * @param canvas the canvas on which to draw the dots
     */
    private fun drawCircles(canvas: Canvas) {
        var posX = startPosX

        for (dot in dotsList) {
            val paint: Paint
            val radius: Int

            // Determine the paint and radius for each dot based on state
            when (dot.state) {
                Dot.State.ACTIVE -> {
                    paint = activePaint
                    radius = getActiveDotRadius()
                    posX += getActiveDotStartX()
                }
                Dot.State.INACTIVE -> {
                    paint = inactivePaint
                    radius = getInactiveDotRadius()
                    posX += getInactiveDotStartX()
                }
                Dot.State.MEDIUM -> {
                    paint = inactivePaint
                    radius = getMediumDotRadius()
                    posX += getMediumDotStartX()
                }
                Dot.State.SMALL -> {
                    paint = inactivePaint
                    radius = getSmallDotRadius()
                    posX += getSmallDotStartX()
                }
            }

            canvas.drawCircle(posX.toFloat(), posY.toFloat(), radius.toFloat(), paint)
        }
    }

    private fun getTranslationAnimation(from: Int, to: Int, listener: AnimationListener?): ValueAnimator {
        translationAnim?.cancel()
        translationAnim = ValueAnimator.ofInt(from, to).apply {
            duration = 120
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { valueAnimator ->
                val valAnim = valueAnimator.animatedValue as Int
                if (startPosX != valAnim) {
                    setStartPosX(valAnim)
                    invalidate()
                }
            }
            addListener(object :
                AnimatorListener() {
                override fun onAnimationEnd(animator: Animator) {
                    listener?.onAnimationEnd()
                }
            })
        }
        return translationAnim!!
    }

    // Set the number of pages and update visibility
    fun setNoOfPages(noOfPages: Int) {
        visibility = if (noOfPages <= 1) GONE else VISIBLE
        this.noOfPages = noOfPages
        recreate()
    }

    fun getNoOfPages(): Int = noOfPages

    fun setVisibleDotCounts(visibleDotCounts: Int) {
        if (visibleDotCounts < MIN_VISIBLE_DOT_COUNT)
            throw RuntimeException("Visible Dot count cannot be smaller than $MIN_VISIBLE_DOT_COUNT")
        this.visibleDotCounts = visibleDotCounts
        recreate()
    }

    private fun recreate() {
        initCircles()
        requestLayout()
        invalidate()
    }

    fun getVisibleDotCounts(): Int = visibleDotCounts

    fun setStartPosX(startPosX: Int) {
        this.startPosX = startPosX
    }

    fun getStartPosX(): Int = startPosX

    private fun getActiveDotStartX(): Int = activeDotSize + dotMargin

    private fun getInactiveDotStartX(): Int = inactiveDotSize + dotMargin

    private fun getMediumDotStartX(): Int = mediumDotSize + dotMargin

    private fun getSmallDotStartX(): Int = smallDotSize + dotMargin

    private fun getActiveDotRadius(): Int = activeDotSize / 2

    private fun getInactiveDotRadius(): Int = inactiveDotSize / 2

    private fun getMediumDotRadius(): Int = mediumDotSize / 2

    private fun getSmallDotRadius(): Int = smallDotSize / 2

    fun onPageChange(page: Int) {
        currentPage = page
        if (page != previousPage && page in 0 until noOfPages) {
            updateDots()
            previousPage = currentPage
        }
    }

    private fun updateDots() {
        if (noOfPages <= visibleDotCounts) {
            setupNormalDots()
            return
        }

        for (i in dotsList.indices) {
            val currentDot = dotsList[i]
            if (currentDot.state == Dot.State.ACTIVE) {
                currentDot.state = Dot.State.INACTIVE
                if (currentPage > previousPage) {
                    setupFlexibleCirclesRight(i)
                } else {
                    setupFlexibleCirclesLeft(i)
                }
                return
            }
        }
    }

    private fun setupNormalDots() {
        dotsList[currentPage].state = Dot.State.ACTIVE
        dotsList[previousPage].state = Dot.State.INACTIVE
        invalidate()
    }

    private fun setupFlexibleCirclesRight(position: Int) {
        if (position >= visibleDotCounts - 3) {
            when (currentPage) {
                noOfPages - 1 -> {
                    dotsList[dotsList.size - 1].state = Dot.State.ACTIVE
                    invalidate()
                }
                noOfPages - 2 -> {
                    dotsList[dotsList.size - 1].state = Dot.State.MEDIUM
                    dotsList[dotsList.size - 2].state = Dot.State.ACTIVE
                    invalidate()
                }
                else -> removeAddRight(position)
            }
        } else {
            dotsList[position + 1].state = Dot.State.ACTIVE
            invalidate()
        }
    }

    private fun removeAddRight(position: Int) {
        dotsList.removeAt(0)
        setStartPosX(getStartPosX() + getSmallDotStartX())

        getTranslationAnimation(getStartPosX(), getSmallDotStartX()) {
            dotsList[0].state = Dot.State.SMALL
            dotsList[1].state = Dot.State.MEDIUM

            val newDot = Dot().apply { state = Dot.State.ACTIVE }
            dotsList.add(position, newDot)
            invalidate()
        }.start()
    }

    private fun setupFlexibleCirclesLeft(position: Int) {
        if (position <= 2) {
            when (currentPage) {
                0 -> {
                    dotsList[0].state = Dot.State.ACTIVE
                    invalidate()
                }
                1 -> {
                    dotsList[0].state = Dot.State.MEDIUM
                    dotsList[1].state = Dot.State.ACTIVE
                    invalidate()
                }
                else -> removeAddLeft(position)
            }
        } else {
            dotsList[position - 1].state = Dot.State.ACTIVE
            invalidate()
        }
    }



    private fun removeAddLeft(position: Int) {
        dotsList.removeAt(dotsList.size - 1)
        setStartPosX(0)

        getTranslationAnimation(getStartPosX(), getSmallDotStartX()) {
            dotsList[dotsList.size - 1].state = Dot.State.SMALL
            dotsList[dotsList.size - 2].state = Dot.State.MEDIUM

            val newDot = Dot().apply { state = Dot.State.ACTIVE }
            dotsList.add(position, newDot)
            invalidate()
        }.start()
    }
}