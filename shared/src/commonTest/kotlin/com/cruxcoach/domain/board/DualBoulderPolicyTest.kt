package com.cruxcoach.domain.board

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DualBoulderPolicyTest {

    @Test
    fun disjointSpansCanDisplayTogether() {
        val left = HorizontalSpan(minX = 10, maxX = 40)
        val right = HorizontalSpan(minX = 50, maxX = 90)

        assertTrue(DualBoulderPolicy.canDisplayTogether(left, right))
        assertTrue(DualBoulderPolicy.canDisplayTogether(right, left))
        assertEquals(BoulderSide.LEFT, DualBoulderPolicy.sideOf(left, right))
        assertEquals(BoulderSide.RIGHT, DualBoulderPolicy.sideOf(right, left))
        assertEquals(45f, DualBoulderPolicy.dividerX(left, right))
        assertEquals(45f, DualBoulderPolicy.dividerX(right, left))
    }

    @Test
    fun touchingSpansAtSameXCannotDisplayTogether() {
        // Strict inequality: rightmost hold of left boulder must be strictly
        // to the left of leftmost hold of right boulder (maxX < minX).
        val left = HorizontalSpan(minX = 10, maxX = 50)
        val right = HorizontalSpan(minX = 50, maxX = 90)

        assertFalse(DualBoulderPolicy.canDisplayTogether(left, right))
        assertFalse(DualBoulderPolicy.canDisplayTogether(right, left))
        assertNull(DualBoulderPolicy.sideOf(left, right))
        assertNull(DualBoulderPolicy.dividerX(left, right))
    }

    @Test
    fun overlappingSpansCannotDisplayTogether() {
        val a = HorizontalSpan(minX = 10, maxX = 60)
        val b = HorizontalSpan(minX = 40, maxX = 90)

        assertFalse(DualBoulderPolicy.canDisplayTogether(a, b))
        assertNull(DualBoulderPolicy.sideOf(a, b))
        assertNull(DualBoulderPolicy.dividerX(a, b))
    }

    @Test
    fun auroraHoldsAndFramesSpanComputation() {
        val xByPlacement = mapOf(
            100 to 12L,
            101 to 28L,
            200 to 80L,
            201 to 108L,
        )
        val leftHolds = listOf(BoardHold(100, 12), BoardHold(101, 13))
        val rightHolds = listOf(BoardHold(200, 12), BoardHold(201, 14))

        assertTrue(DualBoulderPolicy.canDisplayTogether(leftHolds, rightHolds, xByPlacement))
        assertEquals(
            HorizontalSpan(12, 28),
            DualBoulderPolicy.horizontalSpanFromFrames("p100r12p101r13", xByPlacement),
        )
        assertEquals(
            HorizontalSpan(80, 108),
            DualBoulderPolicy.horizontalSpanFromFrames("p200r12p201r14", xByPlacement),
        )
    }

    @Test
    fun missingPlacementCoordinateFailsClosed() {
        val xByPlacement = mapOf(100 to 12L, 200 to 80L)
        val withUnknownHold = listOf(BoardHold(100, 12), BoardHold(999, 13))
        val validHolds = listOf(BoardHold(200, 12))

        assertNull(DualBoulderPolicy.horizontalSpan(withUnknownHold, xByPlacement))
        assertNull(DualBoulderPolicy.horizontalSpanFromFrames("p100r12p999r13", xByPlacement))
        assertFalse(DualBoulderPolicy.canDisplayTogether(withUnknownHold, validHolds, xByPlacement))
    }

    @Test
    fun moonBoardColumnSpanAndDisjointCheck() {
        // Column A (col 0): holdId 1 (A1), holdId 12 (A2)
        // Column D (col 3): holdId 4 (D1), holdId 26 (D3)
        // Column F (col 5): holdId 6 (F1)
        // Column K (col 10): holdId 11 (K1)
        val leftFrames = "p1r42p4r43p26r44" // cols 0..3
        val rightFrames = "p6r42p11r44" // cols 5..10
        val touchingFrames = "p4r42p11r44" // cols 3..10 (touches col 3)

        assertEquals(HorizontalSpan(0, 3), DualBoulderPolicy.moonBoardHorizontalSpan(leftFrames))
        assertEquals(HorizontalSpan(5, 10), DualBoulderPolicy.moonBoardHorizontalSpan(rightFrames))
        assertTrue(DualBoulderPolicy.canDisplayMoonBoardTogether(leftFrames, rightFrames))
        assertFalse(DualBoulderPolicy.canDisplayMoonBoardTogether(leftFrames, touchingFrames))
        assertEquals(4f, DualBoulderPolicy.dividerX(HorizontalSpan(0, 3), HorizontalSpan(5, 10)))
    }

    @Test
    fun moonBoardOutOfRangeHoldFailsClosed() {
        // Mini MoonBoard has 12 rows -> max holdId = 132. Hold 150 is out of range.
        assertNull(DualBoulderPolicy.moonBoardHorizontalSpan("p1r42p150r44", gridRows = 12))
    }

    @Test
    fun boardBrandEligibilityExcludesQuantumAndInfoLayer() {
        assertTrue(DualBoulderPolicy.isEligibleBoardBrand(BoardBrand.KILTER))
        assertTrue(DualBoulderPolicy.isEligibleBoardBrand(BoardBrand.MOONBOARD))
        assertTrue(DualBoulderPolicy.isEligibleBoardBrand(BoardBrand.TENSION))
        assertTrue(DualBoulderPolicy.isEligibleBoardBrand(BoardBrand.GRASSHOPPER))
        assertTrue(DualBoulderPolicy.isEligibleBoardBrand(BoardBrand.DECOY))
        assertTrue(DualBoulderPolicy.isEligibleBoardBrand(BoardBrand.SOILL))
        assertTrue(DualBoulderPolicy.isEligibleBoardBrand(BoardBrand.TOUCHSTONE))
        assertFalse(DualBoulderPolicy.isEligibleBoardBrand(BoardBrand.QUANTUM))
        assertFalse(DualBoulderPolicy.isEligibleBoardBrand(BoardBrand.AURORA))
        assertFalse(DualBoulderPolicy.isEligibleBoardBrand(BoardBrand.TWELVECLIMB))
    }
}
