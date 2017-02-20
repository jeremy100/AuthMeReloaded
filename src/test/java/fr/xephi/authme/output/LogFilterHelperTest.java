package fr.xephi.authme.output;

import com.google.common.base.Preconditions;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandInitializer;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link LogFilterHelper}.
 */
public class LogFilterHelperTest {

    private static final List<CommandDescription> ALL_COMMANDS = new CommandInitializer().getCommands();

    /**
     * Checks that {@link LogFilterHelper#COMMANDS_TO_SKIP} contains the entries we expect
     * (commands with password argument).
     */
    @Test
    public void shouldBlacklistAllSensitiveCommands() {
        // given
        List<CommandDescription> sensitiveCommands = Arrays.asList(
            getCommand("register"), getCommand("login"), getCommand("changepassword"), getCommand("unregister"),
            getCommand("authme", "register"), getCommand("authme", "changepassword")
        );
        // Build array with entries like "/register ", "/authme cp ", "/authme changepass "
        String[] expectedEntries = sensitiveCommands.stream()
            .map(cmd -> buildCommandSyntaxes(cmd))
            .flatMap(List::stream)
            .map(syntax -> syntax + " ")
            .toArray(String[]::new);

        // when / then
        assertThat(Arrays.asList(LogFilterHelper.COMMANDS_TO_SKIP), containsInAnyOrder(expectedEntries));

    }

    private static CommandDescription getCommand(String label) {
        return findCommandWithLabel(label, ALL_COMMANDS);
    }

    private static CommandDescription getCommand(String parentLabel, String childLabel) {
        CommandDescription parent = getCommand(parentLabel);
        return findCommandWithLabel(childLabel, parent.getChildren());
    }

    private static CommandDescription findCommandWithLabel(String label, List<CommandDescription> commands) {
        return commands.stream()
            .filter(cmd -> cmd.getLabels().contains(label))
            .findFirst().orElseThrow(() -> new IllegalArgumentException(label));
    }

    /**
     * Returns all "command syntaxes" from which the given command can be reached.
     * For example, the result might be a List containing "/authme changepassword", "/authme changepass"
     * and "/authme cp".
     *
     * @param command the command to build syntaxes for
     * @return command syntaxes
     */
    private static List<String> buildCommandSyntaxes(CommandDescription command) {
        // assumes that parent can only have one label -> if this fails in the future, we need to revise this method
        Preconditions.checkArgument(command.getParent() == null || command.getParent().getLabels().size() == 1);

        String prefix = command.getParent() == null
            ? "/"
            : "/" + command.getParent().getLabels().get(0) + " ";
        return command.getLabels().stream()
            .map(label -> prefix + label)
            .collect(Collectors.toList());
    }
}