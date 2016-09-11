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

package com.pwn9.filter.bungee.listener;

import com.pwn9.filter.bukkit.PwnFilterBukkitPlugin;
import com.pwn9.filter.bukkit.PwnFilterPlugin;
import com.pwn9.filter.bukkit.config.BukkitConfig;
import com.pwn9.filter.bungee.PwnFilterBungeePlugin;
import com.pwn9.filter.bungee.config.BungeeConfig;
import com.pwn9.filter.engine.api.FilterContext;
import com.pwn9.filter.engine.api.MessageAuthor;
import com.pwn9.filter.engine.api.UnknownAuthor;
import com.pwn9.filter.engine.rules.chain.InvalidChainException;
import com.pwn9.filter.minecraft.util.ColoredString;
import com.pwn9.filter.util.SimpleString;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.bukkit.ChatColor;

/**
 * @author Cory Redmond &lt;ace@ac3-servers.eu&gt;
 */
public class PwnFilterChatListener extends BaseListener<ChatEvent> {

	private final PwnFilterPlugin plugin;

	public PwnFilterChatListener(PwnFilterPlugin plugin) {
		super(plugin.getFilterService());
		this.plugin = plugin;
	}

	public String getShortName() {
		return "CHAT";
	}


	public void eventProcessor(ChatEvent event) {

		if (event.isCancelled() || !(event.getSender() instanceof ProxiedPlayer) || !event.getMessage().startsWith( "/" ) ) return;

		ProxiedPlayer player = ((ProxiedPlayer) event.getSender());
		MessageAuthor minecraftPlayer = plugin.getFilterService().getAuthor((player.getUniqueId()));

		// This should never happen.  Log it, if it does.
		if (minecraftPlayer instanceof UnknownAuthor ) {
			plugin.getLogger().info("Filtering Aborted. Unable to lookup player in Chat Event.  PlayerUUID: "
					+ player.getUniqueId());
			plugin.getLogger().info("Message: " + event.getMessage());
			plugin.getLogger().info("AuthorServices: " + filterService.getAuthorServices());
			plugin.getLogger().info("Bukkit player online: " + (player.getServer() != null));
			return;
		}

		// Permissions Check, if player has bypass permissions, then skip everything.
		if (minecraftPlayer.hasPermission("pwnfilter.bypass.chat")) return;

		String message = event.getMessage();

		// Global mute
		if ((BukkitConfig.globalMute()) && (!minecraftPlayer.hasPermission("pwnfilter.bypass.mute"))) {
			event.setCancelled(true);
			return; // No point in continuing.
		}

		if (BukkitConfig.spamfilterEnabled() && !minecraftPlayer.hasPermission("pwnfilter.bypass.spam")) {
			// Keep a log of the last message sent by this player.  If it's the same as the current message, cancel.
			if (PwnFilterBukkitPlugin.lastMessage.containsKey(minecraftPlayer.getId()) && PwnFilterBukkitPlugin.lastMessage.get(minecraftPlayer.getId()).equals(message)) {
				event.setCancelled(true);
				minecraftPlayer.sendMessage(ChatColor.DARK_RED + "[PwnFilter]" + ChatColor.RED + " Repeated command blocked by spam filter.");

				return;
			}
			PwnFilterBukkitPlugin.lastMessage.put(minecraftPlayer.getId(), message);

		}

		FilterContext state = new FilterContext(new ColoredString(message), minecraftPlayer, this);

		// Global decolor
		if ((BukkitConfig.decolor()) && !(minecraftPlayer.hasPermission("pwnfilter.color"))) {
			// We are changing the state of the message.  Let's do that before any rules processing.
			state.setModifiedMessage(new SimpleString(state.getModifiedMessage().toString()));
		}

		// Take the message from the ChatEvent and send it through the filter.
		plugin.getLogger().finer("Applying '" + ruleChain.getConfigName() + "' to message: " + state.getModifiedMessage());
		ruleChain.execute(state, filterService);

		// Only update the message if it has been changed.
		if (state.messageChanged()) {
			event.setMessage(state.getModifiedMessage().getRaw());
		}
		if (state.isCancelled()) event.setCancelled(true);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Activate this listener.  This method can be called either by the owning plugin
	 * or by PwnFilter.  PwnFilter will call the shutdown / activate methods when PwnFilter
	 * is enabled / disabled and whenever it is reloading its config / rules.
	 * <p>
	 * These methods could either register / deregister the listener with Bukkit, or
	 * they could just enable / disable the use of the filter.
	 */
	@Override
	public void activate() {

		if (isActive()) return;

		try {

			ruleChain = getCompiledChain(filterService.getConfig().getRuleFile("chat.txt"));

			PluginManager pm = ProxyServer.getInstance().getPluginManager();

			setPriority( BungeeConfig.getChatpriority() );

			plugin.getLogger().info("Activated PlayerListener with Priority Setting: " + BukkitConfig.getChatpriority().toString()
					+ " Rule Count: " + getRuleChain().ruleCount());

			pm.registerListener( PwnFilterBungeePlugin.getInstance(), this );

			setActive();
		} catch (InvalidChainException e) {
			plugin.getLogger().severe("Unable to activate PlayerListener.  Error: " + e.getMessage());
			setInactive();
		}
	}

	// Watch how gross this is.. :(

	@EventHandler( priority = EventPriority.LOWEST )
	public void onEventLowest( ChatEvent event ) {
		doEvent( event, EventPriority.LOWEST );
	}

	@EventHandler( priority = EventPriority.LOW )
	public void onEventLow( ChatEvent event ) {
		doEvent( event, EventPriority.LOW );
	}

	@EventHandler( priority = EventPriority.NORMAL )
	public void onEventNormal( ChatEvent event ) {
		doEvent( event, EventPriority.NORMAL );
	}

	@EventHandler( priority = EventPriority.HIGHEST )
	public void onEventHighest( ChatEvent event ) {
		doEvent( event, EventPriority.HIGHEST );
	}

	@EventHandler( priority = EventPriority.HIGH )
	public void onEventHigh( ChatEvent event ) {
		doEvent( event, EventPriority.HIGH );
	}

}
