package de.metas.adempiere.addon.impl;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.Adempiere;
import org.compiere.util.CLogger;

import de.metas.adempiere.addon.IAddOn;
import de.metas.adempiere.addon.IAddonStarter;

public final class AddonStarter implements IAddonStarter
{
	public static final String PROPS_RESOURCE = "/addons.properties";

	private static final CLogger logger = CLogger.getCLogger(AddonStarter.class);
	
	/**
	 * Shall we log a WARNING if the {@link #PROPS_RESOURCE} is missing.
	 */
	public static boolean warnIfPropertiesFileMissing = true;

	private final Properties props;

	public AddonStarter()
	{
		super();
		
		props = new Properties();
		try
		{
			//
			// Try to get the URL first (more for friendly error reporting, in case we don't find it)
			final URL url = Adempiere.class.getResource(PROPS_RESOURCE);
			if (url == null)
			{
				logger.log(warnIfPropertiesFileMissing ? Level.SEVERE : Level.INFO, "No properties file was found for " + PROPS_RESOURCE);
				return;
			}
			logger.info("Loading addons from " + url);

			//
			// Actually load the resource from stream
			final InputStream propsIn = Adempiere.class.getResourceAsStream(PROPS_RESOURCE);
			if (propsIn != null)
			{
				props.load(propsIn);
				return;
			}
		}
		catch (IOException e)
		{
			logger.saveError("Tried to load addon props from resource " + PROPS_RESOURCE, e);
		}
		logger.info("Resource file '" + PROPS_RESOURCE + "' couldn't not be loaded. Addons won't be started.");
	};

	AddonStarter(final Properties props)
	{
		super();
		this.props = props;
	}

	@Override
	public void startAddons()
	{
		for (final Object addonName : props.keySet())
		{
			final String addonClass = (String)props.get(addonName);

			logger.info("Starting addon " + addonName + " with  class " + addonClass);
			startAddon(addonClass);
		}
	}

	@Override
	public Properties getAddonProperties()
	{
		return props;
	}

	private static void startAddon(final String className)
	{
		try
		{
			final Class<?> clazz = Class.forName(className);

			final Class<? extends IAddOn> clazzVC = clazz
					.asSubclass(IAddOn.class);

			final IAddOn instance = clazzVC.newInstance();
			instance.initAddon();

		}
		catch (ClassNotFoundException e)
		{
			logger.saveError("Addon not available: " + className, e);
		}
		catch (ClassCastException e)
		{
			logger.saveError("Addon class " + className + " doesn't implement "
					+ IAddOn.class.getName(), e);
		}
		catch (InstantiationException e)
		{
			throw new RuntimeException(e);
		}
		catch (IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
	}

}