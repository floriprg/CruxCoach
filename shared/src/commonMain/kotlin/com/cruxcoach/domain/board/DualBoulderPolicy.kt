package com.cruxcoach.domain.board

/**
 * Horizontal extent of a boulder's holds on a board (in placement X units
 * for Aurora/Quantum boards, or 0-based column index 0..10 for MoonBoard).
 * X increases strictly from left to right.
 */
data class HorizontalSpan(
    val minX: Long,
    val maxX: Long,
) {
    /** True when this boulder sits strictly to the left of [other]:
     *  its rightmost hold is strictly to the left of [other]'s leftmost hold. */
    fun isLeftOf(other: HorizontalSpan): Boolean = maxX < other.minX

    /** True when this boulder sits strictly to the right of [other]. */
    fun isRightOf(other: HorizontalSpan): Boolean = other.maxX < minX

    /** True when the two boulders have strictly disjoint horizontal spans. */
    fun isDisjointFrom(other: HorizontalSpan): Boolean = isLeftOf(other) || isRightOf(other)
}

/** Which horizontal side of the board a boulder occupies in a dual-boulder pair. */
enum class BoulderSide {
    LEFT,
    RIGHT,
}

/**
 * Pure policy for displaying two boulders simultaneously on one board.
 *
 * Two boulders may be shown together if and only if they are strictly
 * horizontally separated: the rightmost hold of the left boulder lies
 * strictly to the left of the leftmost hold of the right boulder
 * (`maxX(left) < minX(right)`).
 *
 * All calculations fail closed: empty hold sets or holds with missing /
 * out-of-range coordinates return `null` and cannot be paired.
 */
object DualBoulderPolicy {

    /**
     * Boards that use the side-by-side dual-boulder overlay.
     * Interactive single-layer boards (Kilter, MoonBoard, Tension, Grasshopper,
     * Decoy, So iLL, Touchstone) support it; Quantum uses its own 4-layer
     * independent-controller rack, and map-only brands have no climbs.
     */
    fun isEligibleBoardBrand(brand: BoardBrand): Boolean =
        brand.isInteractive && !brand.supportsIndependentClimbLayers

    /**
     * Compute the horizontal span of [holds] using [xByPlacement].
     * Returns `null` if [holds] is empty or any hold lacks a coordinate.
     */
    fun horizontalSpan(
        holds: List<BoardHold>,
        xByPlacement: Map<Int, Long>,
    ): HorizontalSpan? {
        if (holds.isEmpty()) return null
        var minX = Long.MAX_VALUE
        var maxX = Long.MIN_VALUE
        for (hold in holds) {
            val x = xByPlacement[hold.placementId] ?: return null
            if (x < minX) minX = x
            if (x > maxX) maxX = x
        }
        return HorizontalSpan(minX, maxX)
    }

    /**
     * Fast horizontal-span scan over a raw Aurora `frames` string (`p{id}r{role}…`),
     * matching [BoardZoneFilter.climbInZone]'s zero-regex hot path.
     * Returns `null` if [frames] is blank, has no holds, or any hold is missing
     * from [xByPlacement].
     */
    fun horizontalSpanFromFrames(
        frames: String,
        xByPlacement: Map<Int, Long>,
    ): HorizontalSpan? {
        if (frames.isBlank()) return null
        if (BoardClimbParser.isClimbConcat(frames)) {
            return horizontalSpan(BoardClimbParser.parseFrames(frames), xByPlacement)
        }
        var minX = Long.MAX_VALUE
        var maxX = Long.MIN_VALUE
        var sawHold = false
        var i = 0
        val n = frames.length
        while (i < n) {
            if (frames[i] != 'p') {
                i++
                continue
            }
            var j = i + 1
            var id = 0
            var digits = false
            while (j < n && frames[j] in '0'..'9') {
                id = id * 10 + (frames[j] - '0')
                j++
                digits = true
            }
            if (digits && j < n && frames[j] == 'r') {
                val x = xByPlacement[id] ?: return null
                sawHold = true
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                i = j + 1
            } else {
                i = maxOf(j, i + 1)
            }
        }
        return if (sawHold) HorizontalSpan(minX, maxX) else null
    }

    /**
     * Compute the horizontal span of a MoonBoard climb's `frames` string in
     * 0-based column coordinates (`0 = A` .. `10 = K`).
     * Returns `null` if empty or if any hold id is outside the variant's grid.
     */
    fun moonBoardHorizontalSpan(
        frames: String,
        gridRows: Int = MoonBoardVariant.GRID_ROWS,
    ): HorizontalSpan? {
        if (frames.isBlank()) return null
        val parsed = MoonBoardFrameEncoder.parseHolds(frames)
        if (parsed.isEmpty()) return null
        val maxHoldId = MoonBoardVariant.GRID_COLUMNS * gridRows
        var minCol = Long.MAX_VALUE
        var maxCol = Long.MIN_VALUE
        for ((holdId, _) in parsed) {
            if (holdId !in 1..maxHoldId) return null
            val col = ((holdId - 1) % MoonBoardVariant.GRID_COLUMNS).toLong()
            if (col < minCol) minCol = col
            if (col > maxCol) maxCol = col
        }
        return HorizontalSpan(minCol, maxCol)
    }

    /** True when both spans are known and strictly horizontally disjoint. */
    fun canDisplayTogether(first: HorizontalSpan?, second: HorizontalSpan?): Boolean =
        first != null && second != null && first.isDisjointFrom(second)

    /** Convenience check directly from two Aurora hold lists. */
    fun canDisplayTogether(
        firstHolds: List<BoardHold>,
        secondHolds: List<BoardHold>,
        xByPlacement: Map<Int, Long>,
    ): Boolean = canDisplayTogether(
        horizontalSpan(firstHolds, xByPlacement),
        horizontalSpan(secondHolds, xByPlacement),
    )

    /** Convenience check directly from two MoonBoard frame strings. */
    fun canDisplayMoonBoardTogether(
        firstFrames: String,
        secondFrames: String,
        gridRows: Int = MoonBoardVariant.GRID_ROWS,
    ): Boolean = canDisplayTogether(
        moonBoardHorizontalSpan(firstFrames, gridRows),
        moonBoardHorizontalSpan(secondFrames, gridRows),
    )

    /**
     * Which side [target] sits on relative to [other], or `null` if they overlap.
     */
    fun sideOf(target: HorizontalSpan, other: HorizontalSpan): BoulderSide? = when {
        target.isLeftOf(other) -> BoulderSide.LEFT
        target.isRightOf(other) -> BoulderSide.RIGHT
        else -> null
    }

    /**
     * Midpoint X coordinate between the rightmost hold of the left boulder and
     * the leftmost hold of the right boulder, or `null` if they overlap.
     */
    fun dividerX(first: HorizontalSpan, second: HorizontalSpan): Float? {
        if (!first.isDisjointFrom(second)) return null
        val leftMax = minOf(first.maxX, second.maxX)
        val rightMin = maxOf(first.minX, second.minX)
        return (leftMax + rightMin) / 2f
    }
}
