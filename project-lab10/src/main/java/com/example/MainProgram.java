package com.example;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MainProgram {
    public static void main(String[] args) {
        Job describedJob = new DescribedJob("Wykonuje cykliczne zadanie");
        Job showTimeJob = new ShowTimeJob(LocalDateTime.now());

        JobScheduler describedJobScheduler = new SimpleJobScheduler()
                .forJob(describedJob)
                .startsAt(LocalDateTime.now().plusSeconds(2))
                .everySeconds(1)
                .repeatTimes(5);

        JobScheduler showTimeJobScheduler = new SimpleJobScheduler()
                .forJob(showTimeJob)
                .startsAt(LocalDateTime.now().plusSeconds(1))
                .everySeconds(2)
                .repeatTimes(3);

        JobSchedulerRegistry registry = JobSchedulerRegistry.getInstance();

        registry.register(describedJobScheduler);
        registry.register(showTimeJobScheduler);

        registry.start();
    }
}

interface Job {
    void run();

    void setJobTime(LocalDateTime jobTime);

    LocalDateTime getJobTime();
}

class ShowTimeJob implements Job {
    private LocalDateTime jobTime;

    public ShowTimeJob(LocalDateTime jobTime) {
        this.jobTime = jobTime;
    }

    @Override
    public void run() {
        System.out.println("ShowTimeJob: " + jobTime);
    }

    @Override
    public void setJobTime(LocalDateTime jobTime) {
        this.jobTime = jobTime;
    }

    @Override
    public LocalDateTime getJobTime() {
        return jobTime;
    }
}

class DescribedJob implements Job {
    private final String description;
    private LocalDateTime jobTime;

    public DescribedJob(String description) {
        this.description = description;
    }

    @Override
    public void run() {
        System.out.println("DescribedJob: " + description + " | time: " + jobTime);
    }

    @Override
    public void setJobTime(LocalDateTime jobTime) {
        this.jobTime = jobTime;
    }

    @Override
    public LocalDateTime getJobTime() {
        return jobTime;
    }
}

class JobThread extends Thread {
    private final Job job;

    public JobThread(Job job) {
        this.job = job;
    }

    @Override
    public void run() {
        job.run();
    }
}

interface JobScheduler {
    JobScheduler forJob(Job job);

    JobScheduler startsAt(LocalDateTime startTime);

    JobScheduler everySeconds(int seconds);

    JobScheduler repeatTimes(int times);

    void listenTo(TimeEvent event);

    boolean isFinished();
}

class SimpleJobScheduler implements JobScheduler {
    private Job job;
    private LocalDateTime startTime;
    private LocalDateTime nextRunTime;

    private int intervalSeconds = 1;
    private int repeatTimes = 0;
    private int executedTimes = 0;

    private boolean finished = false;

    @Override
    public JobScheduler forJob(Job job) {
        this.job = job;
        return this;
    }

    @Override
    public JobScheduler startsAt(LocalDateTime startTime) {
        this.startTime = startTime;
        this.nextRunTime = startTime;
        return this;
    }

    @Override
    public JobScheduler everySeconds(int seconds) {
        if (seconds <= 0) {
            throw new IllegalArgumentException("Interval must be greater than 0.");
        }

        this.intervalSeconds = seconds;
        return this;
    }

    @Override
    public JobScheduler repeatTimes(int times) {
        if (times < 0) {
            throw new IllegalArgumentException("Repeat times cannot be negative.");
        }

        this.repeatTimes = times;
        return this;
    }

    @Override
    public synchronized void listenTo(TimeEvent event) {
        if (finished || job == null || event == null || event.getTime() == null) {
            return;
        }

        LocalDateTime currentTime = event.getTime();

        if (startTime == null) {
            startTime = currentTime;
            nextRunTime = currentTime;
        }

        if (!currentTime.isBefore(nextRunTime)) {
            job.setJobTime(currentTime);

            Thread jobThread = new JobThread(job);
            jobThread.start();

            executedTimes++;

            if (repeatTimes > 0 && executedTimes >= repeatTimes) {
                finished = true;
                return;
            }

            nextRunTime = nextRunTime.plusSeconds(intervalSeconds);
        }
    }

    @Override
    public boolean isFinished() {
        return finished;
    }
}

class JobSchedulerRegistry extends Thread {
    private static final JobSchedulerRegistry instance = new JobSchedulerRegistry();

    private final List<JobScheduler> schedulers = new ArrayList<>();
    private boolean running = true;

    private JobSchedulerRegistry() {
    }

    public static JobSchedulerRegistry getInstance() {
        return instance;
    }

    public synchronized void register(JobScheduler scheduler) {
        if (scheduler != null) {
            schedulers.add(scheduler);
        }
    }

    public synchronized void stopRegistry() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            TimeEvent event = new TimeEvent(LocalDateTime.now());

            List<JobScheduler> schedulersSnapshot;

            synchronized (this) {
                schedulers.removeIf(JobScheduler::isFinished);
                schedulersSnapshot = new ArrayList<>(schedulers);
            }

            for (JobScheduler scheduler : schedulersSnapshot) {
                scheduler.listenTo(event);
            }

            if (schedulersSnapshot.isEmpty()) {
                running = false;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }
}

class TimeEvent {
    private LocalDateTime time;

    public TimeEvent() {
    }

    public TimeEvent(LocalDateTime time) {
        this.time = time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public LocalDateTime getTime() {
        return time;
    }
}