package third_party;

/**
 * Created by usta on 10.10.2016.
 */

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

public class GifExporterFX {

    private final ExecutorService threadPollWorker = Executors.newWorkStealingPool(10);
    private final ScheduledExecutorService scheduleWorker = Executors.newSingleThreadScheduledExecutor();
    private BufferedImage latestBufferedImage;
    private final Semaphore uiSemaphore = new Semaphore(1);
    private final ExecutorService singleExecutor = Executors.newSingleThreadExecutor();


    public ScheduledFuture<?> captureNow(Scene target, Path outputDirectory, int timeBetweenFramesMS, boolean loopContinuously) throws Exception {
        ImageOutputStream output = new FileImageOutputStream(outputDirectory.toFile());
        GifSequenceWriter gifWriter = new GifSequenceWriter(output, 3, timeBetweenFramesMS, loopContinuously);

        ScheduledFuture<?> future = scheduleWorker.scheduleWithFixedDelay(() -> {

            Semaphore semaphore = new Semaphore(1);

            int w = (int) target.getWidth();
            int h = (int) target.getHeight();
            WritableImage img = new WritableImage(w, h);

            runActionLater(() -> {
                try {
                    target.snapshot(img);
                    BufferedImage bufferedImage = SwingFXUtils.fromFXImage(img, null);
                    runTaskLater(() -> {
                        boolean isSame = isSameImage(bufferedImage, latestBufferedImage);
                        latestBufferedImage = bufferedImage;
                        if (!isSame) {
                            try {
                                gifWriter.writeToSequence(bufferedImage);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    semaphore.release();
                } catch (Exception ex) {
                    Logger.getLogger(GifExporterFX.class.getName()).log(Level.SEVERE, null, ex);
                }
            });

            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }, 0, timeBetweenFramesMS, TimeUnit.MILLISECONDS);

        start(() -> {
            try {
                future.get();
                gifWriter.close();
                output.close();
            } catch (Exception e) {
            }
        });

        return future;
    }

    public void start(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void runActionLater(final Runnable runnable) {
        if (Platform.isFxApplicationThread()) {
            runnable.run();
        } else {
            try {
                uiSemaphore.acquire();
                Platform.runLater(() -> {
                    try {
                        runnable.run();
                        releaseUiSemaphore();
                    } catch (Exception e) {
                        releaseUiSemaphore();
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                releaseUiSemaphore();
                throw new RuntimeException(e);
            }
        }
    }

    private void releaseUiSemaphore() {
        singleExecutor.submit((Runnable) uiSemaphore::release);
    }

    private <T> Future<?> runTaskLater(Runnable runnable) {

        Task<T> task = new Task<T>() {
            @Override
            protected T call() throws Exception {
                runnable.run();
                return null;
            }
        };

        task.exceptionProperty().addListener((observable, oldValue, newValue) -> {
            if (Objects.nonNull(newValue)) {
                newValue.printStackTrace();
            }
        });

        return threadPollWorker.submit(task);
    }

    public static boolean isSameImage(BufferedImage firstImage, BufferedImage secondImage) {

        if (Objects.isNull(firstImage)) {
            return false;
        }

        if (Objects.isNull(secondImage)) {
            return false;
        }

        // The images must be the same size.
        if (firstImage.getWidth() == secondImage.getWidth() && firstImage.getHeight() == secondImage.getHeight()) {
            int width = firstImage.getWidth();
            int height = firstImage.getHeight();

            // Loop over every pixel.
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // Compare the pixels for equality.
                    if (firstImage.getRGB(x, y) != secondImage.getRGB(x, y)) {
                        return false;
                    }
                }
            }
        } else {
            return false;
        }

        return true;
    }
}