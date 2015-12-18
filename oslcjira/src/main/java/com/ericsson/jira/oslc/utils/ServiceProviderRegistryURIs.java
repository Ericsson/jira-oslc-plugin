package com.ericsson.jira.oslc.utils;

import java.net.InetAddress;
import java.util.logging.Logger;

public final class ServiceProviderRegistryURIs
{
    private static final Logger LOGGER = Logger.getLogger(ServiceProviderRegistryURIs.class.getName());

    private static final String SYSTEM_PROPERTY_NAME_REGISTRY_URI = ServiceProviderRegistryURIs.class.getPackage().getName() + ".registryuri";
    private static final String SYSTEM_PROPERTY_NAME_UI_URI       = ServiceProviderRegistryURIs.class.getPackage().getName() + ".uiuri";

    private static final String SERVICE_PROVIDER_REGISTRY_URI;
    private static final String UI_URI;

    
    static
    {
        final String registryURI = System.getProperty(SYSTEM_PROPERTY_NAME_REGISTRY_URI);
        final String uiURI       = System.getProperty(SYSTEM_PROPERTY_NAME_UI_URI);

        System.out.println("SYSTEM_PROPERTY_NAME_REGISTRY_URI = " + SYSTEM_PROPERTY_NAME_REGISTRY_URI);
        System.out.println("SYSTEM_PROPERTY_NAME_UI_URI = " + SYSTEM_PROPERTY_NAME_UI_URI);
        
        String defaultBase = null;

        if ((registryURI == null) ||
            (uiURI == null))
        {
            // We need at least one default URI

            String hostName = "localhost";

            try
            {
                hostName = InetAddress.getLocalHost().getCanonicalHostName();
            }
            catch (final Exception exception)
            {
                // Default to localhost
            }

            defaultBase = "http://" + hostName + ":8080/";
        }

        if (registryURI != null)
        {
            SERVICE_PROVIDER_REGISTRY_URI = registryURI;
        }
        else
        {
            // In order to force Jena to show SPC first in XML, add a bogus identifier to the SPC URI.
            // This is because Jena can show an object anywhere in its graph where it is referenced.  Since the
            // SPC URI (without tailing identifier) is the same as its QueryCapability's queryBase, it can
            // be strangely rendered with the SPC nested under the queryBase.
            // This also allows us to distinguish between array and single results within the ServiceProviderCatalogResource.
            SERVICE_PROVIDER_REGISTRY_URI = defaultBase + "OSLC4JRegistry/catalog/singleton";

            LOGGER.warning("System property '" + SYSTEM_PROPERTY_NAME_REGISTRY_URI + "' not set.  Using calculated value '" + SERVICE_PROVIDER_REGISTRY_URI + "'");
        }

        if (uiURI != null)
        {
            UI_URI = uiURI;
        }
        else
        {
            UI_URI = defaultBase + "OSLC4JUI";

            LOGGER.warning("System property '" + SYSTEM_PROPERTY_NAME_UI_URI + "' not set.  Using calculated value '" + UI_URI + "'");
        }
    }

    private ServiceProviderRegistryURIs()
    {
        super();
    }

    public static String getServiceProviderRegistryURI()
    {
    	System.out.println("SERVICE_PROVIDER_REGISTRY_URI = " + SERVICE_PROVIDER_REGISTRY_URI);
        return SERVICE_PROVIDER_REGISTRY_URI;
    }

    public static String getUIURI()
    {
    	System.out.println("UI_URI = " + UI_URI);
        return UI_URI;
    }
}
