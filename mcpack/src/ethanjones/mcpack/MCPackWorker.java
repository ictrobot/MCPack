package ethanjones.mcpack;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MCPackWorker {

    private static final int NUM_WORKERS = Math.max(8, Runtime.getRuntime().availableProcessors());
    private static final Thread[] threads = new Thread[NUM_WORKERS];
    private static final Worker worker = new Worker();

    static {
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(worker, "Worker" + i);
            threads[i].setDaemon(true);
            threads[i].setPriority(Thread.MIN_PRIORITY);
            threads[i].start();
        }
    }

    private static class Worker implements Runnable {
        private LinkedBlockingQueue<WorkerTask> queue = new LinkedBlockingQueue<>();

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                WorkerTask task = queue.peek();
                while (task == null) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        return;
                    }
                    task = queue.peek();
                }

                runQueue(task, task.fileTasks);
                task.filesComplete.countDown();
                try {
                    task.filesComplete.await();
                } catch (InterruptedException e) {
                    return;
                }

                runQueue(task, task.folderTasks);
                task.foldersComplete.countDown();
                try {
                    task.foldersComplete.await();
                } catch (InterruptedException e) {
                    return;
                }

                runQueue(task, task.fileTasks);

                task.done.countDown();
                try {
                    task.done.await();
                } catch (InterruptedException e) {
                    return;
                }

                queue.remove(task);
            }
        }

        private void runQueue(WorkerTask task, ConcurrentLinkedQueue<BRunnable> tasks) {
            BRunnable bRunnable;
            while ((bRunnable = tasks.poll()) != null) {
                if (!bRunnable.run()) task.success.set(false);
            }
        }
    }

    public interface BRunnable {
        boolean run();
    }

    public static class WorkerTask {
        private final ConcurrentLinkedQueue<BRunnable> fileTasks = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<BRunnable> folderTasks = new ConcurrentLinkedQueue<>();

        private final CountDownLatch filesComplete = new CountDownLatch(NUM_WORKERS);
        private final CountDownLatch foldersComplete = new CountDownLatch(NUM_WORKERS);
        private final CountDownLatch done = new CountDownLatch(NUM_WORKERS);

        private final AtomicBoolean success = new AtomicBoolean(true);

        public void addFileTask(BRunnable b) {
            fileTasks.add(b);
        }

        public void addFolderTask(BRunnable b) {
            folderTasks.add(b);
        }

        public boolean run() {
            worker.queue.add(this);
            try {
                done.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return success.get();
        }
    }
}
