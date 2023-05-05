package com.systemcraftsman.kubegame.service.domain;

import com.systemcraftsman.kubegame.customresource.Game;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.configuration.ProfileManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class GameService {

    private static final String POSTGRES_SUFFIX = "-postgres";
    public static final int POSTGRES_DB_PORT = 5432;

    @Inject
    KubernetesClient client;

    public Game getGame(String name, String namespace) {
        return client.resources(Game.class)
                .inNamespace(namespace)
                .withName(name)
                .get();
    }

    public Deployment getPostgresDeployment(Game game) {
        return client.apps()
                .deployments()
                .inNamespace(game.getMetadata().getNamespace())
                .withName(game.getMetadata().getName() + POSTGRES_SUFFIX)
                .get();
    }

    public Service getPostgresService(Game game) {
        Service service = client.services()
                .inNamespace(game.getMetadata().getNamespace())
                .withName(game.getMetadata().getName() + POSTGRES_SUFFIX).get();

        return service;
    }

    public Deployment createPostgresDeployment(Game game) {
        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName(game.getMetadata().getName() + POSTGRES_SUFFIX)
                .withNamespace(game.getMetadata().getNamespace())
                .endMetadata()
                .withNewSpec()
                .withSelector(
                        new LabelSelectorBuilder().withMatchLabels(getLabels(game)).build())
                .withTemplate(
                        new PodTemplateSpecBuilder()
                                .withMetadata(
                                        new ObjectMetaBuilder().withLabels(getLabels(game)).build())
                                .withSpec(
                                        new PodSpecBuilder()
                                                .withContainers(
                                                        new ContainerBuilder()
                                                                .withEnv(
                                                                        new EnvVarBuilder()
                                                                                .withName("POSTGRES_DB")
                                                                                .withValue("postgres")
                                                                                .build(),
                                                                        new EnvVarBuilder()
                                                                                .withName("POSTGRES_USER")
                                                                                .withValue(game.getSpec().getDatabase().getUsername())
                                                                                .build(),
                                                                        new EnvVarBuilder()
                                                                                .withName("POSTGRES_PASSWORD")
                                                                                .withValue(game.getSpec().getDatabase().getPassword())
                                                                                .build(),
                                                                        new EnvVarBuilder()
                                                                                .withName("PGDATA")
                                                                                .withValue("/temp/data")
                                                                                .build()
                                                                )
                                                                .withImage("postgres:10.1")
                                                                .withName("postgres")
                                                                .withPorts(
                                                                        new ContainerPortBuilder()
                                                                                .withContainerPort(POSTGRES_DB_PORT)
                                                                                .withProtocol("TCP")
                                                                                .build())
                                                                .build())
                                                .build())
                                .build())

                .endSpec()
                .build();
        deployment.addOwnerReference(game);
        client.apps().deployments().create(deployment);

        return deployment;
    }

    public Service createPostgresService(Game game) {
        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(game.getMetadata().getName() + POSTGRES_SUFFIX)
                .withNamespace(game.getMetadata().getNamespace())
                .endMetadata()
                .withNewSpec()
                .withSelector(getLabels(game))
                .addNewPort()
                .withProtocol("TCP")
                .withPort(POSTGRES_DB_PORT)
                .withTargetPort(new IntOrString(POSTGRES_DB_PORT))
                .endPort()
                .withType("ClusterIP")
                .endSpec()
                .build();

        service.addOwnerReference(game);
        client.services().resource(service).create();

        if(service != null && ProfileManager.getActiveProfile() != "prod") {
            client.services().resource(service).portForward(POSTGRES_DB_PORT, POSTGRES_DB_PORT);
        }

        return service;
    }

    public String getPostgresServiceName(Game game){
        if(ProfileManager.getActiveProfile() != "prod") {
            return "localhost";
        }
        return game.getMetadata().getName() + POSTGRES_SUFFIX;
    }

    private Map<String, String> getLabels(Game game) {
        Map<String, String> labels = new HashMap<>();
        labels.put("game", game.getMetadata().getName());
        return labels;
    }

}
