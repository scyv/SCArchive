package de.scyv.scarchive.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Scheduler service that runs registered runnables periodically.
 */
@Service
public class Scheduler {

    private final List<Runnable> runner = new ArrayList<>();

    @Value("${scarchive.scheduler.pollingInterval}")
    private String pollingIntervall;

    /**
     * Add a runnable to the list.
     *
     * @param runner
     *            not null!
     */
    public void addRunner(Runnable runner) {
        this.runner.add(runner);
    }

    /**
     * Start the scheduler
     */
    @PostConstruct
    public void start() {
        final Runnable runnable = () -> runner.forEach(Runnable::run);

        final ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
        es.scheduleWithFixedDelay(runnable, 10, Integer.parseInt(pollingIntervall), TimeUnit.SECONDS);
    }
}
