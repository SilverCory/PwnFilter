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
import com.pwn9.filter.bungee.PwnFilterBungeePlugin;
import com.pwn9.filter.bungee.config.BungeeConfig;
import com.pwn9.filter.engine.api.FilterContext;
import com.pwn9.filter.engine.api.MessageAuthor;
import com.pwn9.filter.engine.rules.chain.InvalidChainException;
import com.pwn9.filter.engine.rules.chain.RuleChain;
import com.pwn9.filter.minecraft.util.ColoredString;
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
public class PwnFilterCommandListener extends BaseListener<ChatEvent> {

	private final PwnFilterBungeePlugin plugin;
	private RuleChain chatRuleChain;

	public PwnFilterCommandListener(PwnFilterBungeePlugin plugin) {
		super(plugin.getFilterService());
		this.plugin = plugin;
	}

	public String getShortName() {
		return "COMMAND";
	}

	@Override
	public void activate() {
		if (isActive()) return;

		PluginManager pm = ProxyServer.getInstance().getPluginManager();
		setPriority( BungeeConfig.getCmdpriority() );
		if (BungeeConfig.cmdfilterEnabled()) {
			try {
				ruleChain = getCompiledChain(filterService.getConfig().getRuleFile("command.txt"));
				chatRuleChain = getCompiledChain(filterService.getConfig().getRuleFile("chat.txt"));

				pm.registerListener( PwnFilterBungeePlugin.getInstance(), this );

				setActive();
				plugin.getLogger().info("Activated CommandListener with Priority Setting: " + getPriorityString( getPriority() ) + " Rule Count: " + getRuleChain().ruleCount());

				StringBuilder sb = new StringBuilder("Commands to filter: ");
				for (String command : BungeeConfig.getCmdlist())
					sb.append(command).append(" ");
				plugin.getLogger().finest(sb.toString().trim());

				sb = new StringBuilder("Commands to never filter: ");
				for (String command : BungeeConfig.getCmdblist())
					sb.append(command).append(" ");
				plugin.getLogger().finest(sb.toString().trim());
			} catch (InvalidChainException e) {
				plugin.getLogger().severe("Unable to activate CommandListener.  Error: " + e.getMessage());
				pm.unregisterListener( this );
				setInactive();
			}
		}
	}

	/**
	 * <p>eventProcessor.</p>
	 *
	 * @param event a {@link org.bukkit.event.player.PlayerCommandPreprocessEvent} object.
	 */
	@Override
	public void eventProcessor(ChatEvent event) {

		if (event.isCancelled() || !(event.getSender() instanceof ProxiedPlayer) || !event.getMessage().startsWith( "/" ) ) return;

		MessageAuthor minecraftPlayer = plugin.getFilterService().getAuthor( (((ProxiedPlayer) event.getSender()).getUniqueId()) );

		if (minecraftPlayer.hasPermission("pwnfilter.bypass.commands")) return;

		String message = event.getMessage();

		//Gets the actual command as a string
		String cmdmessage = message.substring(1).split(" ")[0];


		FilterContext filterTask = new FilterContext(new ColoredString(message), minecraftPlayer, this);

		// Check to see if we should treat this command as chat (eg: /tell)
		if (BungeeConfig.getCmdchat().contains(cmdmessage)) {
			// Global mute
			if ((BungeeConfig.globalMute()) && (!minecraftPlayer.hasPermission("pwnfilter.bypass.mute"))) {
				event.setCancelled(true);
				return;
			}

			// Simple Spam filter
			if (BungeeConfig.commandspamfilterEnabled() && !minecraftPlayer.hasPermission("pwnfilter.bypass.spam")) {
				// Keep a log of the last message sent by this player.  If it's the same as the current message, cancel.
				if ( PwnFilterBukkitPlugin.lastMessage.containsKey(minecraftPlayer.getId()) && PwnFilterBukkitPlugin.lastMessage.get(minecraftPlayer.getId()).equals(message)) {
					event.setCancelled(true);
					minecraftPlayer.sendMessage( ChatColor.DARK_RED + "[PwnFilter]" + ChatColor.RED + " Repeated command blocked by spam filter.");
					return;
				}
				PwnFilterBukkitPlugin.lastMessage.put(minecraftPlayer.getId(), message);
			}

			chatRuleChain.execute(filterTask, filterService);

		} else {

			if (!BungeeConfig.getCmdlist().isEmpty() && !BungeeConfig.getCmdlist().contains(cmdmessage))
				return;
			if (BungeeConfig.getCmdblist().contains(cmdmessage)) return;

			// Take the message from the Command Event and send it through the filter.

			ruleChain.execute(filterTask, filterService);

		}

		// Only update the message if it has been changed.
		if (filterTask.messageChanged()) {
			if (filterTask.getModifiedMessage().toString().isEmpty()) {
				event.setCancelled(true);
				return;
			}
			event.setMessage(filterTask.getModifiedMessage().getRaw());
		}

		if (filterTask.isCancelled()) event.setCancelled(true);

	}

	// Watch how gross this is.. :(

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEventLowest( ChatEvent event ) {
		doEvent( event, EventPriority.LOWEST );
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onEventLow( ChatEvent event ) {
		doEvent( event, EventPriority.LOW );
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onEventNormal( ChatEvent event ) {
		doEvent( event, EventPriority.NORMAL );
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEventHighest( ChatEvent event ) {
		doEvent( event, EventPriority.HIGHEST );
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onEventHigh( ChatEvent event ) {
		doEvent( event, EventPriority.HIGH );
	}

}
