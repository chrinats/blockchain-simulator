package org.example;

import java.util.PriorityQueue;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

public class Scheduler {

    public static class Event implements Comparable<Event> {
        public int callerId;
        public int blockNumber;
        public long eventTime;
        public Node generatedBy;

        private static double calculateForkPercentage(Node[] nodes) {
            int totalForks = 0;
            for (Node node : nodes) {
                totalForks += node.getChain().size() - Sharables.ChainLength;
            }
            return (double)totalForks * 100 / Sharables.ChainLength;
        }

        public Event(int callerId, long eventTime, int blockNumber, Node generatedBy) {
            this.callerId = callerId;
            this.eventTime = eventTime;
            this.blockNumber = blockNumber;
            this.generatedBy = generatedBy;
        }

        @Override
        public int compareTo(Event other) {
            return Long.compare(this.eventTime, other.eventTime);
        }
    }

    private static PriorityQueue<Event> queue = new PriorityQueue<>();
    public static PriorityQueue<Event> kadcastQueue = new PriorityQueue<>();

    public static void init() {
        synchronized (queue) {
            queue.clear();
        }
    }

    public static void initKadcastQueue() {
        synchronized (kadcastQueue) {
            kadcastQueue.clear();
        }
    }

    public static synchronized void addEvent(int callerId, long eventTime, int blockNumber, Node generatedBy) {
        Event event = new Event(callerId, eventTime, blockNumber, generatedBy);
        synchronized (queue) {
            queue.add(event);
        }
    }

    public static synchronized void addKadcastEvent(long eventTime, int blockNumber, Node generatedBy) {
        Event event = new Event(generatedBy.getId(), eventTime, blockNumber, generatedBy);
        synchronized (kadcastQueue) {
            kadcastQueue.add(event);
        }
    }

    public static void start() {
        while (!queue.isEmpty()) {
            Event event;
            synchronized (queue) {
                event = queue.poll();
            }

            if (event != null) {
                event.generatedBy.CallBlack(event.eventTime, event.blockNumber);
            }
        }
    }

    public static void startKadcast() {
        Sharables.TotalPropagationTime = 0; // Reset πριν ξεκινήσει

        long maxTotalTime = 0;

        // Process all events in the Kadcast queue
        while (!kadcastQueue.isEmpty()) {
            Event event;
            synchronized (kadcastQueue) {
                event = kadcastQueue.poll();
            }

            if (event != null) {
                event.generatedBy.KadcastBroadcast(event.generatedBy);
                maxTotalTime+=Sharables.TotalPropagationTime;
            }
        }

        Sharables.AvgPropagationTime = maxTotalTime / Sharables.ChainLength;
    }
}
