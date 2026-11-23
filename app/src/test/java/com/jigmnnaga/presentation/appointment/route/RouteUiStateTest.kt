package com.jigmnnaga.presentation.appointment.route

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteUiStateTest {
    private val overview = RouteSampleData.overview
    private val selecting = RouteSampleData.selecting

    @Test
    fun overviewStartsWithoutSelectionAndCannotStart() {
        assertEquals(RouteStage.OVERVIEW, overview.stage)
        assertNull(overview.selectedTransport)
        assertFalse(overview.canStart())
        assertFalse(overview.canArrive())
    }

    @Test
    fun openRouteSelectionMovesOverviewToSelect() {
        assertEquals(RouteStage.SELECT, overview.openRouteSelection().stage)
    }

    @Test
    fun openRouteSelectionKeepsOtherStages() {
        val moving = RouteSampleData.moving
        assertSame(moving, moving.openRouteSelection())
        assertSame(selecting, selecting.openRouteSelection())
    }

    @Test
    fun closeRouteSelectionReturnsToOverviewAndFoldsDetail() {
        val expanded = selecting.toggleOptionExpanded(RouteTransport.SUBWAY)
        val closed = expanded.closeRouteSelection()
        assertEquals(RouteStage.OVERVIEW, closed.stage)
        assertNull(closed.expandedTransport)
    }

    @Test
    fun closeRouteSelectionKeepsChosenTransport() {
        val chosen = selecting.selectTransport(RouteTransport.TAXI)
        assertEquals(RouteTransport.TAXI, chosen.closeRouteSelection().selectedTransport)
    }

    @Test
    fun closeRouteSelectionIgnoresOverviewAndMoving() {
        assertSame(overview, overview.closeRouteSelection())
        val moving = RouteSampleData.moving
        assertSame(moving, moving.closeRouteSelection())
    }

    @Test
    fun selectTransportSetsSelectionAndEnablesStart() {
        val chosen = selecting.selectTransport(RouteTransport.BUS)
        assertEquals(RouteTransport.BUS, chosen.selectedTransport)
        assertEquals(RouteTransport.BUS, chosen.selectedOption?.transport)
        assertTrue(chosen.canStart())
    }

    @Test
    fun selectTransportReplacesPreviousChoice() {
        val chosen = selecting.selectTransport(RouteTransport.BUS).selectTransport(RouteTransport.SUBWAY)
        assertEquals(RouteTransport.SUBWAY, chosen.selectedTransport)
    }

    @Test
    fun selectTransportIgnoresUnknownTransportAndOtherStages() {
        val withoutTaxi = selecting.copy(options = selecting.options.filter { it.transport != RouteTransport.TAXI })
        assertSame(withoutTaxi, withoutTaxi.selectTransport(RouteTransport.TAXI))
        assertSame(overview, overview.selectTransport(RouteTransport.BUS))
    }

    @Test
    fun toggleOptionExpandedOpensOneAtATime() {
        val bus = selecting.toggleOptionExpanded(RouteTransport.BUS)
        assertEquals(RouteTransport.BUS, bus.expandedTransport)
        val subway = bus.toggleOptionExpanded(RouteTransport.SUBWAY)
        assertEquals(RouteTransport.SUBWAY, subway.expandedTransport)
    }

    @Test
    fun toggleOptionExpandedFoldsWhenToggledTwice() {
        val folded = selecting
            .toggleOptionExpanded(RouteTransport.BUS)
            .toggleOptionExpanded(RouteTransport.BUS)
        assertNull(folded.expandedTransport)
    }

    @Test
    fun expandingDoesNotChangeSelection() {
        val chosen = selecting.selectTransport(RouteTransport.BUS)
        val expanded = chosen.toggleOptionExpanded(RouteTransport.TAXI)
        assertEquals(RouteTransport.BUS, expanded.selectedTransport)
        assertEquals(RouteTransport.TAXI, expanded.expandedTransport)
    }

    @Test
    fun toggleOptionExpandedIgnoresOverview() {
        assertSame(overview, overview.toggleOptionExpanded(RouteTransport.BUS))
    }

    @Test
    fun startMovingNeedsSelection() {
        assertSame(selecting, selecting.startMoving())
    }

    @Test
    fun startMovingNeedsMovingInformation() {
        val withoutMoving = selecting.selectTransport(RouteTransport.BUS).copy(moving = null)
        assertFalse(withoutMoving.canStart())
        assertSame(withoutMoving, withoutMoving.startMoving())
    }

    @Test
    fun startMovingMovesToMovingAndFoldsDetail() {
        val started = selecting
            .selectTransport(RouteTransport.BUS)
            .toggleOptionExpanded(RouteTransport.BUS)
            .startMoving()
        assertEquals(RouteStage.MOVING, started.stage)
        assertNull(started.expandedTransport)
    }

    @Test
    fun toggleMyRouteExpandedFlipsOnlyWhileMoving() {
        val moving = RouteSampleData.moving
        assertFalse(moving.isMyRouteExpanded)
        val expanded = moving.toggleMyRouteExpanded()
        assertTrue(expanded.isMyRouteExpanded)
        assertFalse(expanded.toggleMyRouteExpanded().isMyRouteExpanded)
        assertSame(overview, overview.toggleMyRouteExpanded())
    }

    @Test
    fun arriveIsIgnoredUntilNearArrival() {
        val moving = RouteSampleData.moving
        assertFalse(moving.canArrive())
        assertSame(moving, moving.arrive())
    }

    @Test
    fun nearArrivalEnablesArrive() {
        assertTrue(RouteSampleData.nearArrival.canArrive())
    }

    @Test
    fun arriveMarksMeArrivedAndDisablesButton() {
        val arrived = RouteSampleData.nearArrival.arrive()
        assertTrue(arrived.moving?.hasArrived == true)
        assertFalse(arrived.canArrive())
        assertEquals(RouteMemberStatus.ARRIVED, arrived.members.first { it.isMe }.status)
    }

    @Test
    fun arriveLeavesFriendsUntouched() {
        val before = RouteSampleData.nearArrival
        val after = before.arrive()
        assertEquals(before.members.filter { !it.isMe }, after.members.filter { !it.isMe })
    }

    @Test
    fun arriveCannotRunTwice() {
        val arrived = RouteSampleData.nearArrival.arrive()
        assertSame(arrived, arrived.arrive())
    }

    @Test
    fun locationPermissionGrantedByDefaultSoNoPermissionScreenShows() {
        assertTrue(RouteSampleData.moving.locationPermissionGranted)
        assertFalse(RouteSampleData.moving.needsLocationPermission)
    }

    @Test
    fun noPermissionScreenShowsOnlyWhileMovingWithoutPermission() {
        assertTrue(RouteSampleData.moving.copy(locationPermissionGranted = false).needsLocationPermission)
        assertFalse(overview.copy(locationPermissionGranted = false).needsLocationPermission)
        assertFalse(selecting.copy(locationPermissionGranted = false).needsLocationPermission)
    }

    @Test
    fun noPermissionScreenDoesNotShowAfterArrival() {
        assertFalse(RouteSampleData.arrived.copy(locationPermissionGranted = false).needsLocationPermission)
    }

    @Test
    fun startIsBlockedWhenTodayIsNotTheMeetingDay() {
        val chosen = selecting.selectTransport(RouteTransport.BUS)
        assertTrue(chosen.canStart())
        assertFalse(chosen.copy(departureOpen = false).canStart())
    }

    @Test
    fun startIsOpenByDefaultWhenMeetingDateIsUnknown() {
        assertNull(overview.meetingDate)
        assertTrue(overview.departureOpen)
    }
}
