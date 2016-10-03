package lt.ekgame.bancho.client;

import lt.ekgame.bancho.api.packets.Packet;
import lt.ekgame.bancho.api.packets.client.PacketCreateRoom;
import lt.ekgame.bancho.api.packets.client.PacketLeaveRoom;
import lt.ekgame.bancho.api.packets.client.PacketRoomFinishMap;
import lt.ekgame.bancho.api.packets.client.PacketRoomMapDoneLoading;
import lt.ekgame.bancho.api.packets.client.PacketRoomReady;
import lt.ekgame.bancho.api.packets.client.PacketRoomStartGame;
import lt.ekgame.bancho.api.packets.client.PacketRoomUnready;
import lt.ekgame.bancho.api.packets.client.PacketSignalMultiplayer;
import lt.ekgame.bancho.api.packets.client.PacketUpdateRoom;
import lt.ekgame.bancho.api.packets.server.PacketRoomEveryoneFinished;
import lt.ekgame.bancho.api.packets.server.PacketRoomEveryoneLoaded;
import lt.ekgame.bancho.api.packets.server.PacketRoomJoined;
import lt.ekgame.bancho.api.packets.server.PacketRoomUpdate;
import lt.ekgame.bancho.api.units.Beatmap;
import lt.ekgame.bancho.api.units.MatchSpecialMode;
import lt.ekgame.bancho.api.units.Mods;
import lt.ekgame.bancho.api.units.MultiplayerRoom;
import lt.ekgame.bancho.api.units.UserStatus;

public class MultiplayerHandler implements PacketHandler {
    private ClientHandler clientHandler;
    private MultiplayerRoom currentRoom;
    private BanchoClient bancho;
    private String roomPassword;
    private boolean enabledMultiplayer = false;
    private boolean isReady = false;

    public MultiplayerHandler(BanchoClient bancho, ClientHandler clientHandler) {
        this.bancho = bancho;
        this.clientHandler = clientHandler;
    }

    public void enableMultiplayer() {
        if (!this.enabledMultiplayer) {
            this.enabledMultiplayer = true;
            this.bancho.sendPacket(new PacketSignalMultiplayer());
            this.clientHandler.setStatus(UserStatus.LOBBY);
            this.clientHandler.sendStatusUpdate();
        }
    }

    public void createRoom(String roomname, String password, int openSlots) {
        if (this.currentRoom != null) {
            return;
        }
        this.enableMultiplayer();
        this.roomPassword = password;
        MultiplayerRoom room = new MultiplayerRoom(roomname, password, openSlots, this.clientHandler.getCurrentBeatmap(), this.clientHandler.getMods(), this.clientHandler.getUserId());
        this.bancho.sendPacket(new PacketCreateRoom(room));
    }

    public boolean isHost() {
        if (this.currentRoom != null && this.currentRoom.hostId == this.clientHandler.getUserId()) {
            return true;
        }
        return false;
    }

    public void setReady(boolean ready) {
        this.isReady = ready;
        if (this.isReady) {
            this.bancho.sendPacket(new PacketRoomReady());
        } else {
            this.bancho.sendPacket(new PacketRoomUnready());
        }
    }

    public void startGame() {
        if (this.isHost()) {
            this.setReady(true);
            this.bancho.sendPacket(new PacketRoomStartGame());
            this.bancho.sendPacket(new PacketRoomMapDoneLoading());
            this.clientHandler.setCurrentBeatmap(this.currentRoom.getBeatmap());
            this.clientHandler.sendStatusUpdate();
        }
    }

    @Override
    public void handle(Packet packet) {
        if (packet instanceof PacketRoomUpdate && this.isHost()) {
            PacketRoomUpdate update = (PacketRoomUpdate)packet;
            if (update.room.matchId == this.getMatchId()) {
                this.currentRoom = update.room;
            }
        }
        if (packet instanceof PacketRoomJoined) {
            PacketRoomJoined roomUpdate = (PacketRoomJoined)packet;
            this.currentRoom = roomUpdate.room;
            this.clientHandler.setStatus(UserStatus.MULTIPLAYER);
            this.clientHandler.sendStatusUpdate();
        }
        if (packet instanceof PacketRoomEveryoneLoaded) {
            this.bancho.sendPacket(new PacketRoomFinishMap());
            this.clientHandler.setStatus(UserStatus.MULTIPLAYING);
            this.clientHandler.sendStatusUpdate();
        }
        if (packet instanceof PacketRoomEveryoneFinished) {
            this.clientHandler.setStatus(UserStatus.MULTIPLAYER);
            this.clientHandler.sendStatusUpdate();
            this.setReady(false);
        }
    }

    public void leaveRoom() {
        this.bancho.sendPacket(new PacketLeaveRoom());
        this.currentRoom = null;
        this.isReady = false;
    }

    public void setRoomName(String newName) {
        if (this.isHost()) {
            this.currentRoom.roomName = newName;
            this.sendRoomUpdate();
        }
    }

    public void setBeatmap(Beatmap beatmap) {
        if (this.isHost()) {
            this.currentRoom.setBeatmap(beatmap == null ? Beatmap.DEFAULT : beatmap);
            this.sendRoomUpdate();
        }
    }

    private void sendRoomUpdate() {
        if (this.isHost()) {
            this.bancho.sendPacket(new PacketUpdateRoom(this.currentRoom));
        }
    }

    public String getRoomPassword() {
        return this.roomPassword;
    }

    public int getMatchId() {
        return this.currentRoom == null ? -1 : this.currentRoom.matchId;
    }

    public boolean isReady() {
        return this.isReady;
    }

    public boolean isFreeModsEnabled() {
        if (this.currentRoom != null && this.currentRoom.specialMode == MatchSpecialMode.FREE_MOD) {
            return true;
        }
        return false;
    }

    public void setFreeMods(boolean enabled) {
        if (this.isHost()) {
            this.currentRoom.specialMode = enabled ? MatchSpecialMode.FREE_MOD : MatchSpecialMode.NONE;
            this.sendRoomUpdate();
        }
    }

    public MultiplayerRoom getRoom() {
        return this.currentRoom;
    }

    public void setMods(int mods) {
        if (this.isHost()) {
            this.currentRoom.mods = new Mods(mods);
            this.sendRoomUpdate();
        }
    }
}
