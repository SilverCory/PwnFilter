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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.pwn9.filter.bukkit.PwnFilterPlugin;
import com.pwn9.filter.engine.api.AuthorService;
import com.pwn9.filter.engine.api.NotifyTarget;
import com.pwn9.filter.minecraft.api.MinecraftAPI;
import com.pwn9.filter.minecraft.api.MinecraftConsole;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * @author Cory Redmond &lt;ace@ac3-servers.eu&gt;
 */
public class BungeeAPI implements MinecraftAPI, AuthorService, NotifyTarget {

	private final PwnFilterPlugin plugin;
	private final MinecraftAPI playerAPI = this;
	private final Cache<UUID, BungeePlayer> playerCache = CacheBuilder.newBuilder()
			.maximumSize(100)
			.build();

	BungeeAPI(PwnFilterPlugin p) {
		plugin = p;
	}

	public MinecraftConsole getConsole() {
		return plugin.getConsole();
	}

	@Override
	public synchronized void reset() {
		playerCache.invalidateAll();
	}

	@Override
	public BungeePlayer getAuthorById(final UUID u) {

		BungeePlayer bPlayer = playerCache.getIfPresent(u);
		if (bPlayer == null) {
			ProxiedPlayer onlinePlayer = ProxyServer.getInstance().getPlayer( u );
			if (onlinePlayer != null) {
				playerCache.asMap().putIfAbsent(u, new BungeePlayer(u, this));
			}
		}

		// At this point, the player should be in the cache if they are online.
		// If player is offline, returns null
		return playerCache.getIfPresent(u);

	}

	@Override
	public boolean burn( UUID uuid, int duration, String messageString ) {
		throw new UnsupportedOperationException( "Burning players won't work using BungeeCord." );
	}

	@Override
	public void sendMessage( UUID uuid, String message ) {
		ProxiedPlayer player = ProxyServer.getInstance().getPlayer( uuid );
		if( player != null ) player.sendMessage( message );
	}

	@Override
	public void sendMessages( UUID uuid, List<String> messages ) {
		ProxiedPlayer player = ProxyServer.getInstance().getPlayer( uuid );
		if( player != null ) player.sendMessages( messages.toArray( new String[messages.size()] ) );
	}

	@Override
	public void executePlayerCommand( UUID uuid, String command ) {
		ProxiedPlayer player = ProxyServer.getInstance().getPlayer( uuid );
		if( !ProxyServer.getInstance().getPluginManager().dispatchCommand( player, command ) ) {
			player.chat( "/" + command );
		}
	}

	@Override
	public boolean withdrawMoney( UUID uuid, Double amount, String messageString ) {
		throw new UnsupportedOperationException( "Fining players won't work using BungeeCord." );
	}

	@Override
	public void kick( UUID uuid, String messageString ) {
		ProxiedPlayer player = ProxyServer.getInstance().getPlayer( uuid );
		player.disconnect( messageString );
	}

	@Override
	public void kill( UUID uuid, String messageString ) {
		throw new UnsupportedOperationException( "Killing players won't work using BungeeCord." );
	}

	@Override
	public String getPlayerWorldName( UUID uuid ) {
		throw new UnsupportedOperationException( "Worlds can't be fetched via BungeeCord." );
	}

	@Override
	public String getPlayerName( UUID uuid ) {
		ProxiedPlayer player = ProxyServer.getInstance().getPlayer( uuid );
		return player == null ? null : player.getName();
	}

	@Nullable
	@Override
	public Boolean playerIdHasPermission( UUID u, String s ) {
		ProxiedPlayer player = ProxyServer.getInstance().getPlayer( u );
		return player != null && player.hasPermission( s );
	}

	@Override
	public void sendConsoleMessage( String message ) {
		ProxyServer.getInstance().getConsole().sendMessage( message );
	}

	@Override
	public void sendConsoleMessages( List<String> messageList ) {
		ProxyServer.getInstance().getConsole().sendMessages( messageList.toArray( new String[messageList.size()] ) );
	}

	@Override
	public void sendBroadcast( String message ) {
		ProxyServer.getInstance().broadcast( message );
	}

	@Override
	public void sendBroadcast( List<String> messageList ) {
		messageList.forEach( this::sendBroadcast );
	}

	@Override
	public void executeCommand( String command ) {
		ProxyServer.getInstance().getPluginManager().dispatchCommand( ProxyServer.getInstance().getConsole(), command );
	}

	@Override
	public void notifyWithPerm( String permissionString, String sendString ) {
		ProxyServer.getInstance().getPlayers().stream()
				.filter( proxiedPlayer -> proxiedPlayer.hasPermission( sendString ) )
				.forEach( proxiedPlayer -> proxiedPlayer.sendMessage( sendString ) );
	}

}
