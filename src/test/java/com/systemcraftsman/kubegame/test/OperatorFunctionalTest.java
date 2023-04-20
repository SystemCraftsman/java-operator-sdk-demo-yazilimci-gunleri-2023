package com.systemcraftsman.kubegame.test;

import com.systemcraftsman.kubegame.customresource.Game;
import com.systemcraftsman.kubegame.customresource.World;
import com.systemcraftsman.kubegame.service.GameService;
import com.systemcraftsman.kubegame.service.PostgresService;
import com.systemcraftsman.kubegame.service.WorldService;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.lang3.ThreadUtils;
import org.junit.Assert;
import org.junit.jupiter.api.*;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OperatorFunctionalTest {

    private static final String NAMESPACE = "default";

    @Inject
    Operator operator;

    @Inject
    KubernetesClient client;

    @Inject
    private GameService gameService;

    @Inject
    private WorldService worldService;

    @Inject
    private PostgresService postgresService;

    @BeforeAll
    void startOperator() {
        operator.start();
    }

    @AfterAll
    void stopOperator() {
        operator.stop();
    }

    @Test
    @Order(1)
    void testGame() throws InterruptedException {
        client.resources(Game.class).inNamespace(NAMESPACE)
                .load(getClass().getResource("/examples/oasis.yaml").getFile()).create();

        Game game = gameService.getGame("oasis", NAMESPACE);

        Assert.assertNotNull(game);

        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            Deployment postgresDeployment = gameService.getPostgresDeployment(game);
            Assert.assertNotNull(postgresDeployment);
            Assert.assertEquals(Integer.valueOf(1), postgresDeployment.getStatus().getReadyReplicas());

            Assert.assertTrue(gameService.getGame(game.getMetadata().getName(), NAMESPACE).getStatus().isReady());
        });

    }

    @Test
    @Order(2)
    void testWorld() throws InterruptedException, SQLException {
        client.resources(World.class).inNamespace(NAMESPACE)
                .load(getClass().getResource("/examples/archaide.yaml").getFile()).create();
        client.resources(World.class).inNamespace(NAMESPACE)
                .load(getClass().getResource("/examples/incipio.yaml").getFile()).create();
        client.resources(World.class).inNamespace(NAMESPACE)
                .load(getClass().getResource("/examples/chthonia.yaml").getFile()).create();

        World worldArchaide = worldService.getWorld("archaide", NAMESPACE);
        World worldIncipio = worldService.getWorld("incipio", NAMESPACE);
        World worldChthonia = worldService.getWorld("chthonia", NAMESPACE);

        Assert.assertNotNull(worldArchaide);
        Assert.assertNotNull(worldIncipio);
        Assert.assertNotNull(worldChthonia);

        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            Assert.assertTrue(worldService.getWorld(worldArchaide.getMetadata().getName(), NAMESPACE).getStatus().isReady());
            Assert.assertTrue(worldService.getWorld(worldIncipio.getMetadata().getName(), NAMESPACE).getStatus().isReady());
            Assert.assertTrue(worldService.getWorld(worldChthonia.getMetadata().getName(), NAMESPACE).getStatus().isReady());
        });

        Game game = gameService.getGame(worldArchaide.getSpec().getGame(), worldArchaide.getMetadata().getNamespace());
        ResultSet resultSet = postgresService.executeQuery(gameService.getPostgresServiceName(game) + ":" + GameService.POSTGRES_DB_PORT,
                "postgres", game.getSpec().getDatabase().getUsername(), game.getSpec().getDatabase().getPassword(),
                "SELECT * FROM World WHERE game=?",
                game.getMetadata().getName());

        int resultCount = 0;
        while(resultSet.next()){
            resultCount++;
        }
        Assert.assertEquals(3, resultCount);

    }

    @Test
    @Order(3)
    void testDeletion() throws InterruptedException, SQLException {
        client.resources(World.class).inNamespace(NAMESPACE)
                .load(getClass().getResource("/examples/archaide.yaml").getFile()).delete();
        client.resources(World.class).inNamespace(NAMESPACE)
                .load(getClass().getResource("/examples/incipio.yaml").getFile()).delete();
        client.resources(World.class).inNamespace(NAMESPACE)
                .load(getClass().getResource("/examples/chthonia.yaml").getFile()).delete();

        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            Assert.assertNull(worldService.getWorld("archaide", NAMESPACE));
            Assert.assertNull(worldService.getWorld("incipio", NAMESPACE));
            Assert.assertNull(worldService.getWorld("chthonia", NAMESPACE));
        });

        client.resources(Game.class).inNamespace(NAMESPACE)
                .load(getClass().getResource("/examples/oasis.yaml").getFile()).delete();

        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            Assert.assertNull(gameService.getGame("oasis", NAMESPACE));
            Assert.assertNull(client.apps().deployments().inNamespace(NAMESPACE).withName("oasis-postgres").get());
            Assert.assertNull(client.services().inNamespace(NAMESPACE).withName("oasis-postgres").get());
        });
    }

}
