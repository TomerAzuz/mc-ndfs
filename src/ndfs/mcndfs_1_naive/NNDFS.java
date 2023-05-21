package ndfs.mcndfs_1_naive;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.concurrent.*;


import ndfs.NDFS;

/**
 * Implements the {@link ndfs.NDFS} interface, mostly delegating the work to a
 * worker class.
 */
public class NNDFS implements NDFS {

    private final ArrayList<Worker> workers = new ArrayList<>();

    /**
     * Constructs an NDFS object using the specified Promela file.
     *
     * @param promelaFile the Promela file.
     * @throws FileNotFoundException is thrown in case the file could not be read.
     */

    public NNDFS(File promelaFile, int nrWorkers) throws FileNotFoundException {
        for (int i = 0; i < nrWorkers; i++) {
            workers.add(new Worker(promelaFile, i));
        }
    }

    @Override
    public boolean ndfs() {
        boolean isCycle = false;
        ExecutorService pool = Executors.newFixedThreadPool(workers.size());
        CompletionService<Boolean> ecs = new ExecutorCompletionService<>(pool);

        for (Worker w : workers) {
            ecs.submit(w);
        }

        try {
            isCycle = ecs.take().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        pool.shutdownNow();

        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        System.out.println("Terminated");
        return isCycle;
    }
}
