package com.jigmnnaga.presentation.appointment.invite

import com.jigmnnaga.domain.model.AppointmentInvite
import com.jigmnnaga.domain.model.InviteOperationResult
import com.jigmnnaga.domain.model.InviteParticipant
import com.jigmnnaga.domain.model.InviteParticipantRole
import com.jigmnnaga.domain.model.InviteParticipationStatus
import com.jigmnnaga.domain.repository.AppointmentInviteRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentInviteViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun acceptOnlyShowsAcceptedAfterRepositorySuccessAndIgnoresDuplicateTap() =
        runTest(mainDispatcherRule.dispatcher) {
            val fake = FakeInviteRepository()
            val gate = CompletableDeferred<InviteOperationResult<Long>>()
            fake.accept = { gate.await() }
            val viewModel = AppointmentInviteViewModel(fake, mainDispatcherRule.dispatcher)
            viewModel.loadInvite("abc123")
            advanceUntilIdle()

            viewModel.acceptInvite()
            viewModel.acceptInvite()
            advanceUntilIdle()
            val submitting = viewModel.state.value as AppointmentInviteFlowState.Invitation
            assertEquals(AppointmentInviteResult.PENDING, submitting.uiState.result)
            assertTrue(submitting.uiState.isSubmitting)
            assertEquals(1, fake.acceptCalls)

            gate.complete(InviteOperationResult.Success(appointmentId))
            advanceUntilIdle()
            val accepted = viewModel.state.value as AppointmentInviteFlowState.Invitation
            assertEquals(AppointmentInviteResult.ACCEPTED, accepted.uiState.result)
            assertEquals(appointmentId, accepted.uiState.appointmentId)
            assertFalse(accepted.uiState.isSubmitting)
        }

    @Test
    fun declineShowsDeclinedOnlyAfterRepositorySuccess() = runTest(mainDispatcherRule.dispatcher) {
        val fake = FakeInviteRepository()
        val gate = CompletableDeferred<InviteOperationResult<Unit>>()
        fake.decline = { gate.await() }
        val viewModel = AppointmentInviteViewModel(fake, mainDispatcherRule.dispatcher)
        viewModel.loadInvite("abc123")
        advanceUntilIdle()

        viewModel.declineInvite()
        advanceUntilIdle()
        val submitting = viewModel.state.value as AppointmentInviteFlowState.Invitation
        assertEquals(AppointmentInviteResult.PENDING, submitting.uiState.result)
        assertTrue(submitting.uiState.isSubmitting)

        gate.complete(InviteOperationResult.Success(Unit))
        advanceUntilIdle()
        val declined = viewModel.state.value as AppointmentInviteFlowState.Invitation
        assertEquals(AppointmentInviteResult.DECLINED, declined.uiState.result)
        assertFalse(declined.uiState.isSubmitting)
    }

    @Test
    fun failedDecisionKeepsPendingAndAllowsRetry() = runTest(mainDispatcherRule.dispatcher) {
        val fake = FakeInviteRepository()
        fake.accept = {
            if (fake.acceptCalls == 1) InviteOperationResult.Failure(retryable = true)
            else InviteOperationResult.Success(appointmentId)
        }
        val viewModel = AppointmentInviteViewModel(fake, mainDispatcherRule.dispatcher)
        viewModel.loadInvite("abc123")
        advanceUntilIdle()

        viewModel.acceptInvite()
        advanceUntilIdle()
        val failed = viewModel.state.value as AppointmentInviteFlowState.Invitation
        assertEquals(AppointmentInviteResult.PENDING, failed.uiState.result)
        assertEquals(AppointmentInviteDecisionError.REQUEST_FAILED, failed.uiState.decisionError)
        assertFalse(failed.uiState.isSubmitting)

        viewModel.acceptInvite()
        advanceUntilIdle()
        val accepted = viewModel.state.value as AppointmentInviteFlowState.Invitation
        assertEquals(AppointmentInviteResult.ACCEPTED, accepted.uiState.result)
        assertEquals(2, fake.acceptCalls)
    }

    @Test
    fun loginRetryUsesTheSameInviteCode() = runTest(mainDispatcherRule.dispatcher) {
        val fake = FakeInviteRepository()
        fake.get = { InviteOperationResult.LoginRequired }
        val viewModel = AppointmentInviteViewModel(fake, mainDispatcherRule.dispatcher)
        viewModel.loadInvite("resume-me")
        advanceUntilIdle()
        assertEquals(AppointmentInviteFlowState.LoginRequired, viewModel.state.value)

        fake.get = { InviteOperationResult.Success(invite()) }
        viewModel.retryCurrentInvite()
        advanceUntilIdle()
        assertEquals("resume-me", fake.lastInviteCode)
        assertTrue(viewModel.state.value is AppointmentInviteFlowState.Invitation)
    }

    private class FakeInviteRepository : AppointmentInviteRepository {
        var get: suspend () -> InviteOperationResult<AppointmentInvite> = {
            InviteOperationResult.Success(invite())
        }
        var accept: suspend () -> InviteOperationResult<Long> = {
            InviteOperationResult.Success(appointmentId)
        }
        var decline: suspend () -> InviteOperationResult<Unit> = {
            InviteOperationResult.Success(Unit)
        }
        var acceptCalls = 0
        var lastInviteCode: String? = null

        override suspend fun getInvite(inviteCode: String): InviteOperationResult<AppointmentInvite> {
            lastInviteCode = inviteCode
            return get()
        }

        override suspend fun acceptInvite(inviteCode: String): InviteOperationResult<Long> {
            lastInviteCode = inviteCode
            acceptCalls++
            return accept()
        }

        override suspend fun declineInvite(inviteCode: String): InviteOperationResult<Unit> {
            lastInviteCode = inviteCode
            return decline()
        }
    }

    class MainDispatcherRule : TestWatcher() {
        val dispatcher = StandardTestDispatcher()
        override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
        override fun finished(description: Description) = Dispatchers.resetMain()
    }

    private companion object {
        const val appointmentId = 42L
    }
}

private fun invite() = AppointmentInvite(
    appointmentId = 42,
    title = "테스트 약속",
    canceled = false,
    hostNickname = "초대한 사람",
    responseDeadline = null,
    expired = false,
    respondedCount = 1,
    joinedCount = 1,
    myStatus = InviteParticipationStatus.PENDING,
    myRole = InviteParticipantRole.MEMBER,
    participants = listOf(
        InviteParticipant(
            memberId = 7,
            nickname = "초대한 사람",
            profileImageUrl = null,
            role = InviteParticipantRole.HOST,
            status = InviteParticipationStatus.ACCEPTED
        ),
        InviteParticipant(
            memberId = 8,
            nickname = "나",
            profileImageUrl = null,
            role = InviteParticipantRole.MEMBER,
            status = InviteParticipationStatus.PENDING
        )
    )
)
