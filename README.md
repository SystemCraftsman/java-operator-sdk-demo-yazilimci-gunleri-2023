# Java Operator SDK Demo for Yazilimci Gunleri 2023

Demo for the Developing Kubernetes Operators with Java Operator SDK talk in Yazilimci Gunleri 2023

## Architecture

![Architecture](https://user-images.githubusercontent.com/10568159/236544627-afd5a1e1-a12e-420a-9980-028f53d8055e.png)

## Examining the Operator

* Examine the application.properties
* Examine the target/kubernetes
* Examine the custom resources and their specs
* Examine the reconcilers
* Examine the services

## Rough Steps

Make sure a K8S instance is running on your local.

Run the operator on your local.

Then run the following commands one by one:

```shell
k get pods -w
```

```shell
k apply -f examples/readyPlayerOne/oasis.yaml 
```

```shell
k get games
```

```shell
k get games oasis -o yaml
```

```shell
k exec -it oasis-postgres-956694c99-7gnhc -- bash
```

```shell
psql -U postgres
```

```postgresql
\c
```

```postgresql
\dt
```

```shell
k apply -f examples/readyPlayerOne/incipio.yaml
```

```postgresql
\dt
```

```postgresql
select * from world;
```

```shell
k apply -f examples/readyPlayerOne/
```

```postgresql
select * from world;
```

```shell
k delete games oasis
```

## Running the test

* Examine the `OperatorFunctionalTest`
* Run the test

