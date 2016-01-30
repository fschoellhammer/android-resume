package fs.resume.timeline

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.support.annotation.DrawableRes
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import fs.resume.R
import org.threeten.bp.LocalDate
import java.io.FileInputStream


data class TimelineEvent(val date : LocalDate, val title : String, @DrawableRes val icon : Int,
        val x : Float, val y : Float) {

    lateinit var bitmap : Bitmap
}


class TimelineView : View {

    private companion object {
        private val TAG = "TimelineView"
        /** radius of event shape **/
        private val EVENT_SHAPE_RADIUS = 20f
        /** overscroll limit **/
        private val OVERSCROLL = 150f
        /** fling scaling factor (higher values reduce fling speed) **/
        private val FLING_SCALE = 1f
        /** radius of touch zone of event **/
        private val EVENT_TOUCH_RADIUS = 25f
        /** radius from [xcur] to consider an event as selected **/
        private val EVENT_SELECTED_DISTANCE = 1f
        /** line width **/
        private val LINE_WIDTH = 2f
        /** line highlight width **/
        private val LINE_HIGHLIGHT_WIDTH = 2.5f
        /** radius from [xcur] to highlight an event **/
        private val EVENT_HIGHLIGHT_DISTANCE = 20f
        /** image size **/
        private val IMAGE_SIZE = 48f;
        /** image size **/
        private val IMAGE_Y_TRANSLATION = 2*EVENT_SHAPE_RADIUS + 15f;
    }

    private val paint = Paint()

    /** time ordered events **/
    private val events = listOf(
            TimelineEvent(LocalDate.of(2003, 1, 1), "Uni Wien", R.drawable.ic_uni, 0f, 80f),
            TimelineEvent(LocalDate.of(2004, 2, 1), "TU Wien", R.drawable.ic_tu, 100f, 30f),
            TimelineEvent(LocalDate.of(2005, 9, 1), "Waseda", R.drawable.ic_waseda, 180f, 40f),
            TimelineEvent(LocalDate.of(2008, 7, 1), "China", R.drawable.ic_southeast, 300f, 100f),
            TimelineEvent(LocalDate.of(2011, 6, 1), "S-Can", R.drawable.ic_scan, 380f, 70f),
            TimelineEvent(LocalDate.of(2013, 1, 1), "Rakuten", R.drawable.ic_rakuten, 520f, 40f),
            TimelineEvent(LocalDate.of(2015, 2, 1), "Solarier", R.drawable.ic_solarier, 640f, 10f)
    )

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

    private lateinit var gestureDetector : GestureDetector
    private lateinit var scroller: OverScroller
    private lateinit var scrollAnimator : ValueAnimator


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
        events.scrollToNearest()

        // load bitmaps
        events.forEach { it.bitmap = it.icon.loadBitmapOfSize( dp2px(IMAGE_SIZE).toInt() ) }
    }


    //region -- DRAW --

//    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
//        super.onLayout(changed, left, top, right, bottom)
//        setTimeRange(mCurrentTimeRangePerCm, true)
//    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val xcur = this.xcur

        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.STROKE
        paint.textSize = dp2px(12f)
        paint.flags = paint.flags or Paint.ANTI_ALIAS_FLAG

        // draw line segments
        paint.style = Paint.Style.STROKE
        for (index in (1..events.size - 1)) {
            val event1 = events[index - 1]
            val event2 = events[index]
            val px1 = worldX2screenX(event1.x).toFloat()
            val py1 = worldY2screenY(event1.y).toFloat()
            val px2 = worldX2screenX(event2.x).toFloat()
            val py2 = worldY2screenY(event2.y).toFloat()
            paint.strokeWidth = dp2px(LINE_WIDTH)
            paint.color = Color.BLACK
            canvas.drawLine(px1, py1, px2, py2, paint)

            // draw line highlight
            if (xcur >= event1.x && xcur <= event2.x) {
                val k = (py2 - py1) / (px2 - px1)
                val d = py1
                val tx1 = worldX2screenX(xcur - EVENT_HIGHLIGHT_DISTANCE/2f).toFloat()
                val ty1 = (tx1 - px1) * k + d
                val tx2 = worldX2screenX(xcur + EVENT_HIGHLIGHT_DISTANCE/2f).toFloat()
                val ty2 = (tx2 - px1) * k + d
                paint.strokeWidth = dp2px(LINE_HIGHLIGHT_WIDTH)
                paint.color = Color.YELLOW
                canvas.drawLine(tx1, ty1, tx2, ty2, paint)
            }
        }

        // draw event circles
        paint.strokeWidth = dp2px(LINE_WIDTH)
        for (index in (0..events.size - 1)) {
            val event = events[index]
            val px = worldX2screenX(event.x).toFloat()
            val py = worldY2screenY(event.y).toFloat()
            val r = worldD2screenD(EVENT_SHAPE_RADIUS).toFloat()

            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
            canvas.drawCircle(px, py, r, paint)
            paint.color = Color.BLACK
            paint.style = Paint.Style.STROKE
            canvas.drawCircle(px, py, r, paint)

            // draw event circle highlight
            if(xcur >= event.x - EVENT_SHAPE_RADIUS && xcur <= event.x + EVENT_SHAPE_RADIUS) {
                // fixme: would be much cooler to draw actual circle arcs fron entry and exit point
                val dx = xcur - event.x
                paint.strokeWidth = dp2px(LINE_HIGHLIGHT_WIDTH)
                paint.color = Color.YELLOW
                canvas.save()
                canvas.clipRect(
                        worldX2screenX(xcur + dx - EVENT_SHAPE_RADIUS - LINE_HIGHLIGHT_WIDTH/2f), 0,
                        worldX2screenX(xcur + dx + EVENT_SHAPE_RADIUS + LINE_HIGHLIGHT_WIDTH/2f), height)
                canvas.drawCircle(px, py, r, paint)
                canvas.restore()
            }


            // draw labels
            paint.color = Color.BLACK
            paint.style = Paint.Style.FILL
            //canvas.drawText(event.title, px, py + worldD2screenD(EVENT_SHAPE_RADIUS + 15f), paint )
            //canvas.drawBitmap(event.bitmap, px - event.bitmap.width/2, py - event.bitmap.height/2 + , paint)
            rect.set(event.bitmap)
                    .centerIn(0f, 0f)
                    .fitSize(worldD2screenD(IMAGE_SIZE).toFloat())
                    .translate(px, py + worldD2screenD(IMAGE_Y_TRANSLATION))
            canvas.drawBitmap(event.bitmap, null, rect, paint)

        }
    }

    /** set world x coordinate **/
    private fun setWorldX(x : Float) {
        Log.i(TAG, "setWorldX(): $x")
        xcur = Math.max(xmin - OVERSCROLL, Math.min(xmax + OVERSCROLL, x))
        invalidate()
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
            Log.i(TAG, "onTouchEvent(): action=: ${event.action}, scroller=${scroller.isFinished}");
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
            Log.i(TAG, "Click: x=$x, y=$y, event=$event")
            event?.scrollTo()
            return event != null
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            // NOTE: we calculate in pixel
            accumulatedDistance += screenD2worldD(distanceX)
            val x = initialOffset + accumulatedDistance

            Log.i(TAG, "onScroll(): initialOffset=$initialOffset,  accumulatedDistance=$accumulatedDistance -> x=$x");
            setWorldX(x)
            return false
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            // NOTE: we calculate in pixel
            val cur = worldD2screenD(xcur)
            val min = worldD2screenD(xmin)
            val max = worldD2screenD(xmax)
            val velocity = (-velocityX / dp2px(FLING_SCALE)).toInt()
            val overScroll = dp2px(OVERSCROLL).toInt()
            stopScrolling()
            Log.i(TAG, "onFling(): velocity=$velocityX, cur=$cur, min=$min, max=$max, overScroll=$overScroll");
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
        val cur = worldD2screenD(xcur)
        val min = worldD2screenD(this.x)
        val max = worldD2screenD(this.x)
        Log.i(TAG, "scrollToNearestEvent(): cur=$cur, min=$min, max=$max");
        if (scroller.springBack(
                cur, 0, // current
                min, max, // limits x
                0, 0)) {
            // limit y
            Log.i(TAG, "springBack started");
            scrollAnimator.start()
        }
    }

    /** called by scroll animator during fling or springBack  */
    private fun onScrollAnimationStep() {
        if (!scroller.isFinished) {
            scroller.computeScrollOffset()
            Log.i(TAG, "onScrollAnimationStep(): current=${scroller.currX}, start=${scroller.startX}, end=${scroller.finalX}, velocity=${scroller.currVelocity}");
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

    //region -- UTILITY FUNCTIONS --

    private fun dp2px(dp : Float): Float = dp * Resources.getSystem().displayMetrics.density

    private fun px2dp(px : Float): Float = px / Resources.getSystem().displayMetrics.density

    private fun px2dp(px : Int): Float =  px2dp(px.toFloat())

    private fun worldX2screenX(worldX: Float): Int = width/2 + worldD2screenD(worldX - xcur)

    private fun worldY2screenY(worldY: Float): Int = height/2 + worldD2screenD(worldY)

    private fun screenX2worldX(screenX: Int): Float = screenX2worldX(screenX.toFloat())

    private fun screenX2worldX(screenX: Float): Float = xcur + screenD2worldD(screenX - width/2f)

    private fun screenY2worldY(screenY: Int): Float = screenY2worldY(screenY.toFloat())

    private fun screenY2worldY(screenY: Float): Float = screenD2worldD(screenY - height/2f)

    private fun worldD2screenD(worldDistance: Float): Int = (worldDistance * dp2px(1f)).toInt()

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
        this.offset(dx, dy)
        return this
    }

    private fun RectF.centerIn(x : Float, y : Float) : RectF {
        return translate(x - width()/2f, y - height()/2f)
    }

    private fun RectF.fitSize(size : Float) : RectF {
        val scale = when {
            rect.height() > rect.width() -> size / rect.height()
            else -> size / rect.width()
        }
        return scaleCenter(scale)
    }

    private fun RectF.scaleCenter(scale : Float) : RectF {
        val w2 = width()*(scale - 1f)/2f
        val h2 = height()*(scale - 1f)/2f
        this.left -= w2
        this.top -= h2
        this.right += w2
        this.bottom += h2
        return this
    }
}