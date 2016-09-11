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

package com.pwn9.filter.bungee.commands;

import com.pwn9.filter.bukkit.PwnFilterBukkitPlugin;
import com.pwn9.filter.engine.FilterService;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import org.bukkit.ChatColor;

/**
 * @author Cory Redmond &lt;ace@ac3-servers.eu&gt;
 */
public class PwnFilterReloadCommand extends Command {

	public PwnFilterReloadCommand() {
		super( "pfreload", "pwnfilter.reload" );
	}

	@Override
	public void execute( CommandSender sender, String[] args ) {

		FilterService filterService = PwnFilterBukkitPlugin.getInstance().getFilterService();

		sender.sendMessage( ChatColor.RED + "Reloading config.yml and rules/*.txt files.");

		filterService.getLogger().info("Disabling all listeners");
		filterService.disableClients();

		if (!PwnFilterBukkitPlugin.getInstance().configurePlugin()) return;

		filterService.getLogger().config("Reloaded config.yml as requested by " + sender.getName());
		filterService.getLogger().config("All rules reloaded by " + sender.getName());

		// Re-register our listeners
		filterService.enableClients();
		filterService.getLogger().info("All listeners re-enabled");

	}

}
