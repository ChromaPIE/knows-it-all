package cr.chromapie.knowsitall.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class ServerScheduler {

    private static final Queue<Runnable> serverTasks = new ConcurrentLinkedQueue<>();
    private static final Queue<Runnable> clientTasks = new ConcurrentLinkedQueue<>();

    public static void schedule(Runnable task) {
        serverTasks.add(task);
    }

    public static void scheduleClient(Runnable task) {
        clientTasks.add(task);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Runnable task;
        while ((task = serverTasks.poll()) != null) {
            task.run();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Runnable task;
        while ((task = clientTasks.poll()) != null) {
            task.run();
        }
    }
}
