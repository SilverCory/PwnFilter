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

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.pwn9.filter.engine.api.MessageAuthor;
import com.pwn9.filter.engine.rules.action.targeted.BurnTarget;
import com.pwn9.filter.engine.rules.action.targeted.FineTarget;
import com.pwn9.filter.engine.rules.action.targeted.KickTarget;
import com.pwn9.filter.engine.rules.action.targeted.KillTarget;
import com.pwn9.filter.minecraft.api.MinecraftAPI;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Author of a text string sent to us by Bungee. This is typically a proxiedplayer.
 * These objects are transient, and only last for as long as the message does.
 * <p>
 * @author Cory Redmond &lt;ace@ac3-servers.eu&gt;
 */
public class BungeePlayer implements MessageAuthor, FineTarget, BurnTarget, KillTarget, KickTarget {

	static final int MAX_CACHE_AGE_SECS = 60; //

	private final MinecraftAPI minecraftAPI;
	private final UUID playerId;
	private final Stopwatch stopwatch;
	private final ConcurrentHashMap<String, Boolean> playerPermCache =
			new ConcurrentHashMap<>(16, 0.9f, 1); // Optimizations for Map
	private String playerName = "";

	BungeePlayer(UUID uuid, MinecraftAPI api) {
		this.playerId = uuid;
		this.minecraftAPI = api;
		this.stopwatch = Stopwatch.createStarted();
	}

	// For testing
	BungeePlayer(UUID uuid, MinecraftAPI api, Ticker ticker) {
		this.playerId = uuid;
		this.minecraftAPI = api;
		this.stopwatch = Stopwatch.createStarted(ticker);
	}

	@Override
	public boolean hasPermission(String permString) {

		// We are caching permissions, so we don't have to ask the API every time,
		// as that could get expensive for complex rulechains.  Every MAX_CACHE_AGE_SECS
		// we invalidate the cache on access.  This should have us asking if a player
		// has any given perm only 1 or 2 times every MAX_CACHE_AGE_SECS

		if (stopwatch.elapsed(TimeUnit.SECONDS) > MAX_CACHE_AGE_SECS) {
			stopwatch.reset();
			stopwatch.start();
			playerPermCache.clear();
		}

		Boolean hasPerm = playerPermCache.get(permString);

		if (hasPerm == null) {
			Boolean newPerm = minecraftAPI.playerIdHasPermission(playerId, permString);
			if (newPerm != null)
				playerPermCache.putIfAbsent(permString, newPerm);
		}
		// At this point, the player should be in the cache if they are online.
		// If player is offline, or there is an API failure, returns null

		hasPerm = playerPermCache.get(permString);

		return hasPerm != null && hasPerm;
	}

	@NotNull
	@Override
	public String getName() {
		if (playerName.isEmpty()) {
			String name = minecraftAPI.getPlayerName(playerId);
			if (name != null) playerName = name;
		}
		return playerName;
	}

	@NotNull
	@Override
	public UUID getId() {
		return playerId;
	}

	public MinecraftAPI getMinecraftAPI() {
		return minecraftAPI;
	}

	// Not cached.
	public String getWorldName() {
		return minecraftAPI.getPlayerWorldName(playerId);
	}


	public boolean burn(final int duration, final String messageString) {
		return minecraftAPI.burn(playerId, duration, messageString);
	}

	@Override
	public void sendMessage(final String message) {
		minecraftAPI.sendMessage(playerId, message);
	}

	@Override
	public void sendMessages(final List<String> messages) {
		minecraftAPI.sendMessages(playerId, messages);
	}

	public void executeCommand(final String command) {
		minecraftAPI.executePlayerCommand(playerId, command);
	}

	public boolean fine(final Double amount, final String messageString) {
		return minecraftAPI.withdrawMoney(playerId, amount, messageString);
	}

	public void kick(final String messageString) {
		minecraftAPI.kick(playerId, messageString);
	}

	public void kill(final String messageString) {
		minecraftAPI.kill(playerId, messageString);
	}

}
