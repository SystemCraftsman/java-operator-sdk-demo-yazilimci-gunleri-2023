# Java Operator SDK Demo for JavaDay Istanbul 2023

Demo for the Developing Kubernetes Operators with Java Operator SDK talk in JavaDay Istanbul 2023

## Architecture

![Architecture](https://user-images.githubusercontent.com/10568159/236544627-afd5a1e1-a12e-420a-9980-028f53d8055e.png)

## Steps:

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
k apply -f examples/readyPlayerOne/
```

```postgresql
\dt
```

```postgresql
select * from world;
```

