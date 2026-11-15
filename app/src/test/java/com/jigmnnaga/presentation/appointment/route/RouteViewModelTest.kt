package com.jigmnnaga.presentation.appointment.route

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteViewModelTest {
    private fun viewModel(state: RouteUiState = RouteSampleData.overview) = RouteViewModel(state)

    @Test
    fun startsWithGivenState() {
        val viewModel = viewModel()
        assertEquals(RouteSampleData.overview, viewModel.uiState.value)
    }

    @Test
    fun myCardClickOpensRouteSelection() {
        val viewModel = viewModel()
        viewModel.onMyCardClick()
        assertEquals(RouteStage.SELECT, viewModel.uiState.value.stage)
    }

    @Test
    fun walksThroughAllStagesInOrder() {
        val viewModel = viewModel()
        viewModel.onMyCardClick()
        viewModel.onSelectTransport(RouteTransport.BUS)
        viewModel.onStartMoving()
        assertEquals(RouteStage.MOVING, viewModel.uiState.value.stage)
        assertEquals(RouteTransport.BUS, viewModel.uiState.value.selectedTransport)
    }

    @Test
    fun startMovingDoesNothingBeforeSelection() {
        val viewModel = viewModel()
        viewModel.onMyCardClick()
        viewModel.onStartMoving()
        assertEquals(RouteStage.SELECT, viewModel.uiState.value.stage)
    }

    @Test
    fun toggleOptionExpandsAndFolds() {
        val viewModel = viewModel(RouteSampleData.selecting)
        viewModel.onToggleOption(RouteTransport.SUBWAY)
        assertEquals(RouteTransport.SUBWAY, viewModel.uiState.value.expandedTransport)
        viewModel.onToggleOption(RouteTransport.SUBWAY)
        assertEquals(null, viewModel.uiState.value.expandedTransport)
    }

    @Test
    fun toggleMyRouteWorksWhileMoving() {
        val viewModel = viewModel(RouteSampleData.moving)
        viewModel.onToggleMyRoute()
        assertTrue(viewModel.uiState.value.isMyRouteExpanded)
    }

    @Test
    fun arriveFinishesTheTripOnlyNearArrival() {
        val moving = viewModel(RouteSampleData.moving)
        moving.onArrive()
        assertFalse(moving.uiState.value.moving?.hasArrived == true)

        val near = viewModel(RouteSampleData.nearArrival)
        near.onArrive()
        assertTrue(near.uiState.value.moving?.hasArrived == true)
    }

    @Test
    fun backFromSelectionReturnsToOverviewAndIsHandled() {
        val viewModel = viewModel(RouteSampleData.selecting)
        assertTrue(viewModel.onBack())
        assertEquals(RouteStage.OVERVIEW, viewModel.uiState.value.stage)
    }

    @Test
    fun backFromOverviewAndMovingIsLeftToTheCaller() {
        val overview = viewModel(RouteSampleData.overview)
        assertFalse(overview.onBack())
        assertEquals(RouteStage.OVERVIEW, overview.uiState.value.stage)

        val moving = viewModel(RouteSampleData.moving)
        assertFalse(moving.onBack())
        assertEquals(RouteStage.MOVING, moving.uiState.value.stage)
    }
}
