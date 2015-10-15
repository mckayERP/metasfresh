package org.adempiere.webui.event;

/*
 * #%L
 * de.metas.adempiere.adempiere.zkwebui
 * %%
 * Copyright (C) 2015 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */


import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;

/**
 * ZK Component that supports invokeLater events 
 * @author tsa
 *
 */
public interface ZkInvokeLaterComponent extends Component
{
	/**
	 * Method called when an invokeLater event arrives.
	 * In a normal case, please just call {@link ZkInvokeLaterSupport#onInvokeLater(Event)} method
	 * @param event
	 * @see ZkInvokeLaterSupport#onInvokeLater(Event)
	 */
	public void onInvokeLater(Event event);
	
	/**
	 * 
	 * @return {@link ZkInvokeLaterSupport} for this component
	 */
	public ZkInvokeLaterSupport getInvokeLaterSupport();
}