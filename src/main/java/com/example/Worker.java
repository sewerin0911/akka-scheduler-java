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

public class Worker extends AbstractBehavior<Worker.Request> {

    private ActorRef<Scheduler.Request> scheduler;

    public static Behavior<Worker.Request> create(ActorRef<Scheduler.Request> scheduler) {
        return Behaviors.setup(context -> new Worker(context, scheduler));
    }

    public Worker(ActorContext<Request> context, ActorRef<Scheduler.Request> scheduler) {
        super(context);
        this.scheduler = scheduler;
    }

    public interface Request {
    }

    /**
     *
     */
    public static final class TaskAssigned implements Request {
        private int calculatedNumber;
        private int pos;
        private ActorRef<Task.Response> sender;

        // this worker is assigned to position pos in original task list
        // and is given the corresponding number at that position
        public TaskAssigned(ActorRef<Task.Response> sender, int number, int pos) {
            this.sender = sender;
            this.calculatedNumber = number * 2;
            this.pos = pos;
        }
    }

    @Override
    public Receive<Request> createReceive() {
        return newReceiveBuilder()
                .onMessage(Worker.TaskAssigned.class, this::onTaskAssigned)
                .build();
    }

    /**
     * After the calculation the worker sends it back to task.
     *
     * @param response
     * @return
     */
    private Behavior<Worker.Request> onTaskAssigned(TaskAssigned response) {
        scheduler.tell(new Scheduler.WorkerDone(this.getContext().getSelf(), response.sender));
        response.sender.tell(new Task.WorkerSend(
                this.getContext().getSelf(), response.calculatedNumber, response.pos));
        return Behaviors.stopped();
    }
}
