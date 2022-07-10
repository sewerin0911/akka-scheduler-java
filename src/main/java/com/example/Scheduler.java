// Sewerin Kuss     201346
// Duc Ahn Le       230662
// Janis Melon      209928

package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class Scheduler extends AbstractBehavior<Scheduler.Request> {
    private int emptySlots = 10;
    private final Queue<ActorRef<Task.Response>> tasksQueue = new LinkedList<>();
    private final Queue<Integer> workerQueue = new LinkedList<>();
    private int workersCount = 1;

    public static Behavior<Scheduler.Request> create() {
        return Behaviors.setup(Scheduler::new);
    }

    public Scheduler(ActorContext<Request> context) {
        super(context);
    }

    public interface Request {
    }

    /**
     * This message is sent when a task asks for workers.
     */
    public static final class WorkersDemand implements Request {
        ActorRef<Task.Response> sender;
        int amount;

        public WorkersDemand(ActorRef<Task.Response> sender, int amount) {
            this.sender = sender;
            this.amount = amount;
        }
    }

    /**
     * This message is sent when a worker has done its task.
     */
    public static final class WorkerDone implements Request {
        ActorRef<Worker.Request> sender;
        ActorRef<Task.Response> ofWhich;

        public WorkerDone(ActorRef<Worker.Request> sender, ActorRef<Task.Response> ofWhich) {
            this.sender = sender;
            this.ofWhich = ofWhich;
        }
    }

    /**
     * This message is sent when a task has been completed.
     */
    public static final class TaskDone implements Request {
        ActorRef<Task.Response> sender;
        int amount;

        public TaskDone(ActorRef<Task.Response> sender, int amount) {
            this.sender = sender;
            this.amount = amount;
        }
    }

    @Override
    public Receive<Request> createReceive() {
        return newReceiveBuilder()
                .onMessage(WorkersDemand.class, this::onWorkersDemand)
                .onMessage(TaskDone.class, this::onTaskDone)
                .onMessage(WorkerDone.class, this::onWorkerDone)
                .build();
    }

    /**
     * Create a new array with the length of the given number
     * and consists of new workers.
     *
     * @param amount is the requested amount of workers.
     * @return the new array with new workers.
     */
    private ArrayList<ActorRef<Worker.Request>> createWorkers(int amount) {
        ArrayList<ActorRef<Worker.Request>> newWorkers = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            newWorkers.add(getContext().spawn(Worker.create(this.getContext().getSelf()), "Worker" + workersCount));
            workersCount++;
        }
        return newWorkers;
    }

    /**
     * Prints current queue.
     *
     * @return A string with current queue or no queue.
     */
    private String currentQueue() {
        String s;
        if (tasksQueue.isEmpty()) {
            s = "There is no task left in queue.";
        } else {
            s = "The current queue is: ";
            for (ActorRef<Task.Response> t : tasksQueue) {
                s += t.path().name() + " ";
            }
        }
        return s;
    }

    /**
     * If there are enough workers available, assigns the task with new workers
     * and tells us how many empty slots left are free,
     * so that new tasks can be assigned.
     * And if they ask for too many workers, these will be queued into a waiting queue.
     *
     * @param request is the requesting message of a task.
     */
    private Behavior<Scheduler.Request> onWorkersDemand(WorkersDemand request) {
        getContext().getLog().info(
                "{} asks {} for {} workers.", request.sender, this.getContext().getSelf(), request.amount);
        // if enough empty slots available
        if (request.amount <= emptySlots) {
            emptySlots -= request.amount;
            // show empty slots after the "reservation"
            if (emptySlots > 1) {
                getContext().getLog().info("There are {} empty slots left.", emptySlots);
            } else {
                getContext().getLog().info("There is {} empty slot left.", emptySlots);
            }
            // the actual workers assignment
            request.sender.tell(
                    new Task.WorkersAssigned(this.getContext().getSelf(), createWorkers(request.amount), emptySlots));
        }
        // if not enough empty slots for the current task
        else {
            // put it in queue
            getContext().getLog().info("Not enough workers available, {} is now in queue.", request.sender);
            tasksQueue.add(request.sender);
            workerQueue.add(request.amount);
            // print current queue
            getContext().getLog().info(currentQueue());
        }
        return this;
    }

    /**
     * When a worker has done its job, increase the amount of available slots
     * and then check the queue task.
     * When there are still task(s) in the queue, check the amount of requested workers
     * of the first task in queue to see, if the current empty slots are enough for
     * this task to be out of queue and be assigned with new workers.
     * Otherwise, the queue remains the same until the next time it's getting checked.
     *
     * @param response is the message that a worker sends when their task is done.
     */
    private Behavior<Scheduler.Request> onWorkerDone(WorkerDone response) {
        getContext().getLog().info("{} has done their assignment in {} and is now terminated.", response.sender, response.ofWhich);
        emptySlots += 1;
        // check queue everytime after a worker has done their task,
        // so that the next task can be assigned asap with new workers
        if (!tasksQueue.isEmpty() && workerQueue.peek() <= emptySlots) {
            getContext().getLog().info(
                    "There are {} empty slots, so {} is now out of queue.", emptySlots, tasksQueue.peek());
            this.emptySlots -= workerQueue.peek();
            tasksQueue.remove().tell(new Task.WorkersAssigned(
                    this.getContext().getSelf(), createWorkers(workerQueue.remove()), emptySlots
            ));
            // after updating the queue, print current queue
            getContext().getLog().info(currentQueue());
        }
        return this;
    }

    /**
     * Shows a message when a task is done.
     *
     * @param request is sent when a task is done.
     */
    private Behavior<Scheduler.Request> onTaskDone(TaskDone request) {
        getContext().getLog().info("{} has been completed.", request.sender);
        return this;
    }
}

