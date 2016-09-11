/*
 * http://ryred.co/
 * ace[at]ac3-servers.eu
 *
 * =================================================================
 *
 * Copyright (c) 2016, Cory Redmond
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of PwnFilter nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.pwn9.filter.bungee.config;

import com.pwn9.filter.engine.FilterService;
import com.pwn9.filter.engine.PointManager;
import com.pwn9.filter.engine.api.Action;
import com.pwn9.filter.engine.rules.action.ActionFactory;
import com.pwn9.filter.engine.rules.action.InvalidActionException;
import com.pwn9.filter.engine.rules.action.targeted.TargetedAction;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Cory Redmond &lt;ace@ac3-servers.eu&gt;
 */
public class BungeeConfig {

	private static boolean globalMute = false;
	private static Configuration config;

	private static File dataFolder;

	public static void loadConfiguration(Configuration configuration, File folder, FilterService filterService) {

		dataFolder = folder;
		config = configuration;

		if (config.getBoolean("logfile")) {
			filterService.setLogFileHandler(new File(dataFolder, "pwnfilter.log"));
			filterService.setDebugMode(config.getString("debug", "off"));

		} else { // Needed during configuration reload to turn off logging if the option changes
			filterService.clearLogFileHandler();
		}

		// Set the directory containing rules files.
		File ruleDir = setupDirectory(config.getString("ruledirectory", "rules"),
				filterService.getLogger());
		if (ruleDir != null) {
			filterService.getConfig().setRulesDir(ruleDir);
		} else {
			throw new RuntimeException(
					"Unable to create or access rule directory.");
		}

		// Set the directory containing Text Files
		filterService.getConfig().setTextDir(
				setupDirectory(config.getString("textdir", "textfiles"),
						filterService.getLogger())
		);

		// Set up the default action messages
		TargetedAction.getActionsWithDefaults().
				filter(targetedAction -> !(config.getString(targetedAction.getDefaultMsgConfigName()) == null)).
				forEach(targetedAction -> targetedAction.setDefMsg(
								ChatColor.translateAlternateColorCodes( '&',
										config.getString( targetedAction.getDefaultMsgConfigName() ) ))
				);

		// Setup logging
		Level logLevel;
		try {
			logLevel = Level.parse(config.getString("loglevel", "info").toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new RuntimeException("Could not parse loglevel.  Must be either 'info' or 'fine'.  Found: " + config.getString("loglevel"));
		}
		filterService.getConfig().setLogLevel(logLevel);

		setupPoints(filterService);

	}

	private static void setupPoints(FilterService filterService) {
		PointManager pointManager = filterService.getPointManager();
		Configuration pointsSection = config.getSection("points");
		if (!pointsSection.getBoolean("enabled")) {
			if (pointManager.isEnabled()) {
				pointManager.shutdown();
			}
		} else {
			if (!pointManager.isEnabled()) {
				pointManager.setLeakPoints(pointsSection.getDouble("leak.points", 1));
				pointManager.setLeakInterval(pointsSection.getInt("leak.interval", 30));

				try {
					parseThresholds(pointsSection.getSection( "thresholds" ), pointManager, filterService.getActionFactory());
				} catch (InvalidActionException ex) {
					filterService.getLogger().warning("Invalid Action parsing Thresholds: " + ex.getMessage());
					pointManager.shutdown();
				}
				pointManager.start();
			}
		}
	}

	private static void parseThresholds(Configuration configSection,
	                                    PointManager pointManager,
	                                    ActionFactory actionFactory)
			throws InvalidActionException {

		for (String threshold : configSection.getKeys()) {
			pointManager.getFilterService().getLogger().
					finest("Parsing Threshold: " + threshold);

			List<Action> ascending = new ArrayList<>();
			List<Action> descending = new ArrayList<>();

			for (String action : configSection.getStringList(threshold + ".actions.ascending")) {
				pointManager.getFilterService().getLogger().
						finest("Adding Ascending Action: " + action);
				ascending.add(actionFactory.getActionFromString(action));
			}
			for (String action : configSection.getStringList(threshold + ".actions.descending")) {
				pointManager.getFilterService().getLogger().
						finest("Adding Descending Action: " + action);
				descending.add(actionFactory.getActionFromString(action));
			}
			pointManager.addThreshold(
					configSection.getString(threshold + ".name"),
					configSection.getDouble(threshold + ".points"),
					ascending,
					descending);
			pointManager.getFilterService().getLogger().
					finest("Adding Threshold: " + configSection.getString(threshold + ".name") +
							" at points: " + configSection.getDouble(threshold + ".points"));
		}

	}

	/**
	 * Ensure that the named directory exists and is accessible.  If the
	 * directory begins with a / (slash), it is assumed to be an absolute
	 * path.  Otherwise, the directory is assumed to be relative to the root
	 * data folder.
	 * <p/>
	 * If the directory doesn't exist, an attempt is made to create it.
	 *
	 * @param directoryName relative or absolute path to the directory
	 * @return {@link File} referencing the directory.
	 */
	private static File setupDirectory(@NotNull String directoryName,
	                                   Logger logger) {
		File dir;
		if (directoryName.startsWith("/")) {
			dir = new File(directoryName);
		} else {
			dir = new File(dataFolder, directoryName);
		}
		try {
			if (!dir.exists()) {
				if (dir.mkdirs())
					logger.info("Created directory: " + dir.getAbsolutePath());
			}
			return dir;
		} catch (Exception ex) {
			logger.warning("Unable to access or create directory: " + dir.getAbsolutePath());
			return null;
		}

	}

	public static byte valueOfEvent( String string ) {

		string = string.trim().toUpperCase();
		switch ( string ) {

			case "LOWEST":
				return -64;
			case "LOW":
				return -32;
			case "NORMAL":
				return 0;
			case "HIGH":
				return 32;
			case "HIGHEST":
				return 64;
			default:
				return 0;

		}

	}

	public static boolean decolor() {
		return config.getBoolean("decolor");
	}

	public static boolean globalMute() {
		return globalMute;
	}

	public static void setGlobalMute(boolean globalMute) {
		BungeeConfig.globalMute = globalMute;
	}

	public static List<String> getCmdlist() {
		return config.getStringList("cmdlist");
	}

	public static List<String> getCmdblist() {
		return config.getStringList("cmdblist");
	}

	public static List<String> getCmdchat() {
		return config.getStringList("cmdchat");
	}

	public static byte getCmdpriority() {
		return valueOfEvent( config.getString( "cmdpriority", "LOWEST" ).toUpperCase() );
	}

	public static byte getChatpriority() {
		return valueOfEvent(config.getString("chatpriority", "LOWEST").toUpperCase());
	}

	public static boolean cmdfilterEnabled() {
		return config.getBoolean("commandfilter");
	}

	public static boolean commandspamfilterEnabled() {
		return config.getBoolean("commandspamfilter");
	}

	public static boolean spamfilterEnabled() {
		return config.getBoolean("spamfilter");
	}

	public static boolean consolefilterEnabled() {
		return config.getBoolean("consolefilter");
	}

}
