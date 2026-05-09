import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class BackgroundExecutorTest {

    @Test
    void execute_runsTask() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicBoolean ran = new AtomicBoolean();
        try (BackgroundExecutor ex = new BackgroundExecutor()) {
            ex.execute(() -> {
                ran.set(true);
                done.countDown();
            });
            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertTrue(ran.get());
        }
    }
}
