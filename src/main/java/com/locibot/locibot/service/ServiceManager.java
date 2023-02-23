package com.locibot.locibot.service;

import java.util.ArrayList;
import java.util.List;

public class ServiceManager {

    private final List<Service> services;

    public ServiceManager() {
        this.services = new ArrayList<>(3);
        this.services.addAll(List.of(
                new SentryService(),
                new PrometheusService()));
    }

    public void addService(Service service) {
        this.services.add(service);
    }

    public void addAllServices(List<Service> services) {
        this.services.addAll(services);
    }

    public void start() {
        this.services.stream()
                .filter(Service::isEnabled)
                .forEach(Service::start);
    }

    public void stop() {
        this.services.stream()
                .filter(Service::isEnabled)
                .forEach(Service::stop);
    }

}
