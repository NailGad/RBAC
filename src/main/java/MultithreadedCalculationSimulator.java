import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class MultithreadedCalculationSimulator {

    private static final Object PRINT_LOCK = new Object();

    private MultithreadedCalculationSimulator() {}

    public static void main(String[] args) {
        int threadCount = 4;
        int barLength = 32;
        int stepDelayMs = 100;

        try {
            if (args.length >= 1) {
                threadCount = Integer.parseInt(args[0].trim());
            }
            if (args.length >= 2) {
                barLength = Integer.parseInt(args[1].trim());
            }
            if (args.length >= 3) {
                stepDelayMs = Integer.parseInt(args[2].trim());
            }
        } catch (NumberFormatException e) {
            System.err.println("Аргументы: [потоки] [длинаБара] [мсНаШаг] — целые числа.");
            System.exit(1);
            return;
        }

        if (threadCount < 1 || threadCount > 32) {
            System.err.println("Число потоков: от 1 до 32.");
            System.exit(1);
            return;
        }
        if (barLength < 5 || barLength > 120) {
            System.err.println("Длина прогресс-бара: от 5 до 120.");
            System.exit(1);
            return;
        }
        if (stepDelayMs < 20 || stepDelayMs > 2000) {
            System.err.println("Задержка шага: от 20 до 2000 мс.");
            System.exit(1);
            return;
        }

        new MultithreadedCalculationSimulator().run(threadCount, barLength, stepDelayMs);
    }

    private void run(int threadCount, int barLength, int stepDelayMs) {
        final int firstDataRow = 4;

        synchronized (PRINT_LOCK) {
            System.out.print("\u001b[2J\u001b[H");
            System.out.println("=== Имитация многопоточного расчёта ===");
            System.out.println("потоков: " + threadCount + " | шагов бара: " + barLength + " | задержка: " + stepDelayMs + " мс");
            System.out.println();
            System.out.flush();
        }

        for (int i = 0; i < threadCount; i++) {
            printAtRow(firstDataRow + i, "");
        }

        CountDownLatch done = new CountDownLatch(threadCount);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        for (int n = 1; n <= threadCount; n++) {
            final int workerNo = n;
            pool.execute(() -> runWorker(workerNo, firstDataRow + workerNo - 1, barLength, stepDelayMs, done));
        }

        try {
            done.await();
            pool.shutdown();
            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        int afterRows = firstDataRow + threadCount;
        synchronized (PRINT_LOCK) {
            System.out.print("\u001b[" + afterRows + ";1H\u001b[2K");
            System.out.println("Все потоки завершили работу.");
            System.out.flush();
        }
    }

    @SuppressWarnings("deprecation")
    private void runWorker(int workerNo, int screenRow, int barLength, int stepDelayMs, CountDownLatch done) {
        long startNs = System.nanoTime();
        long threadId = Thread.currentThread().getId();
        try {
            for (int step = 0; step <= barLength; step++) {
                String bar = renderBar(step, barLength);
                double elapsedMs = (System.nanoTime() - startNs) / 1_000_000.0;
                String line;
                if (step < barLength) {
                    line = String.format(
                            "№%-2d | id=%-3d | %s",
                            workerNo, threadId, bar);
                } else {
                    line = String.format(
                            "№%-2d | id=%-3d | %s | готово за %.2f мс",
                            workerNo, threadId, bar, elapsedMs);
                }
                printAtRow(screenRow, line);
                if (step < barLength) {
                    Thread.sleep(stepDelayMs);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            printAtRow(screenRow, String.format("№%-2d | id=%-3d | прервано", workerNo, threadId));
        } finally {
            done.countDown();
        }
    }

    private static String renderBar(int filledSteps, int totalSteps) {
        int filled = totalSteps == 0 ? 0 : (int) Math.round((double) filledSteps / totalSteps * totalSteps);
        filled = Math.min(filled, totalSteps);
        StringBuilder sb = new StringBuilder(totalSteps + 2);
        sb.append('[');
        for (int i = 0; i < totalSteps; i++) {
            sb.append(i < filled ? '#' : '·');
        }
        sb.append(']');
        return sb.toString();
    }

    private static void printAtRow(int rowOneBased, String text) {
        synchronized (PRINT_LOCK) {
            System.out.print("\u001b[" + rowOneBased + ";1H\u001b[2K");
            System.out.print(text);
            System.out.flush();
        }
    }
}
