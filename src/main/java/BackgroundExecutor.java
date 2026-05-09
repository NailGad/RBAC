import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Пул потоков для фоновых задач (отчёты, сохранение).
 */
public final class BackgroundExecutor implements AutoCloseable {

    private final ExecutorService executor;

    public BackgroundExecutor() {
        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "rbac-background");
            t.setDaemon(true);
            return t;
        });
    }

    public void execute(Runnable task) {
        executor.execute(task);
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
