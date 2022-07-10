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
import java.util.Random;

public class Task extends AbstractBehavior<Task.Response> {
    private final ArrayList<Integer> oldTaskList;
    private final ArrayList<Integer> taskList = new ArrayList<>();
    private ArrayList<ActorRef<Worker.Request>> assignedWorkers;
    private final ActorRef<Scheduler.Request> scheduler;
    private final int workersCount;

    public static Behavior<Task.Response> create(ActorRef<Scheduler.Request> scheduler, int cap) {
        return Behaviors.setup(context -> new Task(context, scheduler, cap));
    }

    private Task(ActorContext<Task.Response> context, ActorRef<Scheduler.Request> scheduler, int cap) {
        super(context);
        this.scheduler = scheduler;
        // generate random integers from 0 - 10 for elements in task list
        for (int i = 0; i < cap; i++) {
            // generate a random integer from 0 - (bound - 1)
            taskList.add(new Random().nextInt(11));
        }
        oldTaskList = (ArrayList<Integer>) taskList.clone();
        workersCount = taskList.size();
        getContext().getLog().info("Task list of {} is: {}.",
                this.getContext().getSelf(), this.taskList);
        scheduler.tell(new Scheduler.WorkersDemand(this.getContext().getSelf(), workersCount));
    }

    public interface Response {
    }

    /**
     *
     */
    public static final class WorkersAssigned implements Response {
        ActorRef<Scheduler.Request> sender;
        ArrayList<ActorRef<Worker.Request>> assignedWorkers;
        int emptySlots;

        public WorkersAssigned(ActorRef<Scheduler.Request> sender,
                               ArrayList<ActorRef<Worker.Request>> assignedWorkers, int emptySlots) {
            this.sender = sender;
            this.assignedWorkers = assignedWorkers;
            this.emptySlots = emptySlots;
        }
    }

    /**
     * When worker has done its job, it sends a new calculated number to corresponding task.
     */
    public static final class WorkerSend implements Response {
        ActorRef<Worker.Request> sender;
        int calculatedNumber;
        int pos;

        public WorkerSend(ActorRef<Worker.Request> sender, int calculatedNumber, int pos) {
            this.sender = sender;
            this.calculatedNumber = calculatedNumber;
            this.pos = pos;
        }
    }

    @Override
    public Receive<Response> createReceive() {
        return newReceiveBuilder()
                .onMessage(Task.WorkersAssigned.class, this::onWorkersAssigned)
                .onMessage(WorkerSend.class, this::onWorkerSend)
                .build();
    }

    /**
     *
     *
     * @param response
     * @return
     */
    private Behavior<Response> onWorkersAssigned(WorkersAssigned response) {
        getContext().getLog().info("{} workers were assigned to {}",
                response.assignedWorkers.size(), this.getContext().getSelf());
        this.assignedWorkers = response.assignedWorkers;
        // assign workers to each position individually
        for (int i = 0; i < this.assignedWorkers.size(); i++) {
            response.assignedWorkers.get(i).tell(
                    new Worker.TaskAssigned(this.getContext().getSelf(), taskList.get(i), i));
        }
        return this;
    }

    /**
     *
     *
     * @param response
     * @return
     */
    private Behavior<Response> onWorkerSend(WorkerSend response) {
        // replace the number at pos with new calculated number
        // given from the worker
        taskList.set(response.pos, response.calculatedNumber);
        // then it is deleted from the workers list
        assignedWorkers.remove(response.sender);
        // when all workers have done their tasks
        if (assignedWorkers.size() == 0) {
            this.scheduler.tell(new Scheduler.TaskDone(this.getContext().getSelf(), workersCount));
            // print the old list
            getContext().getLog().info("Old task list of {} is: {}.",
                    this.getContext().getSelf(), this.oldTaskList.toString());
            // print the new list
            getContext().getLog().info("New task list of {} is: {}.",
                    this.getContext().getSelf(), this.taskList);
            return Behaviors.stopped();
        } else {
            return this;
        }
    }
}
