/*
 * Copyright (c) 2017 Michael Krotscheck
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.krotscheck.kangaroo.server;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.EnvironmentConfiguration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the central configuration builder used for common configuration flags
 * across the kangaroo ecosystem.
 *
 * @author Michael Krotscheck
 */
public final class ConfigurationBuilder {

    /**
     * Logger instance.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ConfigurationBuilder.class);

    /**
     * Our commandline options.
     */
    private static final Options CLI_OPTIONS;

    static {
        // Initialize the CLI Options.
        CLI_OPTIONS = new Options();

        Option bindHost = Option.builder("h")
                .longOpt(Config.HOST.getKey())
                .argName(Config.HOST.getKey())
                .hasArg()
                .desc("The IP address or hostname which this server should "
                        + "bind to.")
                .build();
        Option bindPort = Option.builder("p")
                .longOpt(Config.PORT.getKey())
                .argName(Config.PORT.getKey())
                .hasArg()
                .desc("The port on which this port should listen.")
                .build();

        Option keystorePath = Option.builder()
                .longOpt(Config.KEYSTORE_PATH.getKey())
                .argName(Config.KEYSTORE_PATH.getKey())
                .hasArg()
                .desc("Path to an externally provided keystore.")
                .build();

        Option keystorePass = Option.builder()
                .longOpt(Config.KEYSTORE_PASS.getKey())
                .argName(Config.KEYSTORE_PASS.getKey())
                .hasArg()
                .desc("Password for the externally provided keystore.")
                .build();

        Option keystoreType = Option.builder()
                .longOpt(Config.KEYSTORE_TYPE.getKey())
                .argName(Config.KEYSTORE_TYPE.getKey())
                .hasArg()
                .desc("JVM KeyStore type to expect. Default PKCS12.")
                .build();

        Option certAlias = Option.builder()
                .longOpt(Config.CERT_ALIAS.getKey())
                .argName(Config.CERT_ALIAS.getKey())
                .hasArg()
                .desc("Alias of the HTTPS certificate to use.")
                .build();

        Option certKeyPass = Option.builder()
                .longOpt(Config.CERT_KEY_PASS.getKey())
                .argName(Config.CERT_KEY_PASS.getKey())
                .hasArg()
                .desc("Password of the private key for the certificate.")
                .build();

        Option htmlAppRoot = Option.builder()
                .longOpt(Config.HTML_APP_ROOT.getKey())
                .argName(Config.HTML_APP_ROOT.getKey())
                .hasArg()
                .desc("Path to the server's HTML5 Application root. We "
                        + "presume HTML5 routing support in the application "
                        + "served there.")
                .build();

        CLI_OPTIONS.addOption(bindHost);
        CLI_OPTIONS.addOption(bindPort);
        CLI_OPTIONS.addOption(keystorePath);
        CLI_OPTIONS.addOption(keystorePass);
        CLI_OPTIONS.addOption(keystoreType);
        CLI_OPTIONS.addOption(certAlias);
        CLI_OPTIONS.addOption(certKeyPass);
        CLI_OPTIONS.addOption(htmlAppRoot);
    }

    /**
     * The cli configuration.
     */
    private Configuration defaultConfiguration;

    /**
     * The cli configuration.
     */
    private Configuration cliConfiguration;

    /**
     * List of configurations.
     */
    private List<Configuration> fileConfiguration = new ArrayList<>();

    /**
     * Create a new configuration builder.
     */
    public ConfigurationBuilder() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put(Config.HOST.getKey(), Config.HOST.getValue());
        defaults.put(Config.PORT.getKey(), Config.PORT.getValue());

        defaultConfiguration = new MapConfiguration(defaults);
    }

    /**
     * Add commandline options. This path will exit if the options are not
     *
     * @param args Commandline argument list.
     * @return This builder.
     */
    public ConfigurationBuilder addCommandlineArgs(final String[] args) {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(CLI_OPTIONS, args, false);
        } catch (ParseException e) {
            HelpFormatter formater = new HelpFormatter();
            formater.printHelp("Main", CLI_OPTIONS);
            throw new RuntimeException();
        }

        Map<String, String> cliProps = new HashMap<>();
        for (Option o : cmd.getOptions()) {
            cliProps.put(o.getArgName(), o.getValue());
        }
        cliConfiguration = new MapConfiguration(cliProps);

        return this;
    }

    /**
     * Add a file using 'properties' format to the configuration. If this
     * file cannot be read, or if the file does not exist, the file will
     * simply not be added and a warn statement will be sent to the logger.
     *
     * @param path Path to the file.
     * @return This builder.
     */
    public ConfigurationBuilder addPropertiesFile(final String path) {
        return addPropertiesFile(new File(path));
    }

    /**
     * Add a file using 'properties' format to the configuration. If this
     * file cannot be read, or if the file does not exist, the file will
     * simply not be added and a warn statement will be sent to the logger.
     *
     * @param file The file to read.
     * @return This builder.
     */
    public ConfigurationBuilder addPropertiesFile(final File file) {
        if (file.exists()) {
            try {
                LOGGER.info(String.format("Loading configuration: %s",
                        file.getAbsolutePath()));
                Configuration c = new PropertiesConfiguration(file);
                fileConfiguration.add(c);
            } catch (ConfigurationException ce) {
                LOGGER.warn(String.format("Unable to read properties file: %s",
                        file.getAbsolutePath()));
            }
        }
        return this;
    }

    /**
     * Build the configuration instance.
     *
     * @return The composite configuration.
     */
    public Configuration build() {
        CompositeConfiguration config = new CompositeConfiguration();

        // Commandline will be checked first.
        if (cliConfiguration != null) {
            config.addConfiguration(cliConfiguration);
        }
        // System next.
        config.addConfiguration(new SystemConfiguration());
        // Now load all the property files.
        fileConfiguration.forEach(config::addConfiguration);
        // Environment
        config.addConfiguration(new EnvironmentConfiguration());
        // Defaults...
        config.addConfiguration(defaultConfiguration);

        return config;
    }
}
