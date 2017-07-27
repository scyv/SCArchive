package de.scyv.scarchive.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

@Service
public class Scheduler {

	private List<Runnable> runner = new ArrayList<>();

	public void addRunner(Runnable runner) {
		this.runner.add(runner);
	}

	@PostConstruct
	public void startFileWatcher() {
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				runner.forEach(Runnable::run);
			}
		};

		ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
		es.scheduleWithFixedDelay(runnable, 1, 10, TimeUnit.SECONDS);
	}
}
