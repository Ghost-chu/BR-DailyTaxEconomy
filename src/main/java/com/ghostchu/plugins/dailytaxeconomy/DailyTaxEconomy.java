package com.ghostchu.plugins.dailytaxeconomy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("deprecation")
public final class DailyTaxEconomy extends JavaPlugin {

    private static Economy econ = null;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        if (!setupEconomy()) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) return false;
        String playerName = args[0];
        String percentDouble = args[1];
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            sender.sendMessage("Target player is offline!");
            return false;
        }
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            double d = Double.parseDouble(percentDouble);
            double taxBalance = getTaxerTotalBalance();
            getLogger().info("System taxer account balance: "+taxBalance);
            double amount = d * taxBalance;
            BigDecimal bigDecimal = new BigDecimal(amount);
            bigDecimal = bigDecimal.setScale(2,RoundingMode.HALF_UP);
            amount = bigDecimal.doubleValue();

            Optional<String> transferResult = transferTaxerToPlayer(player, amount);
            if (!transferResult.isPresent()) {
                player.sendMessage(ChatColor.GREEN + "经济活跃奖励：" + ChatColor.YELLOW + amount);
                sender.sendMessage("Transfer " + amount + " to " + playerName + " successfully!");
            } else {
                player.sendMessage(ChatColor.RED + "出错了：" + ChatColor.YELLOW + transferResult.get());
                sender.sendMessage("Transfer " + amount + " to " + playerName + " failed: " + transferResult.get());
            }
        });
        return true;
    }


    public Optional<String> transferTaxerToPlayer(Player player, double amount) {
        String taxerSetting = getConfig().getString("taxer-vault", "Taxer");
        EconomyResponse withdrawResponse;
        if (!balanceIsEnough(amount)) {
            return Optional.of("系统税收账户余额不足");
        }
        if (Util.isUUID(taxerSetting)) {
            withdrawResponse = econ.withdrawPlayer(Bukkit.getOfflinePlayer(UUID.fromString(taxerSetting)), amount);
        } else {
            withdrawResponse = econ.withdrawPlayer(taxerSetting, amount);
        }
        if (!withdrawResponse.transactionSuccess()) return Optional.of(withdrawResponse.errorMessage);

        EconomyResponse depositResponse = econ.depositPlayer(player, amount);

        if (depositResponse.transactionSuccess()) {
            return Optional.empty();
        } else {
            return Optional.of(depositResponse.errorMessage);
        }
    }

    public boolean balanceIsEnough(double atLeast) {
        return getTaxerTotalBalance() >= atLeast;
    }

    public double getTaxerTotalBalance() {
        String taxerSetting = getConfig().getString("taxer-vault", "Taxer");
        if (Util.isUUID(taxerSetting)) {
            return econ.getBalance(Bukkit.getOfflinePlayer(UUID.fromString(taxerSetting)));
        } else {
            return econ.getBalance(taxerSetting);
        }
    }


    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

}
