package com.jirascraper;

import com.jirascraper.config.CliConfig;
import com.jirascraper.config.ScraperConfig;
import com.jirascraper.service.JiraScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Main application entry point for the Jira LLM Data Scraper.
 */
public class JiraScraperApplication {

    private static final Logger logger = LoggerFactory.getLogger(JiraScraperApplication.class);

    public static void main(String[] args) {
        // Parse command line arguments
        CliConfig cliConfig = new CliConfig();
        CommandLine cmd = new CommandLine(cliConfig);

        try {
            CommandLine.ParseResult parseResult = cmd.parseArgs(args);

            // Handle help and version flags
            if (cmd.isUsageHelpRequested()) {
                cmd.usage(System.out);
                return;
            }
            if (cmd.isVersionHelpRequested()) {
                cmd.printVersionHelp(System.out);
                return;
            }
        } catch (CommandLine.ParameterException e) {
            logger.error("Invalid command line arguments: {}", e.getMessage());
            cmd.usage(System.err);
            System.exit(1);
        }

        // Check if specific projects are requested via CLI
        if (cliConfig.getProjects() == null || cliConfig.getProjects().length == 0) {
            // No projects specified - show help
            System.err.println("Error: No projects specified. Use -p or --projects to specify project keys.\n");
            cmd.usage(System.err);
            System.exit(1);
        }

        logger.info("=== Jira LLM Data Scraper Started ===");

        // Create configuration from CLI args and defaults
        ScraperConfig config = new ScraperConfig(cliConfig);
        logger.info("Configuration: {}", config);

        // Create necessary directories
        createDirectories(config);

        JiraScraperService scraperService = null;

        try {
            scraperService = new JiraScraperService(config);

            // Scrape specified projects
            for (String projectKey : cliConfig.getProjects()) {
                logger.info("Scraping project: {}", projectKey);
                scraperService.scrapeProject(projectKey);
            }

            logger.info("=== Scraping Completed Successfully ===");

        } catch (Exception e) {
            logger.error("Application error: {}", e.getMessage(), e);
            System.exit(1);

        } finally {
            if (scraperService != null) {
                try {
                    scraperService.close();
                } catch (Exception e) {
                    logger.error("Error closing scraper service", e);
                }
            }
        }
    }

    /**
     * Create necessary directories for the application.
     */
    private static void createDirectories(ScraperConfig config) {
        try {
            Files.createDirectories(Paths.get(config.getOutputDir()));
            Files.createDirectories(Paths.get(config.getCheckpointDir()));
            Files.createDirectories(Paths.get("logs"));
            logger.info("Required directories created/verified");
        } catch (IOException e) {
            logger.error("Failed to create directories", e);
        }
    }
}
