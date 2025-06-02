package com.example.maincontainerfor2048webmaster

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.widget.Toast
import kotlin.math.max

// PUBLIC_INTERFACE
class Game2048View @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), GestureDetector.OnGestureListener {

    // --- Appearance and Layout Constants ---
    private val gridSize = 4
    private val gridPadding = dpToPx(16)
    private val cellMargin = dpToPx(8)
    private var cellSize = 0f

    // --- Theme Colors ---
    private val bgColor = Color.parseColor("#faf8ef")
    private val gridBgColor = Color.parseColor("#bbada0")
    private val accentColor = Color.parseColor("#f59563")
    private val tileColors = mapOf(
        0 to Color.parseColor("#cdc1b4"),
        2 to Color.parseColor("#eee4da"),
        4 to Color.parseColor("#ede0c8"),
        8 to Color.parseColor("#f2b179"),
        16 to Color.parseColor("#f59563"),
        32 to Color.parseColor("#f67c5f"),
        64 to Color.parseColor("#f65e3b"),
        128 to Color.parseColor("#edcf72"),
        256 to Color.parseColor("#edcc61"),
        512 to Color.parseColor("#edc850"),
        1024 to Color.parseColor("#edc53f"),
        2048 to Color.parseColor("#edc22e"),
    )

    // --- Game Matrix / State ---
    private var tiles = Array(gridSize) { IntArray(gridSize) }
    private var score = 0
    private var bestScore = 0
    private var isGameOver = false

    private val gestureDetector = GestureDetector(context, this)

    // --- Paints (cached for performance) ---
    private val paintTile = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintScore = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintBg = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        isFocusable = true
        isClickable = true
        setBackgroundColor(bgColor)
        startNewGame()
    }

    // PUBLIC_INTERFACE
    fun startNewGame() {
        score = 0
        isGameOver = false
        for (i in tiles.indices) {
            tiles[i].fill(0)
        }
        spawnTile()
        spawnTile()
        invalidate()
    }

    // --- Touch and gesture handling for swipe ---
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            return gestureDetector.onTouchEvent(event)
        }
        return super.onTouchEvent(event)
    }

    override fun onDown(e: MotionEvent): Boolean {
        return true
    }

    override fun onShowPress(e: MotionEvent) {
        // Not used for swipe detection
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        // For simple tap: restart when game over
        if (isGameOver) startNewGame()
        return true
    }

    override fun onScroll(
        e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float
    ): Boolean {
        // Not used for swipe detection
        return false
    }

    override fun onLongPress(e: MotionEvent) {
        // Not used, required for interface
    }

    override fun onFling(
        e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float
    ): Boolean {
        if (isGameOver || e1 == null || e2 == null) return true
        val dx = e2.x - e1.x
        val dy = e2.y - e1.y
        if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
            if (dx > 0) move(Direction.RIGHT) else move(Direction.LEFT)
        } else {
            if (dy > 0) move(Direction.DOWN) else move(Direction.UP)
        }
        return true
    }

    // PUBLIC_INTERFACE
    fun restartGame() = startNewGame()

    // --- Game movement/algorithm ---
    // PUBLIC_INTERFACE
    enum class Direction { UP, DOWN, LEFT, RIGHT }

    // PUBLIC_INTERFACE
    fun move(direction: Direction) {
        val before = tilesDeepCopy()
        var moved = false
        when (direction) {
            Direction.UP -> moved = moveVertically(up = true)
            Direction.DOWN -> moved = moveVertically(up = false)
            Direction.LEFT -> moved = moveHorizontally(left = true)
            Direction.RIGHT -> moved = moveHorizontally(left = false)
        }
        if (moved) {
            spawnTile()
            if (score > bestScore) bestScore = score
            if (isNoMovesAvailable()) {
                isGameOver = true
                Toast.makeText(context, "Game Over!", Toast.LENGTH_SHORT).show()
            }
            invalidate()
        }
    }

    private fun tilesDeepCopy() = Array(gridSize) { tiles[it].clone() }

    private fun spawnTile() {
        val empty = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (tiles[r][c] == 0) empty.add(Pair(r, c))
            }
        }
        if (empty.isNotEmpty()) {
            val (row, col) = empty.random()
            tiles[row][col] = if ((0..9).random() == 0) 4 else 2
        }
    }

    private fun moveHorizontally(left: Boolean): Boolean {
        var moved = false
        for (r in 0 until gridSize) {
            val old = tiles[r].copyOf()
            val merged = BooleanArray(gridSize)
            val line = tiles[r].copyOf()
            if (!left) line.reverse()
            var insert = 0
            for (i in 1 until gridSize) {
                if (line[i] == 0) continue
                var j = i
                while (j > insert) {
                    if (line[j-1] == 0) {
                        line[j-1] = line[j]
                        line[j] = 0
                        j--
                    } else if (line[j-1] == line[j] && !merged[j-1]) {
                        line[j-1] *= 2
                        score += line[j-1]
                        merged[j-1] = true
                        line[j] = 0
                        insert = j
                        break
                    } else {
                        break
                    }
                }
            }
            if (!left) line.reverse()
            tiles[r] = line
            if (!old.contentEquals(line)) moved = true
        }
        return moved
    }

    private fun moveVertically(up: Boolean): Boolean {
        var moved = false
        for (c in 0 until gridSize) {
            val old = IntArray(gridSize) { tiles[it][c] }
            val merged = BooleanArray(gridSize)
            val line = IntArray(gridSize) { tiles[it][c] }
            if (!up) line.reverse()
            var insert = 0
            for (i in 1 until gridSize) {
                if (line[i] == 0) continue
                var j = i
                while (j > insert) {
                    if (line[j-1] == 0) {
                        line[j-1] = line[j]
                        line[j] = 0
                        j--
                    } else if (line[j-1] == line[j] && !merged[j-1]) {
                        line[j-1] *= 2
                        score += line[j-1]
                        merged[j-1] = true
                        line[j] = 0
                        insert = j
                        break
                    } else {
                        break
                    }
                }
            }
            if (!up) line.reverse()
            for (r in 0 until gridSize) tiles[r][c] = line[r]
            if (!old.contentEquals(line)) moved = true
        }
        return moved
    }

    // PUBLIC_INTERFACE
    fun isNoMovesAvailable(): Boolean {
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (tiles[r][c] == 0) return false
                if (c < gridSize - 1 && tiles[r][c] == tiles[r][c+1]) return false
                if (r < gridSize - 1 && tiles[r][c] == tiles[r+1][c]) return false
            }
        }
        return true
    }

    // --- Drawing and UI ---
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = minOf(
            MeasureSpec.getSize(widthMeasureSpec), 
            MeasureSpec.getSize(heightMeasureSpec)
        )
        setMeasuredDimension(size, size + dpToPx(100)) // Scoreboard above
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width - 2 * gridPadding
        val height = width

        if (width <= 0 || height <= 0) {
            // Avoid drawing or division by zero when layout is not measured or during IDE preview
            return
        }

        cellSize = (width - (gridSize+1)*cellMargin) / gridSize.toFloat()

        // Draw background
        paintBg.color = bgColor
        canvas.drawRect(0f, 0f, this.width.toFloat(), this.height.toFloat(), paintBg)

        // Draw score and best score
        val scoreboardHeight = dpToPx(60)
        drawScoreBoards(canvas, scoreboardHeight)

        // Draw grid background
        val topOffset = scoreboardHeight + gridPadding
        paintTile.color = gridBgColor
        canvas.drawRoundRect(
            gridPadding.toFloat(),
            topOffset.toFloat(),
            (width + gridPadding).toFloat(),
            (height + topOffset).toFloat(),
            dpToPx(12).toFloat(), dpToPx(12).toFloat(), paintTile
        )

        // Draw tiles
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                drawTile(canvas, r, c, topOffset)
            }
        }

        // Draw game over if needed
        if (isGameOver) {
            paintTile.color = Color.argb(200, 238, 228, 218)
            canvas.drawRect(
                gridPadding.toFloat(),
                topOffset.toFloat(),
                (width + gridPadding).toFloat(),
                (height + topOffset).toFloat(),
                paintTile
            )
            paintText.color = Color.parseColor("#776e65")
            paintText.textAlign = Paint.Align.CENTER
            paintText.textSize = cellSize * 0.40f
            canvas.drawText(
                "Game Over!",
                (this.width / 2).toFloat(),
                (height / 2 + topOffset + cellSize * 0.1f),
                paintText
            )
        }
    }

    private fun drawScoreBoards(canvas: Canvas, height: Int) {
        paintTile.color = accentColor
        val boardWidth = width / 3f
        val offset = gridPadding.toFloat()
        val l1 = offset
        val l2 = width - boardWidth - gridPadding
        val t = gridPadding / 2f
        val b = height - gridPadding / 4f

        // Score box
        canvas.drawRoundRect(l1, t, l1+boardWidth, b, dpToPx(8).toFloat(), dpToPx(8).toFloat(), paintTile)
        paintText.color = Color.WHITE
        paintText.textAlign = Paint.Align.CENTER
        paintText.textSize = dpToPx(16).toFloat()
        canvas.drawText("Score", l1 + boardWidth/2, t + dpToPx(18).toFloat(), paintText)
        paintText.textSize = dpToPx(20).toFloat()
        canvas.drawText("$score", l1 + boardWidth/2, t + dpToPx(40).toFloat(), paintText)

        // Best Score box
        paintTile.color = gridBgColor
        canvas.drawRoundRect(l2, t, l2+boardWidth, b, dpToPx(8).toFloat(), dpToPx(8).toFloat(), paintTile)
        paintText.color = Color.WHITE
        paintText.textAlign = Paint.Align.CENTER
        paintText.textSize = dpToPx(16).toFloat()
        canvas.drawText("Best", l2 + boardWidth/2, t + dpToPx(18).toFloat(), paintText)
        paintText.textSize = dpToPx(20).toFloat()
        canvas.drawText("$bestScore", l2 + boardWidth/2, t + dpToPx(40).toFloat(), paintText)

        // Restart "button" rectangle (we just treat this area as a tap area for restart)
        paintTile.color = accentColor
        val btnWidth = dpToPx(80)
        val btnHeight = dpToPx(36)
        val btnLeft = (width - btnWidth) / 2f + gridPadding
        val btnTop = b + gridPadding/1.5f
        val btnRect = RectF(btnLeft, btnTop, btnLeft+btnWidth, btnTop+btnHeight)
        canvas.drawRoundRect(btnRect, dpToPx(16).toFloat(), dpToPx(16).toFloat(), paintTile)
        paintText.color = Color.WHITE
        paintText.textSize = dpToPx(18).toFloat()
        canvas.drawText("Restart", btnRect.centerX(), btnRect.centerY() + dpToPx(6), paintText)
    }

    private fun drawTile(canvas: Canvas, row: Int, col: Int, yOffset: Int) {
        val value = tiles[row][col]
        val x = gridPadding + cellMargin + col * (cellSize + cellMargin)
        val y = yOffset + cellMargin + row * (cellSize + cellMargin)
        paintTile.color = tileColors.getOrElse(value) { tileColors[2048] ?: Color.LTGRAY }
        canvas.drawRoundRect(
            x, y, x+cellSize, y+cellSize,
            dpToPx(8).toFloat(), dpToPx(8).toFloat(), paintTile
        )

        if (value > 0) {
            paintText.color = if (value <= 4) Color.parseColor("#776e65") else Color.WHITE
            paintText.textAlign = Paint.Align.CENTER
            paintText.textSize = cellSize * (0.35f - 0.02f * (value.toString().length - 1))
            val centerX = x + cellSize / 2
            val centerY = y + cellSize / 2 - (paintText.descent() + paintText.ascent())/2
            canvas.drawText(value.toString(), centerX, centerY, paintText)
        }
    }

    // Helper: dp to px
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
    }

    // --- Restart tap recognition ---
    override fun performClick(): Boolean {
        // Allow accessibility tap
        if (isGameOver || isRestartTap(lastTouchX, lastTouchY)) {
            restartGame()
            return true
        }
        return super.performClick()
    }
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            lastTouchX = it.x
            lastTouchY = it.y
        }
        return super.dispatchTouchEvent(event)
    }
    private fun isRestartTap(x: Float, y: Float): Boolean {
        // Check if the tap was in the restart button region
        val scoreboardHeight = dpToPx(60)
        val btnWidth = dpToPx(80)
        val btnHeight = dpToPx(36)
        val btnLeft = (width - btnWidth) / 2f + gridPadding
        val btnTop = scoreboardHeight - gridPadding / 4f + gridPadding/1.5f
        return x in btnLeft..(btnLeft+btnWidth) && y in btnTop..(btnTop+btnHeight)
    }
}
