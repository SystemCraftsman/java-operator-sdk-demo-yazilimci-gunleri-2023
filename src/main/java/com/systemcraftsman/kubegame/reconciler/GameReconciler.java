package com.systemcraftsman.kubegame.reconciler;

import com.systemcraftsman.kubegame.customresource.Game;
import com.systemcraftsman.kubegame.service.domain.GameService;
import com.systemcraftsman.kubegame.status.GameStatus;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Objects;

public class GameReconciler implements Reconciler<Game> {

    @Inject
    GameService gameService;

    @Override
    public UpdateControl<Game> reconcile(Game resource, Context context) {

        Deployment postgresDeployment = gameService.getPostgresDeployment(resource);
        Service postgresService = gameService.getPostgresService(resource);

        if (postgresDeployment == null) {
            postgresDeployment = gameService.createPostgresDeployment(resource);
        }

        if (postgresService == null) {
            gameService.createPostgresService(resource);
        }

        if(postgresDeployment.getStatus() != null &&
                Objects.equals(postgresDeployment.getStatus().getReadyReplicas(), postgresDeployment.getStatus().getReplicas())){
            GameStatus status = resource.getStatus();
            status.setReady(true);
            status.setMsg("All dependencies are ready");

            return UpdateControl.updateStatus(resource);
        }

        return UpdateControl.<Game>noUpdate().rescheduleAfter(Duration.ofSeconds(5));
    }
}

