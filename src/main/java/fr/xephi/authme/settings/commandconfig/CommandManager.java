package fr.xephi.authme.settings.commandconfig;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.resource.YamlFileResource;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.GeoIpService;
import fr.xephi.authme.util.FileUtils;
import fr.xephi.authme.util.PlayerUtils;
import fr.xephi.authme.util.lazytags.Tag;
import fr.xephi.authme.util.lazytags.WrappedTagReplacer;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static fr.xephi.authme.util.lazytags.TagBuilder.createTag;

/**
 * Manages configurable commands to be run when various events occur.
 */
public class CommandManager implements Reloadable {

    private final File dataFolder;
    private final BukkitService bukkitService;
    private final GeoIpService geoIpService;
    private final CommandMigrationService commandMigrationService;
    private final List<Tag<Player>> availableTags = buildAvailableTags();

    private WrappedTagReplacer<Command, Player> onJoinCommands;
    private WrappedTagReplacer<Command, Player> onLoginCommands;
    private WrappedTagReplacer<Command, Player> onRegisterCommands;

    @Inject
    CommandManager(@DataFolder File dataFolder, BukkitService bukkitService, GeoIpService geoIpService,
                   CommandMigrationService commandMigrationService) {
        this.dataFolder = dataFolder;
        this.bukkitService = bukkitService;
        this.geoIpService = geoIpService;
        this.commandMigrationService = commandMigrationService;
        reload();
    }

    /**
     * Runs the configured commands for when a player has joined.
     *
     * @param player the joining player
     */
    public void runCommandsOnJoin(Player player) {
        executeCommands(player, onJoinCommands.getAdaptedItems(player));
    }

    /**
     * Runs the configured commands for when a player has successfully registered.
     *
     * @param player the player who has registered
     */
    public void runCommandsOnRegister(Player player) {
        executeCommands(player, onRegisterCommands.getAdaptedItems(player));
    }

    /**
     * Runs the configured commands for when a player has logged in successfully.
     *
     * @param player the player that logged in
     */
    public void runCommandsOnLogin(Player player) {
        executeCommands(player, onLoginCommands.getAdaptedItems(player));
    }

    private void executeCommands(Player player, List<Command> commands) {
        for (Command command : commands) {
            final String execution = command.getCommand().replace("%p", player.getName());
            if (Executor.CONSOLE.equals(command.getExecutor())) {
                bukkitService.dispatchConsoleCommand(execution);
            } else {
                bukkitService.dispatchCommand(player, execution);
            }
        }
    }

    @Override
    public void reload() {
        File file = new File(dataFolder, "commands.yml");
        FileUtils.copyFileFromResource(file, "commands.yml");

        SettingsManager settingsManager = new SettingsManager(
            new YamlFileResource(file), commandMigrationService, CommandSettingsHolder.class);
        CommandConfig commandConfig = settingsManager.getProperty(CommandSettingsHolder.COMMANDS);
        onJoinCommands = newReplacer(commandConfig.getOnJoin());
        onLoginCommands = newReplacer(commandConfig.getOnLogin());
        onRegisterCommands = newReplacer(commandConfig.getOnRegister());
    }

    private WrappedTagReplacer<Command, Player> newReplacer(Map<String, Command> commands) {
        return new WrappedTagReplacer<>(availableTags, commands.values(), Command::getCommand,
            (cmd, text) -> new Command(text, cmd.getExecutor()));
    }

    private List<Tag<Player>> buildAvailableTags() {
        return Arrays.asList(
            createTag("%p",       pl -> pl.getName()),
            createTag("%nick",    pl -> pl.getDisplayName()),
            createTag("%ip",      pl -> PlayerUtils.getPlayerIp(pl)),
            createTag("%country", pl -> geoIpService.getCountryName(PlayerUtils.getPlayerIp(pl))));
    }
}