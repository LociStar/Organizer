package com.locibot.locibot.service;

import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.utils.LogUtil;
import io.prometheus.client.exporter.HTTPServer;
import org.jsoup.internal.StringUtil;
import reactor.util.Logger;

import java.io.IOException;

public class PrometheusService implements Service {

    private static final Logger LOGGER = LogUtil.getLogger(PrometheusService.class, LogUtil.Category.SERVICE);

    private final String port;

    private HTTPServer server;

    public PrometheusService() {
        this.port = CredentialManager.get(Credential.PROMETHEUS_PORT);
    }

    @Override
    public boolean isEnabled() {
        return !Config.IS_SNAPSHOT && !StringUtil.isBlank(this.port);
    }

    @Override
    public void start() {
        LOGGER.info("Starting Prometheus service on port {}", this.port);
        try {
            this.server = new HTTPServer(Integer.parseInt(this.port));
        } catch (final IOException err) {
            LOGGER.error("An error occurred while starting Prometheus service", err);
        }
    }

    @Override
    public void stop() {
        if (this.server != null) {
            LOGGER.info("Stopping Prometheus service");
            this.server.stop();
        }
    }
}
