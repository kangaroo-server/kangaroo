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
     * Commandline options (including defaults) added to this builder.
     */
    private final Options commandlineOptions = new Options();
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
        defaultConfiguration = new MapConfiguration(new HashMap<>());
    }

    /**
     * Add certain required values that must exist.
     *
     * @param defaults The defaults to add.
     * @return This builder.
     */
    public ConfigurationBuilder withDefaults(
            final Map<String, Object> defaults) {
        defaults.forEach((k, v) -> defaultConfiguration.setProperty(k, v));
        return this;
    }

    /**
     * Add additional commandline options to this builder.
     *
     * @param options The list of options to add.
     * @return This builder.
     */
    public ConfigurationBuilder withCommandlineOptions(final Options options) {
        // Build the commandline options from defaults.
        options.getOptions().forEach(commandlineOptions::addOption);
        return this;
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
            cmd = parser.parse(commandlineOptions, args, false);
        } catch (ParseException e) {
            HelpFormatter formater = new HelpFormatter();
            formater.setWidth(120);
            formater.printHelp("Main", commandlineOptions);
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
