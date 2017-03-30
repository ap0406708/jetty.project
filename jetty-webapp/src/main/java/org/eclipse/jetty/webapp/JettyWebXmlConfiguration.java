//
//  ========================================================================
//  Copyright (c) 1995-2017 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.webapp;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlConfiguration;


/**
 *
 * JettyWebConfiguration.
 *
 * Looks for XmlConfiguration files in WEB-INF.  Searches in order for the first of jetty6-web.xml, jetty-web.xml or web-jetty.xml
 *
 *
 *
 */
public class JettyWebXmlConfiguration extends AbstractConfiguration
{
    private static final Logger LOG = Log.getLogger(JettyWebXmlConfiguration.class);

    public static final String XML_CONFIGURATION = "org.eclipse.jetty.webapp.JettyWebXmlConfiguration";
    public static final String JETTY_WEB_XML = "jetty-web.xml";

    /**
     * Configure
     * Apply web-jetty.xml configuration
     * @see Configuration#configure(WebAppContext)
     */
    @Override
    public void configure (WebAppContext context) throws Exception
    {
        //cannot configure if the _context is already started
        if (context.isStarted())
        {
            LOG.debug("Cannot configure webapp after it is started");
            return;
        }

        LOG.debug("Configuring web-jetty.xml");

        Resource web_inf = context.getWebInf();
        // handle any WEB-INF descriptors
        if(web_inf!=null&&web_inf.isDirectory())
        {
            // do jetty.xml file
            Resource jetty=web_inf.addPath("jetty8-web.xml");
            if(!jetty.exists())
                jetty=web_inf.addPath(JETTY_WEB_XML);
            if(!jetty.exists())
                jetty=web_inf.addPath("web-jetty.xml");

            if(jetty.exists())
            {             
                if(LOG.isDebugEnabled())
                    LOG.debug("Configure: "+jetty);

                Object xml_attr=context.getAttribute(XML_CONFIGURATION);
                context.removeAttribute(XML_CONFIGURATION);
                
                final XmlConfiguration jetty_config = xml_attr instanceof XmlConfiguration
                    ?(XmlConfiguration)xml_attr
                    :new XmlConfiguration(jetty.getURI().toURL());
                setupXmlConfiguration(jetty_config, web_inf);
                
                try
                {
                    final XmlConfiguration config=jetty_config;
                    context.runWithoutCheckingServerClasses(new Callable<Void>()
                    {
                        @Override
                        public Void call() throws Exception
                        {
                            config.configure(context);
                            return null;
                        }
                    });
                }
                catch(Exception e)
                {
                    LOG.warn("Error applying {}",jetty);
                    throw e;
                }
            }
        }
    }

    /**
     * Configures some well-known properties before the XmlConfiguration reads
     * the configuration.
     * @param jetty_config The configuration object.
     * @param web_inf the WEB-INF location
     */
    private void setupXmlConfiguration(XmlConfiguration jetty_config, Resource web_inf) throws IOException
    {
        Map<String,String> props = jetty_config.getProperties();
        props.put("this.web-inf.url", web_inf.getURI().toURL().toExternalForm());
        String webInfPath = web_inf.getFile().getAbsolutePath();
        if (!webInfPath.endsWith(File.separator))
        {
            webInfPath += File.separator;
        }
        props.put("this.web-inf.path", webInfPath);
    }
}
