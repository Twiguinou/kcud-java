package showoff;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import showoff.App.Demo.BasicDemo;
import showoff.App.DemoApplication;
import showoff.WindowContext.GLFWWindowProcessor;
import showoff.WindowContext.WindowProcessor;

import java.util.function.Function;

public class Main
{
    private static void configureLog4j()
    {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

        builder.setConfigurationName("showoff-log4j-logger");
        builder.setStatusLevel(Level.WARN);

        builder.add(builder.newAppender("Console", "CONSOLE")
                .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT)
                .add(builder.newLayout("PatternLayout")
                        .addAttribute("disableAnsi", false)
                        .addAttribute("pattern", "%highlight{[%d] - %msg%n}{FATAL=red blink, ERROR=red, WARN=yellow bold, INFO=green, DEBUG=green bold, TRACE=blue}")));

        builder.add(builder.newRootLogger(Level.ALL)
                .add(builder.newAppenderRef("Console")));

        Configurator.reconfigure(builder.build());
    }

    public static void main(final String... args)
    {
        configureLog4j();
        ProgramArguments arguments = new ProgramArguments(args);
        final int[] dimensions = arguments.getArgValues("wnd_dimensions", new int[]{1280,960});
        WindowProcessor backendWindowProc = switch (arguments.getArgValueIndexed("wnd_backend", 0, Function.identity()).orElse(""))
                {
                    default -> new GLFWWindowProcessor();
                };
        final boolean debug = arguments.getArgValueIndexed("debug", 0, Boolean::parseBoolean).orElse(true);
        final int msaa_samples = arguments.getArgValueIndexed("msaa_samples", 0, Integer::parseInt).orElse(4);
        if (msaa_samples == 0 || (msaa_samples & (msaa_samples - 1)) != 0)
        {
        	throw new IllegalArgumentException("Invalid number of MSAA samples: " + msaa_samples);
        }

        DemoApplication app = new DemoApplication(backendWindowProc, dimensions[0], dimensions[1], debug, msaa_samples);
        app.setupDemo(new BasicDemo());
        app.run();
    }
}
