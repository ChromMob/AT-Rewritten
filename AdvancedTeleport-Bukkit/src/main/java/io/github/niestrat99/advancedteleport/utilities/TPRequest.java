package io.github.niestrat99.advancedteleport.utilities;

import com.github.puregero.multilib.MultiLib;
import io.github.niestrat99.advancedteleport.CoreClass;
import io.github.niestrat99.advancedteleport.config.CustomMessages;
import io.github.niestrat99.advancedteleport.config.NewConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class TPRequest {

    private static List<TPRequest> requestList = new ArrayList<>();
    private Player requester; // The player sending the request.
    private Player responder; // The player receiving it.
    private BukkitRunnable timer;
    private TeleportType type;

    public TPRequest(Player requester, Player responder, BukkitRunnable timer, TeleportType type) {
        this.requester = requester;
        this.responder = responder;
        this.timer = timer;
        this.type = type;
    }

    public static void registerListener() {
        MultiLib.onString(CoreClass.getInstance(), "io.github.niestrat99.advancedteleport", s -> {
            String[] split = s.split(" ");
            Player requester = Bukkit.getPlayer(split[0]);
            Player responder = Bukkit.getPlayer(split[1]);
            TeleportType type = TeleportType.valueOf(split[2]);
            if (requester == null || responder == null) return;
            if (MultiLib.isExternalPlayer(responder)) return;
            int requestLifetime = NewConfig.get().REQUEST_LIFETIME.get();
            BukkitRunnable run = new BukkitRunnable() {
                @Override
                public void run() {
                    TPRequest.removeRequest(TPRequest.getRequestByReqAndResponder(responder, requester));
                }
            };
            run.runTaskLater(CoreClass.getInstance(), requestLifetime * 20L); // 60 seconds
            addRequest(new TPRequest(requester, responder, run, type));
        });
    }

    public BukkitRunnable getTimer() {
        return timer;
    }

    public Player getRequester() {
        return requester;
    }

    public Player getResponder() {
        return responder;
    }

    public TeleportType getType() {
        return type;
    }

    public enum TeleportType {
        TPAHERE,
        TPA
    }

    public static List<TPRequest> getRequests(Player responder) {
        List<TPRequest> requests = new ArrayList<>();
        for (TPRequest request : requestList) {
            if (request.responder == responder) {
                requests.add(request);
            }
        }
        return requests;
    }

    public static List<TPRequest> getRequestsByRequester(Player requester) {
        List<TPRequest> requests = new ArrayList<>(); // Requests that the requester has pending
        for (TPRequest request : requestList) {
            if (request.getRequester() == requester) {
                requests.add(request);
            }
        }
        return requests;
    }

    public static TPRequest getRequestByReqAndResponder(Player responder, Player requester) {
        for (TPRequest request : requestList) {
            if (request.getRequester() == requester && request.getResponder() == responder) {
                return request;
            }
        }
        return null;
    }

    public static void addRequest(TPRequest request) {
        if (MultiLib.isExternalPlayer(request.getResponder())) {
            MultiLib.notify("io.github.niestrat99.advancedteleport", request.getRequester().getName() + " " + request.getResponder().getName() + " " + request.getType().toString());
            return;
        }
        if (!NewConfig.get().USE_MULTIPLE_REQUESTS.get()) {
            for (TPRequest otherRequest : getRequests(request.responder)) {
                if (NewConfig.get().NOTIFY_ON_EXPIRE.get()) {
                    if (MultiLib.isLocalPlayer(otherRequest.getResponder())) {
                        CustomMessages.sendMessage(otherRequest.requester, "Info.requestDisplaced", "{player}", request.responder.getName());
                    }
                }
                otherRequest.destroy();
            }
        }
        requestList.add(request);
    }

    public static void removeRequest(TPRequest request) {
        requestList.remove(request);
    }

    public void destroy() {
        timer.cancel();
        removeRequest(this);
    }

}
