package io.github.nagare.logging.example;

import io.github.nagare.logging.log4j.HttpAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.Random;

/**
 * It will run for 2 mins, and produce one log event per second.
 * The log level and message of this event is to be randomised.
 * The main purpose of this class is to test the MBean.
 * Test with command below:
 * mvn exec:java -Dexec.mainClass="io.github.nagare.logging.example.LogRunner_HttpAppender"
 */
public class LogRunner_HttpAppender {

    private static final Logger logger = Logger.getLogger(LogRunner_HttpAppender.class);
    private static final Random random = new Random();
    private static final Level[] LEVELS = {
            Level.TRACE,
            Level.DEBUG,
            Level.INFO,
            Level.WARN,
            Level.ERROR,
            Level.FATAL
    };
    private static final long DURATION = 2 * 60 * 1000; // 2 minutes

    public static void main(String[] args){
        logger.setLevel(Level.ALL); // default is DEBUG
        HttpAppender httpAppender = new HttpAppender();
        httpAppender.setName("LogRunner");
        logger.addAppender(httpAppender);

        long start = System.currentTimeMillis();
        long count = 0;
        while (System.currentTimeMillis() - start < DURATION) {
            Level randomLevel = LEVELS[random.nextInt(LEVELS.length)];
            String randomMessage = "Randomised Message: " + random.nextInt(1000);
            logger.log(randomLevel, randomMessage);

            count++;
            if (count >= 120) {
                break;
            }

            // if you want to see 120 logs within 2 mins, not "one log" event per "one second", comment out below
            long next = start + (count * 1000);
            long sleep = next - System.currentTimeMillis();
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    System.err.println("Sleep interrupted, continue logging");
                }
            }
        }
    }

}
