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

import com.pwn9.filter.engine.FilterService;
import com.pwn9.filter.engine.api.FilterClient;
import com.pwn9.filter.engine.rules.chain.Chain;
import com.pwn9.filter.engine.rules.chain.InvalidChainException;
import com.pwn9.filter.engine.rules.chain.RuleChain;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Listener;

import java.io.File;

/**
 * @author Cory Redmond &lt;ace@ac3-servers.eu&gt;
 */
abstract class BaseListener<E extends Event> implements FilterClient, Listener {
	protected final FilterService filterService;
	volatile RuleChain ruleChain;
	private boolean active;
	private byte priority = 0;

	BaseListener(FilterService filterService) {
		this.filterService = filterService;
	}

	RuleChain getCompiledChain(File ruleFile) throws InvalidChainException {
		Chain newChain = filterService.parseRules(ruleFile);
		return (RuleChain) newChain;
	}

	@Override
	public FilterService getFilterService() {
		return filterService;
	}

	@Override
	public RuleChain getRuleChain() {
		return ruleChain;
	}

	public void loadRuleChain(File path) throws InvalidChainException {
		ruleChain = getCompiledChain(path);
	}

	void loadRuleChain(String name) throws InvalidChainException {
		ruleChain = getCompiledChain(filterService.getConfig().getRuleFile(name));
	}


	@Override
	public boolean isActive() {
		return active;
	}

	void setActive() {
		active = true;
	}

	void setInactive() {
		active = false;
	}

	/**
	 * Shutdown this listener.  This method can be called either by the owning plugin
	 * or by PwnFilter.  PwnFilter will call the activate / shutdown methods when PwnFilter
	 * is enabled / disabled and whenever it is reloading its config / rules.
	 * <p>
	 * These methods could either register / deregister the listener with Bukkit, or
	 * they could just enable / disable the use of the filter.
	 */
	@Override
	public void shutdown() {
		if (active) {
			setInactive();
		}
	}

	public void setPriority(byte priority) {
		this.priority = priority;
	}

	public byte getPriority() {
		return this.priority;
	}

	public void doEvent( E event, byte priority ) {

		if( priority == getPriority() ) eventProcessor( event );

	}

	abstract void eventProcessor(E event);

	public static String getPriorityString( byte priority ) {

		switch ( priority) {

			case -64:
				return "LOWEST";
			case -32:
				return "LOW";
			case 0:
				return "NORMAL";
			case 32:
				return "HIGH";
			case 64:
				return "HIGHEST";
			default:
				return String.valueOf( priority );

		}

	}

}
