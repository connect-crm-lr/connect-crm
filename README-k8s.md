# Connect CRM no Kubernetes (Minikube)

## Aplicação
Connect CRM: backend em Spring Boot (Java 21, Maven) com banco PostgreSQL.
A aplicação roda com 2 réplicas e se conecta ao banco pelo Service `banco-service`.

## Passo a passo

### 1. Subir o Minikube
```
minikube start --memory=3500mb --cpus=2
kubectl get nodes
```

### 2. Banco de dados
```
kubectl apply -f k8s/banco-secret.yaml
kubectl apply -f k8s/banco-deployment.yaml
kubectl apply -f k8s/banco-service.yaml
```

### 3. Aplicação
```
cd backend
.\mvnw.cmd package -DskipTests
docker build -t crm:1.1 .
minikube image load crm:1.1
cd ..
kubectl apply -f k8s/app-secret.yaml
kubectl apply -f k8s/app-deployment.yaml
kubectl apply -f k8s/app-service.yaml
kubectl get pods
```

### 4. Acessar a aplicação
```
minikube service app-service --url
```

### 5. Observabilidade (Helm, namespace `monitoring`)
Prometheus (métricas), Loki + Promtail (logs), Tempo (traces) e Grafana (painéis).
Os comandos estão em `k8s/observability/install.md`.
```
kubectl port-forward -n monitoring svc/grafana 3000:80
```
Grafana em http://localhost:3000.

## Evidências

### Pods rodando (2 réplicas da aplicação + banco)
![pods](prints/pods.png)

### Aplicação respondendo no navegador
![navegador](prints/navegador.png)

### Tabelas criadas no banco
![tabelas](prints/tabelas.png)

### Login funcionando (consulta ao banco)
![login](prints/login.png)

### Stack de observabilidade rodando
![monitoring](prints/monitoring.png)

### Grafana
![grafana](prints/grafana.png)