package com.example;

import akka.actor.typed.ActorSystem;

import java.io.IOException;
public class AkkaSchedulerStart {
  public static void main(String[] args) {
    //#actor-system
    final ActorSystem<Main.StartMessage> main = ActorSystem.create(Main.create(), "mainScheduler");
    //#actor-system

    //#main-send-messages
    main.tell(new Main.StartMessage());
    //#main-send-messages

    try {
      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    } catch (IOException ignored) {
    } finally {
      main.terminate();
    }
  }
}
