package fs.resume.timeline

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.support.annotation.DrawableRes
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import fs.resume.R
import org.threeten.bp.LocalDate


data class TimelineEvent(
        val date : LocalDate,
        val title : String,
        @DrawableRes val icon : Int,
        val x : Float,
        val y : Float) {

    lateinit var bitmap : Bitmap
}


class TimelineView : View {

    private companion object {
        const val DEBUG = false
        const val TAG = "TimelineView"
        /** radius of event shape **/
        const val EVENT_SHAPE_RADIUS = 28f
        /** overscroll limit **/
        const val OVERSCROLL = 150f
        /** fling scaling factor (higherconst values reduce fling speed) **/
        const val FLING_SCALE = 1f
        /** radius of touch zone of event **/
        const val EVENT_TOUCH_RADIUS = EVENT_SHAPE_RADIUS + 10f
        /** radius from [xcur] to consider an event as selected **/
        const val EVENT_SELECTED_DISTANCE = 1f
        /** line width **/
        const val STROKE_LINE_WIDTH = 2f
        /** line highlight width **/
        const val STROKE_LINE_HIGHLIGHT_WIDTH = STROKE_LINE_WIDTH + 0.5f
        /** line width **/
        const val STROKE_CIRCLE_WIDTH = 3f
        /** line highlight width **/
        const val STROKE_CIRCLE_HIGHTLIGHT_WIDTH = STROKE_CIRCLE_WIDTH + 0.5f
        /** radius from [xcur] to highlight an event **/
        const val EVENT_HIGHLIGHT_DISTANCE = 20f
        /** image size **/
        const val IMAGE_SIZE = 32f; //48f;
        /** image size **/
        const val IMAGE_Y_TRANSLATION = 0f //2*EVENT_SHAPE_RADIUS + 15f
        /** delay before an item is marked as selected **/
        const val SELECTION_DELAY = 500L
    }

    /** time ordered events **/
    var events = emptyList<TimelineEvent>()
        set(values) {
            field = values
            // update world center
            setWorldX(values.lastOrNull()?.x ?: 0f)
            // load bitmaps
            values.forEach { it.bitmap = it.icon.loadBitmapOfSize( dp2px(IMAGE_SIZE).toInt() ) }
        }

    /** callback in case event was selected **/
    var onEventSelected : ((TimelineEvent) -> Unit)? = null

    /** define selected event as an event that is within certain distance from current position **/
    private val selectedEvent : TimelineEvent?
        get() = events.findClosestEventByX(xcur, EVENT_SELECTED_DISTANCE)

    /** current x position in world coordinates **/
    private var xcur = 50f

    /** minimum x position in world coordinates **/
    private val xmin : Float
        get() = if (events.size != 0) events[0].x else 0f

    /** maximum x position in world coordinates **/
    private val xmax : Float
        get() = if (events.size != 0) events[events.size-1].x else 0f


    private val rect = RectF()
    private val paint = Paint()

    /** detector for gestures (like scroll and flings) **/
    private lateinit var gestureDetector : GestureDetector
    /** calculate scroll position for flings, overscroll and spring-back **/
    private lateinit var scroller: OverScroller
    /** triggers updates while [scroller] is scrolling **/
    private lateinit var scrollAnimator : ValueAnimator

    /** handler to post selection events after event has been selected for [SELECTION_DELAY] **/
    private val selectionHandler = Handler(Looper.getMainLooper())

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs : AttributeSet?) {
        initScrolling()
    }


    protected override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val xcur = this.xcur

        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.STROKE
        paint.textSize = dp2px(10f)
        paint.flags = paint.flags or Paint.ANTI_ALIAS_FLAG

        // draw line segments
        paint.style = Paint.Style.STROKE
        for (index in (1..events.size - 1)) {
            val event1 = events[index - 1]
            val event2 = events[index]
            val px1 = worldX2screenX(event1.x)
            val py1 = worldY2screenY(event1.y)
            val px2 = worldX2screenX(event2.x)
            val py2 = worldY2screenY(event2.y)
            paint.strokeCap = Paint.Cap.BUTT
            paint.strokeWidth = dp2px(STROKE_LINE_WIDTH)
            paint.color = Color.BLACK
            canvas.drawLine(px1, py1, px2, py2, paint)

            // draw line highlight
            if (xcur.isWithin(event1.x, event2.x)) {
                // y = k * x + d
                val k = (py2 - py1) / (px2 - px1)
                val d = py1
                val tx1 = worldX2screenX(xcur - EVENT_HIGHLIGHT_DISTANCE/2f)
                val ty1 = (tx1 - px1) * k + d
                val tx2 = worldX2screenX(xcur + EVENT_HIGHLIGHT_DISTANCE/2f)
                val ty2 = (tx2 - px1) * k + d
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeWidth = dp2px(STROKE_LINE_HIGHLIGHT_WIDTH)
                paint.color = Color.YELLOW
                canvas.drawLine(tx1, ty1, tx2, ty2, paint)
            }
        }

        // draw event circles
        for (index in (0..events.size - 1)) {
            val event = events[index]
            val px = worldX2screenX(event.x)
            val py = worldY2screenY(event.y)
            val r = worldD2screenD(EVENT_SHAPE_RADIUS)

            paint.strokeCap = Paint.Cap.BUTT
            paint.strokeWidth = dp2px(STROKE_CIRCLE_WIDTH)
            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
            canvas.drawCircle(px, py, r, paint)
            paint.color = Color.BLACK
            paint.style = Paint.Style.STROKE
            canvas.drawCircle(px, py, r, paint)

            // draw event circle highlight
            if(xcur.isWithin(event.x - EVENT_SHAPE_RADIUS, event.x + EVENT_SHAPE_RADIUS)) {
                // calculate relative x within [-1,1]
                val rx = (xcur - event.x) / EVENT_SHAPE_RADIUS
                // calculate angle of entry and exit point and sweeps between them (in degree
                val entryAngle = calculateAngle(event, events.getOrNull(index-1), 180f)
                val exitAngle = calculateAngle(event, events.getOrNull(index+1), 0f)
                val sweep1 = (exitAngle - entryAngle).normalizeDegree()
                val sweep2 = 360f - sweep1

                paint.strokeCap = Paint.Cap.ROUND
                paint.color = Color.YELLOW
                paint.strokeWidth = dp2px(STROKE_CIRCLE_HIGHTLIGHT_WIDTH)
                rect.set(worldX2screenX(event.x - EVENT_SHAPE_RADIUS), worldY2screenY(event.y - EVENT_SHAPE_RADIUS),
                        worldX2screenX(event.x + EVENT_SHAPE_RADIUS), worldY2screenY(event.y + EVENT_SHAPE_RADIUS))
                if(rx <= 0) {
                    // rx is [-1,0), so we stretch p [0, 1] to fully close circle
                    val p = ((1f+rx) / 0.9f).limitTo(0f, 1f)
                    canvas.drawArc(rect, entryAngle, sweep1 * p, false, paint)
                    canvas.drawArc(rect, entryAngle, -sweep2 * p, false, paint)
                } else {
                    // rx is (0, 1], so we stretch p [1, 0] to fully close circle
                    val p = ((1f-rx) / 0.9f).limitTo(0f, 1f)
                    canvas.drawArc(rect, exitAngle, -sweep1 * p, false, paint)
                    canvas.drawArc(rect, exitAngle, sweep2 * p, false, paint)
                }
            }

            // draw labels
            paint.strokeCap = Paint.Cap.BUTT
            paint.color = Color.GRAY
            paint.style = Paint.Style.FILL
            paint.strokeWidth = dp2px(STROKE_CIRCLE_WIDTH)
            canvas.drawText(event.title, px, py + worldD2screenD(EVENT_SHAPE_RADIUS + 15f), paint )
            //canvas.drawBitmap(event.bitmap, px - event.bitmap.width/2, py - event.bitmap.height/2 + , paint)
            rect.set(event.bitmap)
                    .centerIn(0f, 0f)
                    .fitSize(worldD2screenD(IMAGE_SIZE))
                    .translate(px, py + worldD2screenD(IMAGE_Y_TRANSLATION))
            canvas.drawBitmap(event.bitmap, null, rect, paint)
        }
    }

    /** calculate angle between positions of two events **/
    private fun calculateAngle(event1 : TimelineEvent, event2 : TimelineEvent?, fallback : Float) : Float {
        return when {
            event2 == null -> fallback
            else -> atan2(event2.y - event1.y, event2.x - event1.x) * (180f / Math.PI).toFloat()
        }.normalizeDegree()
    }

    /** set world x coordinate **/
    private fun setWorldX(x : Float) {
        logd() { "setWorldX(): $x" }
        val wasSelected = selectedEvent != null
        xcur = x.limitTo(xmin - OVERSCROLL, xmax + OVERSCROLL)
        val isSelected = selectedEvent != null
        invalidate()

        if(!wasSelected && isSelected) {
            val event = selectedEvent!!
            selectionHandler.postDelayed({ onEventSelected?.invoke(event) }, SELECTION_DELAY)
        } else if(!isSelected) {
            selectionHandler.removeCallbacksAndMessages(null)
        }
    }

    /** find event by world coordinates within given radius. If multiple events are within distance
     *  then the closest event is returned */
    private fun Collection<TimelineEvent>.findClosestEventByXY(x : Float, y : Float, dist : Float = Float.MAX_VALUE) : TimelineEvent? {
        fun square(x : Float) = x*x
        fun dist2d(event: TimelineEvent) = Math.sqrt(
                (square(x - event.x) + square(y - event.y)).toDouble())
        return filter { dist2d(it) <= dist }. minBy { dist2d(it) }
    }

    /** find event by world coordinates within given distance. If multiple events are within distance
     *  then the closest event is returned */
    private fun Collection<TimelineEvent>.findClosestEventByX(x : Float, dist : Float = Float.MAX_VALUE) : TimelineEvent? {
        val dist1d = { event : TimelineEvent -> Math.abs(event.x - x) }
        return filter { dist1d(it) <= dist }. minBy { dist1d(it) }
    }

    // region -- SCROLLING --

    /** setup scrolling **/
    private fun initScrolling() {
        // init scrolling
        gestureDetector = GestureDetector(context, gestureListener)
        scroller = OverScroller(context);
        scrollAnimator = ValueAnimator.ofFloat(0f, 1f)
        scrollAnimator.repeatCount = ValueAnimator.INFINITE
        scrollAnimator.setDuration(1000L)
        scrollAnimator.addUpdateListener { onScrollAnimationStep() }
    }

    /** event handler for touch events that forwards to gesture detector **/
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // handle scrolling event
        var result = gestureDetector.onTouchEvent(event)
        // in case event was not handled
        if (!result) {
            // in case user released and no fling in progress: initiate springBack in case of drag-overscroll
            logd() {"onTouchEvent(): action=: ${event.action}, scroller=${scroller.isFinished}" }
            if (event.action == MotionEvent.ACTION_UP && scroller.isFinished) {
                events.scrollToNearest()
            }
        }
        return result
    }

    /** gesture listener to analyze scroll events and flings **/
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        private var initialOffset: Float = 0f
        private var accumulatedDistance: Float = 0f

        override fun onDown(e: MotionEvent): Boolean {
            stopScrolling()
            initialOffset = xcur
            accumulatedDistance = 0f
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val x = screenX2worldX(e.x)
            val y = screenY2worldY(e.y)
            val event = events.findClosestEventByXY(x, y, EVENT_TOUCH_RADIUS)
            logd() { "Click: x=$x, y=$y, event=$event" }
            event?.scrollTo()
            return event != null
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            // NOTE: we calculate in pixel
            accumulatedDistance += screenD2worldD(distanceX)
            val x = initialOffset + accumulatedDistance

            logd() {"onScroll(): initialOffset=$initialOffset,  accumulatedDistance=$accumulatedDistance -> x=$x" }
            setWorldX(x)
            return false
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            // NOTE: we calculate in pixel
            val cur = worldD2screenDI(xcur)
            val min = worldD2screenDI(xmin)
            val max = worldD2screenDI(xmax)
            val velocity = (-velocityX / dp2px(FLING_SCALE)).toInt()
            val overScroll = dp2px(OVERSCROLL).toInt()
            stopScrolling()
            logd() {"onFling(): velocity=$velocityX, cur=$cur, min=$min, max=$max, overScroll=$overScroll" }
            scroller.fling(
                    cur, 0, // current position
                    velocity, 0, // fling
                    min, max, // limits x
                    0, 0, // limits y
                    overScroll, 0) // over-limits
            scrollAnimator.start()
            return true
        }
    }

    /** scroll to nearest event **/
    private fun Collection<TimelineEvent>.scrollToNearest() {
        findClosestEventByX(xcur)?.scrollTo()
    }

    /** scroll to this event **/
    private fun TimelineEvent.scrollTo() {
        // NOTE: we calculate in pixel
        val cur = worldD2screenDI(xcur)
        val min = worldD2screenDI(this.x)
        val max = worldD2screenDI(this.x)
        logd() { "scrollToNearestEvent(): cur=$cur, min=$min, max=$max" }
        if (scroller.springBack(
                cur, 0, // current
                min, max, // limits x
                0, 0)) {
            // limit y
            logd() {"springBack started"}
            scrollAnimator.start()
        }
    }

    /** called by scroll animator during fling or springBack  */
    private fun onScrollAnimationStep() {
        if (!scroller.isFinished) {
            scroller.computeScrollOffset()
            logd() { "onScrollAnimationStep(): current=${scroller.currX}, start=${scroller.startX}, end=${scroller.finalX}, velocity=${scroller.currVelocity}" }
            setWorldX(screenD2worldD(scroller.currX))
        } else {
            scrollAnimator.cancel()
            if(selectedEvent == null) {
                events.scrollToNearest()
            }
        }
    }

    /** stop all scrolling animations  */
    private fun stopScrolling() {
        scroller.abortAnimation()
        scrollAnimator.cancel()
    }

    //region -- COORDINATE FUNCTIONS --

    private fun dp2px(dp : Float): Float = dp * Resources.getSystem().displayMetrics.density

    private fun px2dp(px : Float): Float = px / Resources.getSystem().displayMetrics.density

    private fun px2dp(px : Int): Float =  px2dp(px.toFloat())

    private fun worldX2screenXI(worldX: Float): Int = worldX2screenX(worldX).toInt()

    private fun worldX2screenX(worldX: Float): Float = width/2f + worldD2screenD(worldX - xcur)

    private fun worldY2screenYI(worldY: Float): Int = worldY2screenYI(worldY).toInt()

    private fun worldY2screenY(worldY: Float): Float = height/2f + worldD2screenD(worldY)

    private fun screenX2worldX(screenX: Float): Float = xcur + screenD2worldD(screenX - width/2f)

    private fun screenY2worldY(screenY: Float): Float = screenD2worldD(screenY - height/2f)

    private fun worldD2screenDI(worldDistance: Float): Int = worldD2screenD(worldDistance).toInt()

    private fun worldD2screenD(worldDistance: Float): Float = worldDistance * dp2px(1f)

    private fun screenD2worldD(screenDist: Float): Float = screenDist / dp2px(1f)

    private fun screenD2worldD(screenDist: Int): Float = screenD2worldD(screenDist.toFloat())

    //region -- BITMAP --

    private fun Int.loadBitmapOfSize(size : Int) : Bitmap {
        // decode only bounds
        val opts = BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.resources, this, opts);
        // calculate scaling factor
        var scaleFactor : Int;
        if (opts.outHeight > opts.outWidth) {
            //if image height is greater than width
            scaleFactor = Math.round(opts.outHeight / size.toFloat());
        } else {
            //if image width is greater than height
            scaleFactor = Math.round(opts.outWidth / size.toFloat());
        }
        // decode with inSampleSize
        val opts2 = BitmapFactory.Options();
        opts2.inSampleSize = scaleFactor;
        return BitmapFactory.decodeResource(context.resources, this, opts2);
    }

    // region -- RECTANGLE --

    private fun RectF.set(bitmap : Bitmap) : RectF {
        set(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        return this
    }

    private fun RectF.translate(dx : Float, dy : Float) : RectF {
        offset(dx, dy)
        return this
    }

    private fun RectF.centerIn(x : Float, y : Float) : RectF {
        return translate(x - width()/2f, y - height()/2f)
    }

    private fun RectF.fitSize(size : Float) : RectF {
        val scale = when {
            height() > width() -> size / height()
            else -> size / width()
        }
        return scaleCenter(scale)
    }

    private fun RectF.scaleCenter(scale : Float) : RectF {
        val w2 = width()*(scale - 1f)/2f
        val h2 = height()*(scale - 1f)/2f
        left -= w2
        top -= h2
        right += w2
        bottom += h2
        return this
    }

    // --- MATH

    private fun Float.normalizeDegree() = (this + 360f) % 360f

    private fun atan2(y : Float, x : Float) = Math.atan2(y.toDouble(), x.toDouble()).toFloat()

    private fun Float.limitTo(min : Float, max : Float) = Math.min(max, Math.max(min, this))

    private fun Float.abs() = Math.abs(this)

    private fun Float.isWithin(min : Float, max : Float) = this >= min && this <= max

    // --- LOG

    private inline fun logd(message : () -> String) : Unit {
        if(DEBUG) Log.d(TAG, message())
    }
}