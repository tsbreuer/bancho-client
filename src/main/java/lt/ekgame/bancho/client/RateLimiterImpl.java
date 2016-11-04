/*
 * Decompiled with CFR 0_115.
 */
package lt.ekgame.bancho.client;

import java.util.LinkedList;
import java.util.Queue;
import lt.ekgame.bancho.api.packets.Packet;

public class RateLimiterImpl {
    private int delay;
    private Queue<Packet> outgoing = new LinkedList<Packet>();
    private long lastSentTime = 0;

    public RateLimiterImpl(int delay) {
        this.delay = delay;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void sendPacket(Packet packet) {
        Queue<Packet> queue = this.outgoing;
        synchronized (queue) {
            this.outgoing.add(packet);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Packet getOutgoingPacket() {
        if (System.currentTimeMillis() - this.lastSentTime > (long)this.delay) {
            Queue<Packet> queue = this.outgoing;
            synchronized (queue) {
                Packet packet = this.outgoing.poll();
                if (packet != null) {
                    this.lastSentTime = System.currentTimeMillis();
                }
                return packet;
            }
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean hasQueuedPackets() {
        Queue<Packet> queue = this.outgoing;
        synchronized (queue) {
            return !this.outgoing.isEmpty();
        }
    }
}

