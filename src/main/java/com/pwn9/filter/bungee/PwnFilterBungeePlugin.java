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

package com.pwn9.filter.bungee;

import com.google.common.collect.MapMaker;
import com.google.common.io.ByteStreams;
import com.pwn9.filter.bukkit.PwnFilterPlugin;
import com.pwn9.filter.bukkit.TemplateProvider;
import com.pwn9.filter.bungee.config.BungeeConfig;
import com.pwn9.filter.bungee.listener.PlayerCacheListener;
import com.pwn9.filter.bungee.listener.PwnFilterChatListener;
import com.pwn9.filter.bungee.listener.PwnFilterCommandListener;
import com.pwn9.filter.engine.FilterService;
import com.pwn9.filter.engine.rules.action.minecraft.MinecraftAction;
import com.pwn9.filter.engine.rules.action.targeted.TargetedAction;
import com.pwn9.filter.minecraft.api.MinecraftConsole;
import com.pwn9.filter.util.tag.RegisterTags;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Cory Redmond &lt;ace@ac3-servers.eu&gt;
 */
public class PwnFilterBungeePlugin extends Plugin implements PwnFilterPlugin, TemplateProvider {

	public static final ConcurrentMap<UUID, String> lastMessage = new MapMaker().concurrencyLevel(2).weakKeys().makeMap();
	private static PwnFilterBungeePlugin _instance;
	private BungeeAPI minecraftAPI;
	private MinecraftConsole console;
	private FilterService filterService;
	private Configuration config;

	/**
	 * <p>getInstance.</p>
	 *
	 * @return a {@link PwnFilterBungeePlugin} object.
	 */
	public static PwnFilterBungeePlugin getInstance() {
		return _instance;
	}

	@Override
	public void onLoad() {

		if (_instance == null) {
			_instance = this;
		} else {
			throw new IllegalStateException("Only one instance of PwnFilter can be run per server");
		}
		minecraftAPI = new BungeeAPI(this);
		console = new MinecraftConsole(minecraftAPI);
		filterService = new FilterService(getLogger());
		filterService.getActionFactory().addActionTokens(MinecraftAction.class);
		filterService.getActionFactory().addActionTokens(TargetedAction.class);
		filterService.getConfig().setTemplateProvider(this);
		RegisterTags.all();

	}

	@Override
	public void onEnable() {

		if( !getDataFolder().exists() ) {
			getDataFolder().mkdirs();
		}

		File configFile = getConfigurationFile();
		if( !configFile.exists() ) {
			try {
				configFile.createNewFile();
				try ( InputStream is = getResourceAsStream("config.yml"); OutputStream os = new FileOutputStream( getFile() ) ) {
					ByteStreams.copy( is, os );
				}
			} catch ( IOException e ) {
				throw new RuntimeException( "Unable to create config file", e );
			}
		}

		// Now get our configuration
		if (!configurePlugin()) return;

		filterService.registerAuthorService(minecraftAPI);
		filterService.registerNotifyTarget(minecraftAPI);

		//Load up our listeners
		//        BaseListener.setAPI(minecraftAPI);

		filterService.registerClient(new PwnFilterCommandListener(this));
		filterService.registerClient(new PwnFilterChatListener(this));

		getProxy().getPluginManager().registerListener( this, new PlayerCacheListener() );

		// Enable the listeners
		filterService.enableClients();

	}

	@Override
	public void onDisable() {
		filterService.shutdown();
		filterService.deregisterAuthorService(minecraftAPI);
		_instance = null;
	}

	public boolean configurePlugin() {
		minecraftAPI.reset();
		try {
			// Stupid hack because YamlConfiguration.loadConfiguration() eats our exception
			config = ConfigurationProvider.getProvider( YamlConfiguration.class ).load( getConfigurationFile() );

			BungeeConfig.loadConfiguration( getConfig(), getDataFolder(), filterService );
			return true;
		} catch (IOException ex) {
			filterService.getLogger().severe("Fatal configuration failure: " + ex.getMessage());
			filterService.getLogger().severe("PwnFilter disabled.");
		}
		return false;
	}

	@Override
	public FilterService getFilterService() {
		return filterService;
	}

	@Override
	public MinecraftConsole getConsole() {
		return console;
	}

	@Override
	public InputStream getResource( String name ) {
		return getResourceAsStream( name );
	}

	public Configuration getConfig()
	{
		try {
			return this.config == null ? ( this.config = ConfigurationProvider.getProvider( YamlConfiguration.class ).load( getConfigurationFile() ) ) : config;
		} catch ( IOException e ) {
			e.printStackTrace();
			return null;
		}
	}

	public File getConfigurationFile() {
		return new File( getDataFolder(), "config.yml" );
	}
}
