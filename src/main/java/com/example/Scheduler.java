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
    private Queue<ActorRef<Task.Response>> tasksQueue = new LinkedList<>();
    private Queue<Integer> amountQueue = new LinkedList<>();
    private int workersCount = 1;

    public static Behavior<Scheduler.Request> create() {
        return Behaviors.setup(context -> new Scheduler(context));
    }

    public Scheduler(ActorContext<Request> context) {
        super(context);
    }

    public interface Request {
    }

    public static final class WorkersDemand implements Request {
        ActorRef<Task.Response> sender;
        int amount;

        public WorkersDemand(ActorRef<Task.Response> sender, int amount) {
            this.sender = sender;
            this.amount = amount;
        }
    }

    public static final class WorkerDone implements Request {
        ActorRef<Worker.Request> sender;
        ActorRef<Task.Response> ofWhich;

        public WorkerDone(ActorRef<Worker.Request> sender, ActorRef<Task.Response> ofWhich) {
            this.sender = sender;
            this.ofWhich = ofWhich;
        }
    }

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

    // create a new array with amount (argument) new workers
    private ArrayList<ActorRef<Worker.Request>> createWorkers(int amount) {
        ArrayList<ActorRef<Worker.Request>> newWorkers = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            newWorkers.add(getContext().spawn(Worker.create(this.getContext().getSelf()), "Worker" + workersCount));
            workersCount++;
        }
        return newWorkers;
    }

    // print current queue
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

    private Behavior<Scheduler.Request> onWorkersDemand(WorkersDemand request) {
        getContext().getLog().info(
                "{} asks {} for {} workers.", request.sender, this.getContext().getSelf(), request.amount);
        // if enough empty slots available
        if (request.amount <= emptySlots) {
            emptySlots -= request.amount;
            // empty slots after the "reservation"
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
            amountQueue.add(request.amount);
            // print current queue
            getContext().getLog().info(currentQueue());
        }
        return this;
    }

    private Behavior<Scheduler.Request> onWorkerDone(WorkerDone response) {
        getContext().getLog().info("{} has done their assignment in {} and is now terminated.", response.sender, response.ofWhich);
        emptySlots += 1;
        // check queue everytime after a worker has done their task,
        // so that the next task can be assigned asap with new workers
        if (!tasksQueue.isEmpty() && amountQueue.peek() <= emptySlots) {
            getContext().getLog().info(
                    "There are {} empty slots, so {} is now out of queue.", emptySlots, tasksQueue.peek());
            this.emptySlots -= amountQueue.peek();
            tasksQueue.remove().tell(new Task.WorkersAssigned(
                    this.getContext().getSelf(), createWorkers(amountQueue.remove()), emptySlots
            ));
            // after updating the queue, print current queue
            getContext().getLog().info(currentQueue());
        }
        return this;
    }

    private Behavior<Scheduler.Request> onTaskDone(TaskDone request) {
        getContext().getLog().info("{} has been completed.", request.sender);
        return this;
    }
}

