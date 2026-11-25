import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class VisualizerGUI extends JFrame {

    private final ArrayList<Thread> threads = new ArrayList<>();
    private DefaultListModel<String> threadModel;
    private JTextArea output;
    private final JLabel[] stateLabels = new JLabel[6];

    private final String[] states = {
        "NEW", "RUNNABLE", "BLOCKED",
        "WAITING", "TIMED_WAITING", "TERMINATED"
    };

    private int threadCount = 0;

    static class SharedCounter {
        int value = 0;

        void unsafeIncrement() {
            value++;
        }

        synchronized void safeIncrement() {
            value++;
        }
    }

    public VisualizerGUI() {
        setTitle("Multithreading and Thread Lifecycle Demonstrator");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        main.setBackground(new Color(25, 25, 40));

        JLabel title = new JLabel(
            "MULTITHREADING AND THREAD LIFECYCLE",
            JLabel.CENTER
        );
        title.setFont(new Font("Arial", Font.BOLD, 23));
        title.setForeground(Color.WHITE);

        // Thread lifecycle state counters
        JPanel lifecycle = new JPanel(new GridLayout(2, 3, 10, 10));
        lifecycle.setBackground(new Color(25, 25, 40));

        for (int i = 0; i < states.length; i++) {
            stateLabels[i] = new JLabel(states[i] + " : 0", JLabel.CENTER);
            stateLabels[i].setOpaque(true);
            stateLabels[i].setBackground(new Color(50, 60, 80));
            stateLabels[i].setForeground(Color.WHITE);
            stateLabels[i].setFont(new Font("Arial", Font.BOLD, 15));
            lifecycle.add(stateLabels[i]);
        }

        // Thread list and execution log
        threadModel = new DefaultListModel<>();
        JList<String> threadList = new JList<>(threadModel);

        output = new JTextArea();
        output.setEditable(false);
        output.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JPanel left = new JPanel(new BorderLayout());
        left.add(new JLabel("THREAD DETAILS"), BorderLayout.NORTH);
        left.add(new JScrollPane(threadList), BorderLayout.CENTER);
        left.setPreferredSize(new Dimension(260, 300));

        JPanel right = new JPanel(new BorderLayout());
        right.add(new JLabel("EXECUTION LOG"), BorderLayout.NORTH);
        right.add(new JScrollPane(output), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT, left, right
        );
        split.setResizeWeight(0.3);

        // All demonstration buttons
        JButton createButton = new JButton("Create Thread");
        JButton startButton = new JButton("Start All Threads");
        JButton unsafeButton = new JButton("Without Synchronization");
        JButton safeButton = new JButton("With Synchronization");
        JButton raceButton = new JButton("Race Condition Visualizer");
        JButton priorityButton = new JButton("Thread Priority Demo");
        JButton pauseButton = new JButton("Pause / Resume Demo");

        createButton.addActionListener(e -> createThread());
        startButton.addActionListener(e -> startThreads());
        unsafeButton.addActionListener(e -> runCounterDemo(false));
        safeButton.addActionListener(e -> runCounterDemo(true));
        raceButton.addActionListener(e -> showRaceVisualizer());
        priorityButton.addActionListener(e -> showPriorityDemo());
        pauseButton.addActionListener(e -> showPauseResumeDemo());

        JPanel buttons = new JPanel(new GridLayout(2, 4, 8, 8));
        buttons.add(createButton);
        buttons.add(startButton);
        buttons.add(unsafeButton);
        buttons.add(safeButton);
        buttons.add(raceButton);
        buttons.add(priorityButton);
        buttons.add(pauseButton);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.add(lifecycle, BorderLayout.NORTH);
        center.add(split, BorderLayout.CENTER);

        main.add(title, BorderLayout.NORTH);
        main.add(center, BorderLayout.CENTER);
        main.add(buttons, BorderLayout.SOUTH);

        add(main);

        // Refresh actual Java thread states every 100 milliseconds
        new Timer(100, e -> updateStatuses()).start();
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> output.append(message));
    }

    private void registerThread(Thread thread) {
        threads.add(thread);
        threadModel.addElement(thread.getName() + " : " + thread.getState());
    }

    // 1. Create a thread
    private void createThread() {
        threadCount++;
        String name = "Thread-" + threadCount;

        Thread worker = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                final int task = i;
                log(name + " executing task " + task + "\n");

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log(name + " was interrupted.\n");
                    return;
                }
            }

            log(name + " completed.\n");
        }, name);

        registerThread(worker);
        output.append(name + " created.\n");
        updateStatuses();
    }

    // 2. Start all threads that are still in NEW state
    private void startThreads() {
        for (Thread thread : threads) {
            if (thread.getState() == Thread.State.NEW) {
                thread.start();
                output.append(thread.getName() + " started.\n");
            }
        }
        updateStatuses();
    }

    // 3. Synchronization demonstration
    private void runCounterDemo(boolean useSync) {
        final int numberOfThreads = 5;
        final int increments = 10000;

        SharedCounter counter = new SharedCounter();
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        String mode = useSync ? "SYNCHRONIZED" : "UNSYNCHRONIZED";
        output.append("\n--- " + mode + " DEMONSTRATION ---\n");
        output.append("Starting 5 threads...\n");

        for (int i = 1; i <= numberOfThreads; i++) {
            String name = mode + "-" + i;

            Thread worker = new Thread(() -> {
                try {
                    for (int j = 0; j < increments; j++) {
                        if (useSync) {
                            counter.safeIncrement();
                        } else {
                            counter.unsafeIncrement();
                        }
                    }
                } finally {
                    latch.countDown();

                    if (latch.getCount() == 0) {
                        SwingUtilities.invokeLater(() -> {
                            output.append(
                                "Expected counter: "
                                + (numberOfThreads * increments) + "\n"
                            );
                            output.append(
                                "Actual counter: " + counter.value + "\n"
                            );
                            output.append("Demonstration completed!\n\n");
                        });
                    }
                }
            }, name);

            registerThread(worker);
            worker.start();
        }
    }

    // 4. Race condition visualizer
    private void showRaceVisualizer() {
        JFrame frame = new JFrame("Race Condition Visualizer");
        frame.setSize(650, 450);
        frame.setLocationRelativeTo(this);
        frame.setLayout(new GridLayout(5, 1, 10, 10));

        SharedCounter unsafe = new SharedCounter();
        SharedCounter safe = new SharedCounter();

        AtomicInteger unsafeProgress = new AtomicInteger(0);
        AtomicInteger safeProgress = new AtomicInteger(0);

        final int total = 500000;
        final int numberOfThreads = 5;
        final int increments = total / numberOfThreads;

        JLabel unsafeLabel = new JLabel("Without Synchronization: 0");
        JLabel safeLabel = new JLabel("With Synchronization: 0");

        JProgressBar unsafeBar = new JProgressBar(0, total);
        JProgressBar safeBar = new JProgressBar(0, total);
        unsafeBar.setStringPainted(true);
        safeBar.setStringPainted(true);

        JLabel result = new JLabel("Running comparison...");

        frame.add(unsafeLabel);
        frame.add(unsafeBar);
        frame.add(safeLabel);
        frame.add(safeBar);
        frame.add(result);
        frame.setVisible(true);

        CountDownLatch latch = new CountDownLatch(numberOfThreads * 2);

        for (int i = 0; i < numberOfThreads; i++) {
            Thread unsafeThread = new Thread(() -> {
                try {
                    for (int j = 0; j < increments; j++) {
                        unsafe.unsafeIncrement();
                        unsafeProgress.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }, "Race-Unsafe-" + i);

            Thread safeThread = new Thread(() -> {
                try {
                    for (int j = 0; j < increments; j++) {
                        safe.safeIncrement();
                        safeProgress.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }, "Race-Safe-" + i);

            unsafeThread.start();
            safeThread.start();
        }

        Timer timer = new Timer(100, e -> {
            unsafeBar.setValue(unsafeProgress.get());
            safeBar.setValue(safeProgress.get());

            unsafeLabel.setText(
                "Without Synchronization: " + unsafe.value
            );
            safeLabel.setText("With Synchronization: " + safe.value);

            unsafeBar.setString(unsafeProgress.get() + " / " + total);
            safeBar.setString(safeProgress.get() + " / " + total);

            if (latch.getCount() == 0) {
                ((Timer) e.getSource()).stop();

                unsafeBar.setValue(total);
                safeBar.setValue(total);

                result.setText(
                    "Expected: " + total
                    + " | Unsafe: " + unsafe.value
                    + " | Safe: " + safe.value
                );

                output.append("\nRace condition visualization completed.\n");
                output.append("Unsafe result: " + unsafe.value + "\n");
                output.append("Safe result: " + safe.value + "\n");
            }
        });

        timer.start();
    }

    // 5. Thread priority demonstration
    private void showPriorityDemo() {
        JFrame frame = new JFrame("Thread Priority Demonstration");
        frame.setSize(500, 300);
        frame.setLayout(new GridLayout(3, 1, 5, 5));
        frame.setLocationRelativeTo(this);

        JLabel lowLabel = new JLabel("Low Priority (1): 0");
        JLabel normalLabel = new JLabel("Normal Priority (5): 0");
        JLabel highLabel = new JLabel("High Priority (10): 0");

        frame.add(lowLabel);
        frame.add(normalLabel);
        frame.add(highLabel);

        AtomicLong lowCount = new AtomicLong(0);
        AtomicLong normalCount = new AtomicLong(0);
        AtomicLong highCount = new AtomicLong(0);

        Thread low = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                lowCount.incrementAndGet();
            }
        }, "Low-Priority");

        Thread normal = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                normalCount.incrementAndGet();
            }
        }, "Normal-Priority");

        Thread high = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                highCount.incrementAndGet();
            }
        }, "High-Priority");

        low.setPriority(Thread.MIN_PRIORITY);
        normal.setPriority(Thread.NORM_PRIORITY);
        high.setPriority(Thread.MAX_PRIORITY);

        low.start();
        normal.start();
        high.start();

        Timer updateTimer = new Timer(100, e -> {
            lowLabel.setText("Low Priority (1): " + lowCount.get());
            normalLabel.setText("Normal Priority (5): " + normalCount.get());
            highLabel.setText("High Priority (10): " + highCount.get());
        });
        updateTimer.start();

        Timer stopTimer = new Timer(3000, e -> {
            low.interrupt();
            normal.interrupt();
            high.interrupt();

            updateTimer.stop();
            ((Timer) e.getSource()).stop();

            lowLabel.setText("Low Priority Final Count: " + lowCount.get());
            normalLabel.setText(
                "Normal Priority Final Count: " + normalCount.get()
            );
            highLabel.setText("High Priority Final Count: " + highCount.get());

            output.append("\nThread priority demonstration completed.\n");
            output.append("Low priority count: " + lowCount.get() + "\n");
            output.append("Normal priority count: " + normalCount.get() + "\n");
            output.append("High priority count: " + highCount.get() + "\n");
        });
        stopTimer.setRepeats(false);
        stopTimer.start();

        frame.setVisible(true);
    }

    // 6. Pause and resume demonstration using wait() and notifyAll()
    private void showPauseResumeDemo() {
        JFrame frame = new JFrame("Pause and Resume Demonstration");
        frame.setSize(450, 220);
        frame.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 25));
        frame.setLocationRelativeTo(this);

        JLabel status = new JLabel("Thread is starting...");
        JButton pause = new JButton("Pause");
        JButton resume = new JButton("Resume");
        resume.setEnabled(false);

        frame.add(status);
        frame.add(pause);
        frame.add(resume);

        Object lock = new Object();
        boolean[] paused = {false};

        Thread worker = new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                synchronized (lock) {
                    while (paused[0]) {
                        try {
                            lock.wait();
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }

                final int task = i;
                SwingUtilities.invokeLater(
                    () -> status.setText("Executing task: " + task + " / 10")
                );

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            SwingUtilities.invokeLater(() -> {
                status.setText("Thread completed!");
                pause.setEnabled(false);
                resume.setEnabled(false);
            });
        }, "Pause-Resume-Worker");

        pause.addActionListener(e -> {
            synchronized (lock) {
                paused[0] = true;
            }
            status.setText("Thread paused.");
            pause.setEnabled(false);
            resume.setEnabled(true);
            output.append("Pause/Resume worker paused.\n");
        });

        resume.addActionListener(e -> {
            synchronized (lock) {
                paused[0] = false;
                lock.notifyAll();
            }
            status.setText("Thread resumed.");
            pause.setEnabled(true);
            resume.setEnabled(false);
            output.append("Pause/Resume worker resumed.\n");
        });

        worker.start();
        frame.setVisible(true);
    }

    // 7. Update the six actual Java thread lifecycle states
    private void updateStatuses() {
        int[] counts = new int[6];

        for (int i = 0; i < threads.size(); i++) {
            Thread thread = threads.get(i);
            Thread.State state = thread.getState();

            threadModel.set(
                i, thread.getName() + " : " + state
            );

            counts[state.ordinal()]++;
        }

        for (int i = 0; i < states.length; i++) {
            stateLabels[i].setText(states[i] + " : " + counts[i]);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new VisualizerGUI().setVisible(true);
        });
    }
}
