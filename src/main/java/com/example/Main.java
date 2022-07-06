package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Random;

public class Main extends AbstractBehavior<Main.StartMessage> {
    public static class StartMessage {
    }

    ActorRef<Scheduler.Request> scheduler;

    ActorRef<Task.Response> task1;
    ActorRef<Task.Response> task2;
    ActorRef<Task.Response> task3;
    ActorRef<Task.Response> task4;
    ActorRef<Task.Response> task5;
    ActorRef<Task.Response> task6;
    ActorRef<Task.Response> task7;
    ActorRef<Task.Response> task8;
    ActorRef<Task.Response> task9;
    ActorRef<Task.Response> task10;

    public static Behavior<StartMessage> create() {
        return Behaviors.setup(Main::new);
    }

    private Main(ActorContext<StartMessage> context) {
        super(context);
    }

    @Override
    public Receive<StartMessage> createReceive() {
        return newReceiveBuilder().onMessage(StartMessage.class, this::onStartMessage).build();
    }

    private Behavior<StartMessage> onStartMessage(StartMessage command) throws InterruptedException {
        scheduler = getContext().spawn(Scheduler.create(), "Scheduler");
        task1 = getContext().spawn(Task.create(scheduler, new Random().nextInt(4, 11)), "Task1");
        task2 = getContext().spawn(Task.create(scheduler, new Random().nextInt(4, 11)), "Task2");
        task3 = getContext().spawn(Task.create(scheduler, new Random().nextInt(4, 11)), "Task3");
        task4 = getContext().spawn(Task.create(scheduler, new Random().nextInt(4, 11)), "Task4");
        task5 = getContext().spawn(Task.create(scheduler, new Random().nextInt(4, 11)), "Task5");
        task6 = getContext().spawn(Task.create(scheduler, new Random().nextInt(4, 11)), "Task6");
        task7 = getContext().spawn(Task.create(scheduler, new Random().nextInt(4, 11)), "Task7");
        task8 = getContext().spawn(Task.create(scheduler, new Random().nextInt(4, 11)), "Task8");
        task9 = getContext().spawn(Task.create(scheduler, new Random().nextInt(4, 11)), "Task9");
        task10 = getContext().spawn(Task.create(scheduler, new Random().nextInt(4, 11)), "Task10");
        return this;
    }
}