package com.crawljax.web;

import java.io.File;
import java.util.concurrent.*;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.crawljax.web.di.CrawljaxWebModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;

public class CrawljaxServer implements Callable<Void> {

	private static final Logger LOG = LoggerFactory.getLogger(CrawljaxServer.class);

	private Server server;
	private final CountDownLatch isRunningLatch = new CountDownLatch(1);
	private String url = null;

	public CrawljaxServer(int port, File outputDir) {
		setupJulToSlf4();
		server = new Server(port);
		Handler webAppContext = setupWebContext(outputDir);
		server.setHandler(webAppContext);
	}

	public CrawljaxServer(int port) {
		setupJulToSlf4();
		server = new Server(port);
		Handler webAppContext = setupWebContext(new File("out"));
		server.setHandler(webAppContext);
	}

	@Override
	public Void call() {
		start(true);
		return null;
	}

	/**
	 * @param join
	 *            If you want to merge with the remote thread.
	 * @see Thread#join()
	 */
	public void start(boolean join) {

		LOG.info("Starting the server");
		try {
			server.start();
			LOG.info("Server started");
		} catch (Exception e) {
			throw new RuntimeException("Could not start Crawljax web server", e);
		}

		int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort(); //Value will change if originally set to 0
		url = "http://localhost:" + port;

		isRunningLatch.countDown();

		if(join) {
			LOG.debug("Joining server thread");
			try {
				server.join();
			} catch (InterruptedException e) {
				throw new RuntimeException("Crawljax server interrupted", e);
			}
		}
	}

	public void waitUntilRunning(long timeOut_ms) {
		try {
			isRunningLatch.await(timeOut_ms, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void stop() {
		LOG.info("Stopping the server");
		try {
			server.stop();
			LOG.info("Shutdown complete");
		} catch (Exception e) {
			throw new RuntimeException("Could not stop Crawljax web server", e);
		}
	}

	public String getUrl() {
		return url;
	}

	public int getPort() {
		return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
	}

	private void setupJulToSlf4() {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	private WebAppContext setupWebContext(final File outputFolder) {
		WebAppContext webAppContext = new WebAppContext();
		webAppContext.setContextPath("/");
		webAppContext.setBaseResource(Resource.newClassPathResource("web"));
		webAppContext.setParentLoaderPriority(true);
		webAppContext.addEventListener(new GuiceServletContextListener() {

			@Override
			protected Injector getInjector() {
				return Guice.createInjector(new CrawljaxWebModule(outputFolder));
			}

		});

		webAppContext.addFilter(GuiceFilter.class, "/*", null);
		return webAppContext;
	}

}
