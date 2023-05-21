package ndfs.mcndfs_1_naive;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import graph.Graph;
import graph.GraphFactory;
import graph.State;

/**
 * This is a straightforward implementation of Figure 1 of
 * <a href="http://www.cs.vu.nl/~tcs/cm/ndfs/laarman.pdf"> "the Laarman
 * paper"</a>.
 */
public class Worker implements Callable<Boolean> {

    private final Graph graph;
    private final Colors colors = new Colors();
    private final int threadID;
    public volatile static boolean result = false;
    static final HashMap<State, Integer> counter = new HashMap<>();

    // Throwing an exception is a convenient way to cut off the search in case a
    // cycle is found.
    private static class CycleFoundException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    /**
     * Constructs a Worker object using the specified Promela file.
     *
     * @param promelaFile
     *            the Promela file.
     * @throws FileNotFoundException
     *             is thrown in case the file could not be read.
     */
    public Worker(File promelaFile, int threadID) throws FileNotFoundException {
        this.threadID = threadID;
        this.graph = GraphFactory.createGraph(promelaFile);
    }

    public void updateCounter(State s, boolean isIncrement) {
        synchronized (counter)  {
            Integer val = counter.get(s);
            int count = val == null ? 0 : val;
            if (isIncrement) counter.put(s, count + 1);
            else             counter.put(s, count - 1);
        }
    }

    public synchronized boolean isRed(State t)  { return colors.reds.get(t) != null; }

    private void dfsRed(State s) throws CycleFoundException, InterruptedException {
        if(Thread.interrupted())  {
            throw new InterruptedException();
        }
        colors.setPink(s, true);
        List<State> postStates = graph.post(s);
        Collections.shuffle(postStates);

        for (State t : postStates) {
            if (colors.hasColor(t, Color.CYAN)) {
                result = true;
                throw new CycleFoundException();
            }

            if (!colors.isPink(t)  && !isRed(t)) {
                dfsRed(t);
            }
        }
        if(s.isAccepting()) {
            updateCounter(s, false);
            synchronized (counter) {
                while (true) {
                    Integer val = counter.get(s);
                    int count = val == null ? 0 : val;
                    if (count > 0) {
                        counter.wait();
                    } else {
                        break;
                    }
                }
            }
        }
        synchronized (colors.reds) {
            colors.reds.put(s, true);
        }
        colors.setPink(s, false);
    }

    private void dfsBlue(State s) throws CycleFoundException, InterruptedException {
        if(Thread.interrupted())  {
            throw new InterruptedException();
        }
        colors.color(s, Color.CYAN);

        List<State> postStates = graph.post(s);
        Collections.shuffle(postStates);

        for (State t : postStates) {
            if (colors.hasColor(t, Color.WHITE) && !isRed(t)) {
                dfsBlue(t);
            }
        }
        if (s.isAccepting()) {
            updateCounter(s, true);
            dfsRed(s);
        }
        colors.color(s, Color.BLUE);
    }

    private void nndfs(State s) throws CycleFoundException, InterruptedException {
        dfsBlue(s);
    }

    @Override
    public Boolean call() {
        try {
            nndfs(graph.getInitialState());
        } catch (CycleFoundException | InterruptedException c) {
            return result;
        }
        return false;
    }
}