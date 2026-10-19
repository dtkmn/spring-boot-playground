package __PACKAGE_NAME__.ai;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import __PACKAGE_NAME__.service.OrderService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Timeout(10)
class InvocationBudgetTest {
    private static final OrderAssistant.Reply REPLY =
        new OrderAssistant.Reply("CLARIFY", "Please clarify", null, List.of());

    @Test
    void cancellationBeforeDispatchReleasesOnceAndNeverRunsTheCallable() {
        var ran = new AtomicBoolean();
        var releases = new AtomicInteger();
        var work = new OrderAssistant.Work(() -> {
            ran.set(true);
            return REPLY;
        }, releases::incrementAndGet);

        assertThat(work.cancel(true)).isTrue();
        assertThat(releases.get()).isEqualTo(1);
        work.run();
        assertThat(work.cancel(true)).isFalse();
        work.run();
        assertThat(ran.get()).isFalse();
        assertThat(releases.get()).isEqualTo(1);
    }

    @Test
    void cancellationAfterExecutorRejectionReleasesTheUndispatchedPermitOnce() {
        var ran = new AtomicBoolean();
        var releases = new AtomicInteger();
        var work = new OrderAssistant.Work(() -> {
            ran.set(true);
            return REPLY;
        }, releases::incrementAndGet);

        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.shutdown();
            assertThatExceptionOfType(RejectedExecutionException.class)
                .isThrownBy(() -> executor.execute(work));
            assertThat(work.cancel(true)).isTrue();
            assertThat(work.cancel(true)).isFalse();
        }
        assertThat(ran.get()).isFalse();
        assertThat(releases.get()).isEqualTo(1);
    }

    @Test
    void cancelingARunningNoncooperativeCallableRetainsItsPermitUntilActualExit() throws Exception {
        var entered = new CountDownLatch(1);
        var interrupted = new CountDownLatch(1);
        var allowExit = new CountDownLatch(1);
        var releases = new AtomicInteger();
        var work = new OrderAssistant.Work(() -> {
            entered.countDown();
            // Emulate a dependency that notices interruption but keeps working.
            while (true) {
                try {
                    allowExit.await();
                    break;
                } catch (InterruptedException exception) {
                    interrupted.countDown();
                }
            }
            return REPLY;
        }, releases::incrementAndGet);
        var worker = Thread.ofVirtual().start(work);
        try {
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(work.cancel(true)).isTrue();
            assertThat(interrupted.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(work.isDone()).isTrue();
            assertThat(releases.get()).isZero();

            allowExit.countDown();
            worker.join(2000);
            assertThat(worker.isAlive()).isFalse();
            assertThat(releases.get()).isEqualTo(1);
            assertThat(work.cancel(true)).isFalse();
            work.run();
            assertThat(releases.get()).isEqualTo(1);
        } finally {
            allowExit.countDown();
            worker.join(2000);
        }
    }

    @Test
    void normalCompletionReleasesOnceAndPreservesTheReply() throws Exception {
        var releases = new AtomicInteger();
        var work = new OrderAssistant.Work(() -> REPLY, releases::incrementAndGet);
        work.run();
        assertThat(work.get()).isSameAs(REPLY);
        assertThat(releases.get()).isEqualTo(1);
        work.run();
        assertThat(work.cancel(true)).isFalse();
        assertThat(releases.get()).isEqualTo(1);
    }

    @Test
    void failedCompletionReleasesOnceAndPreservesTheFailure() {
        var releases = new AtomicInteger();
        var failure = new IllegalStateException("Dependency failed");
        var work = new OrderAssistant.Work(() -> { throw failure; }, releases::incrementAndGet);
        work.run();
        assertThatExceptionOfType(ExecutionException.class).isThrownBy(work::get)
            .withCause(failure);
        assertThat(releases.get()).isEqualTo(1);
        work.run();
        assertThat(work.cancel(true)).isFalse();
        assertThat(releases.get()).isEqualTo(1);
    }

    @Test
    void permitsOneClassifierCallAndRejectsASecond() {
        var budget = budget();
        budget.beforeModel();
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE, budget::beforeModel);
    }

    @Test
    void toolsRequireTheSelectedOrderAndASuccessfulOrderLookupBeforeShipment() {
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE, () -> budget().beforeTool("getOrder", 2001));
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE, () -> budget().beforeTool("getOrder", 0));
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE, () -> budget().beforeTool("executeSql", 1002));
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE, () -> budget().beforeTool("getShipment", 1002));

        var budget = budget();
        budget.beforeTool("getOrder", 1002);
        budget.orderLookedUp = true;
        budget.order = new OrderService.OrderFacts(1002, "SHIPPED");
        budget.beforeTool("getShipment", 1002);
        budget.shipmentLookedUp = true;
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE, () -> budget.beforeTool("getShipment", 1002));
    }

    @Test
    void repeatedOrderLookupIsRejectedEvenBeforeTheTotalToolBudgetIsExhausted() {
        var budget = budget();
        budget.beforeTool("getOrder", 1002);
        budget.orderLookedUp = true;
        budget.order = new OrderService.OrderFacts(1002, "SHIPPED");
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE, () -> budget.beforeTool("getOrder", 1002));
    }

    @Test
    void missingOwnerFilteredOrderStopsFurtherInference() {
        var budget = budget();
        budget.beforeTool("getOrder", 1002);
        budget.orderLookedUp = true;
        assertStatus(HttpStatus.NOT_FOUND, budget::beforeModel);
        assertStatus(HttpStatus.SERVICE_UNAVAILABLE, () -> budget.beforeTool("getShipment", 1002));
    }

    @Test
    void expiredCanceledAndInterruptedBudgetsRejectFurtherWork() {
        var expired = new InvocationBudget("caller-a", 1002, Duration.ZERO);
        assertStatus(HttpStatus.GATEWAY_TIMEOUT, expired::beforeModel);

        var canceled = budget();
        canceled.cancel();
        assertStatus(HttpStatus.GATEWAY_TIMEOUT, canceled::beforeModel);
        assertStatus(HttpStatus.GATEWAY_TIMEOUT, () -> canceled.beforeTool("getOrder", 1002));

        var interrupted = budget();
        Thread.currentThread().interrupt();
        try {
            assertStatus(HttpStatus.GATEWAY_TIMEOUT, interrupted::check);
        } finally {
            Thread.interrupted();
        }
    }

    private static InvocationBudget budget() {
        return new InvocationBudget("caller-a", 1002, Duration.ofSeconds(60));
    }

    private static void assertStatus(HttpStatus status, Runnable operation) {
        assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(operation::run)
            .satisfies(exception -> assertThat(exception.getStatusCode()).isEqualTo(status));
    }
}
