package io.github.niestrat99.advancedteleport.commands.home;

import io.github.niestrat99.advancedteleport.config.CustomMessages;
import io.github.niestrat99.advancedteleport.config.Homes;
import io.github.niestrat99.advancedteleport.config.Config;
import io.github.niestrat99.advancedteleport.CoreClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.io.IOException;
import java.util.UUID;

public class SetHome implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (Config.isFeatureEnabled("homes")) {
            if (sender instanceof Player) {
                Player player = (Player)sender;
                if (sender.hasPermission("at.member.sethome")) {
                    Bukkit.getScheduler().runTaskAsynchronously(CoreClass.getInstance(), () -> {
                        if (args.length>0) {
                            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
                            if (target != null) {
                                if (sender.hasPermission("at.admin.sethome")) {
                                    // We'll just assume that the admin command overrides the homes limit.
                                    if (args.length>1) {
                                        if (args[1].matches("^[A-Za-z0-9]+$")) {
                                            setHome(player, target.getUniqueId(), args[1], args[0]);
                                        } else {
                                            sender.sendMessage(CustomMessages.getString("Error.invalidName"));
                                        }
                                        return;
                                    }
                                }
                            }
                            // I don't really want to run this method twice if a player has a lot of permissions, so store it as an int
                            int limit = getHomesLimit(player);

                            // If the number of homes a player has is smaller than or equal to the homes limit, or they have a bypass permission
                            if (Homes.getHomes(player.getUniqueId().toString()).size() < limit
                                    || player.hasPermission("at.admin.sethome.bypass")
                                    || limit == -1) {
                                if (args[0].matches("^[A-Za-z0-9]+$")) {
                                    setHome(player, args[0]);
                                } else {
                                    sender.sendMessage(CustomMessages.getString("Error.invalidName"));
                                }

                            } else {
                                sender.sendMessage(CustomMessages.getString("Error.reachedHomeLimit"));
                            }

                        } else {
                            int limit = getHomesLimit(player);
                            if (Homes.getHomes(player.getUniqueId().toString()).size() == 0 && (limit > 0 || limit == -1)) {
                                setHome(player, "home");
                            } else {
                                sender.sendMessage(CustomMessages.getString("Error.noHomeInput"));
                            }
                        }
                    });
                }
            } else {
                sender.sendMessage(CustomMessages.getString("Error.notAPlayer"));
            }
        } else {
            sender.sendMessage(CustomMessages.getString("Error.featureDisabled"));
        }
        return true;
    }

    private void setHome(Player sender, String name) {
        setHome(sender, sender.getUniqueId(), name, sender.getName());
    }

    // Separated this into a separate method so that the code is easier to read.
    // Player player - the player which is having the home set.
    // String name - the name of the home.
    private void setHome(Player sender, UUID player, String homeName, String playerName) {
        Location home = sender.getLocation();
        try {
            if (Homes.getHomes(player.toString()).containsKey(homeName)) {
                sender.sendMessage(CustomMessages.getString("Error.homeAlreadySet").replaceAll("\\{home}", homeName));
            } else {
                try {
                    Homes.setHome(player.toString(), homeName, home);
                    if (sender.getUniqueId() == player) {
                        sender.sendMessage(CustomMessages.getString("Info.setHome").replaceAll("\\{home}", homeName));
                    } else {
                        sender.sendMessage(CustomMessages.getString("Info.setHomeOther").replaceAll("\\{home}", homeName).replaceAll("\\{player}", playerName));
                    }

                } catch (IOException e) {
                    e.getStackTrace();
                }
            }
        } catch (NullPointerException ex) {
            try {
                Homes.setHome(player.toString(), homeName,home);
                if (sender.getUniqueId() == player) {
                    sender.sendMessage(CustomMessages.getString("Info.setHome").replaceAll("\\{home}", homeName));
                } else {
                    sender.sendMessage(CustomMessages.getString("Info.setHomeOther").replaceAll("\\{home}", homeName).replaceAll("\\{player}", playerName));
                }
            } catch (IOException e) {
                e.getStackTrace();
            }
        }
    }

    // Used to get the permission for how many homes a player can have.
    // If there is no permission, then it's assumed that the number of homes they can have is limitless (-1).
    // E.g.: at.member.homes.5
    // at.member.homes.40
    // at.member.homes.100000
    private int getHomesLimit(Player player) {
        int maxHomes = Config.getDefaultHomesLimit();
        for (PermissionAttachmentInfo permission : player.getEffectivePermissions()) {
            if (permission.getPermission().startsWith("at.member.homes.")) {
                if (permission.getValue()) {
                    String perm = permission.getPermission();
                    String ending = perm.substring(perm.lastIndexOf(".") + 1);
                    if (!ending.matches("^[0-9]+$")) continue;
                    int homes = Integer.parseInt(ending);
                    if (maxHomes < homes) {
                        maxHomes = homes;
                    }
                }
            }
        }
        return maxHomes;
    }
}
