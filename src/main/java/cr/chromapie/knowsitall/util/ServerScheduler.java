package cr.chromapie.knowsitall.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class ServerScheduler {

    private static final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    public static void schedule(Runnable task) {
        tasks.add(task);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Runnable task;
        while ((task = tasks.poll()) != null) {
            task.run();
        }
    }
}
