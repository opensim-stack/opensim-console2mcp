package uk.co.bithatch.opensim.console2mcp;

public final class OpensimRESTConsoleParsingHarness {

    public static void main(String[] args) {
        testOptionalGroupedArgsSplit();
        testCommaGroupedArgsSplit();
        testVectorArgsSplit();
        testUnnamedEnumArgs();
        testSlashEnumArgs();
        testOptionAliases();
        testNestedOptionals();
        testPromptCompletionHeuristics();
        System.out.println("All parsing checks passed.");
    }

    private static void testOptionalGroupedArgsSplit() {
        var line = "show caps stats by user [<first-name> <last-name>] - Shows statistics on capabilities use by user.";
        var cmd = OpensimRESTConsole.parseHelpCommandLineForTest(line);
        require("show caps stats by user".equals(cmd.name()), "command name mismatch");
        require(cmd.arguments().size() == 2, "expected 2 grouped optional args");
        require("first-name".equals(cmd.arguments().get(0).name()), "first arg name mismatch");
        require("last-name".equals(cmd.arguments().get(1).name()), "second arg name mismatch");
        require(cmd.arguments().get(0).optional(), "first arg should be optional");
        require(cmd.arguments().get(1).optional(), "second arg should be optional");
    }

    private static void testCommaGroupedArgsSplit() {
        var line = "rotate scene <degrees> [centerX, centerY] - Rotates all scene objects around centerX, centerY";
        var cmd = OpensimRESTConsole.parseHelpCommandLineForTest(line);
        require(cmd.arguments().size() == 3, "expected 3 args for rotate scene");
        require("degrees".equals(cmd.arguments().get(0).name()), "degrees arg missing");
        require("centerX".equals(cmd.arguments().get(1).name()), "centerX arg missing");
        require("centerY".equals(cmd.arguments().get(2).name()), "centerY arg missing");
        require(!cmd.arguments().get(0).optional(), "degrees should be required");
        require(cmd.arguments().get(1).optional(), "centerX should be optional");
        require(cmd.arguments().get(2).optional(), "centerY should be optional");
    }

    private static void testVectorArgsSplit() {
        var line = "delete object pos <start x, start y , start z> <end x, end y, end z> - Delete scene objects within the given volume.";
        var cmd = OpensimRESTConsole.parseHelpCommandLineForTest(line);
        require(cmd.arguments().size() == 6, "expected 6 args for volume boundaries");
        require("start x".equals(cmd.arguments().get(0).name()), "start x missing");
        require("start y".equals(cmd.arguments().get(1).name()), "start y missing");
        require("start z".equals(cmd.arguments().get(2).name()), "start z missing");
        require("end x".equals(cmd.arguments().get(3).name()), "end x missing");
        require("end y".equals(cmd.arguments().get(4).name()), "end y missing");
        require("end z".equals(cmd.arguments().get(5).name()), "end z missing");
    }

    private static void testUnnamedEnumArgs() {
        var line = "debug jobengine <start|stop|status|log> - Start, stop, get status or set logging level of the job engine.";
        var cmd = OpensimRESTConsole.parseHelpCommandLineForTest(line);
        require(cmd.arguments().size() == 1, "expected one enum arg");
        require("arg0".equals(cmd.arguments().get(0).name()), "enum arg should get arg0 name");
        require(cmd.arguments().get(0).values().size() == 4, "enum values size mismatch");
        require("start".equals(cmd.arguments().get(0).values().get(0)), "enum value start missing");
        require("log".equals(cmd.arguments().get(0).values().get(3)), "enum value log missing");
    }

    private static void testOptionAliases() {
        var line = "save oar [-h|--home=<url>] - Save region to OAR.";
        var cmd = OpensimRESTConsole.parseHelpCommandLineForTest(line);
        require(cmd.arguments().size() == 1, "expected one option arg");
        require("home".equals(cmd.arguments().get(0).name()), "expected normalized canonical option name");
        require("url".equals(cmd.arguments().get(0).option()), "expected option placeholder name");
        require(cmd.arguments().get(0).aliases().size() == 1, "expected one option alias");
        require("-h".equals(cmd.arguments().get(0).aliases().get(0)), "expected -h alias");
    }

    private static void testSlashEnumArgs() {
        var line = "bypass permissions <true / false> - Bypass permission checks";
        var cmd = OpensimRESTConsole.parseHelpCommandLineForTest(line);
        require(cmd.arguments().size() == 1, "expected one slash enum arg");
        require("arg0".equals(cmd.arguments().get(0).name()), "slash enum should get arg0 name");
        require(cmd.arguments().get(0).values().size() == 2, "slash enum should have two values");
        require("true".equals(cmd.arguments().get(0).values().get(0)), "slash enum true missing");
        require("false".equals(cmd.arguments().get(0).values().get(1)), "slash enum false missing");
    }

    private static void testNestedOptionals() {
        var line = "load xml [<file name> [-newUID [<x> <y> <z>]]] - Load a region's data from XML format";
        var cmd = OpensimRESTConsole.parseHelpCommandLineForTest(line);
        require(cmd.arguments().size() == 5, "expected 5 arguments from nested optional group");
        require("file name".equals(cmd.arguments().get(0).name()), "file name arg missing");
        require("newUID".equals(cmd.arguments().get(1).name()), "newUID arg missing");
        require(cmd.arguments().get(1).option() == null, "flag options should not have placeholder");
        require("x".equals(cmd.arguments().get(2).name()), "x arg missing");
        require("y".equals(cmd.arguments().get(3).name()), "y arg missing");
        require("z".equals(cmd.arguments().get(4).name()), "z arg missing");
        require(cmd.arguments().stream().allMatch(OpensimRESTConsole.HelpArgument::optional),
                "all nested args should be optional");
    }

    private static void testPromptCompletionHeuristics() {
        require(OpensimRESTConsole.isPromptCompletionLineForTest("true", "false", "anything", "Region #"),
                "prompt flag should complete command");
        require(OpensimRESTConsole.isPromptCompletionLineForTest("false", "true", "anything", "Region #"),
                "command flag should complete command");
        require(OpensimRESTConsole.isPromptCompletionLineForTest("false", "false", "Region (Welcome Island) #",
                "Region (Welcome Island) #"), "prompt text match should complete command");
        require(!OpensimRESTConsole.isPromptCompletionLineForTest("false", "false", "show users",
                "Region (Welcome Island) #"), "non-prompt line should not complete command");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
