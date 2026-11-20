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
Os comandos estão em [k8s/observability/install.md](k8s/observability/install.md).
```
kubectl port-forward -n monitoring svc/grafana 3000:80
```
Grafana em http://localhost:3000.

## Evidências

### Cluster Minikube iniciado
![minikube start](prints/01-minikube-start.png)

### Nó pronto (`kubectl get nodes`)
![nodes](prints/02-kubectl-get-nodes.png)

### Banco de dados: manifestos aplicados
![banco apply](prints/03-banco-apply.png)

### Banco de dados rodando
![banco running](prints/04-banco-pod-running.png)

### Imagem do CRM carregada no Minikube
![imagem](prints/05-imagem-crm-no-minikube.png)

### Aplicação com 2 réplicas ativas + banco (`kubectl get pods`)
![pods](prints/06-pods-app-2-replicas-e-banco.png)

### URL do Service exposta pelo Minikube
![service url](prints/07-minikube-service-url.png)

### Aplicação respondendo no navegador
![navegador](prints/08-aplicacao-no-navegador.png)

### Tabelas criadas pela aplicação no PostgreSQL
![tabelas](prints/09-tabelas-no-banco.png)

### Login funcionando (consulta ao banco, retorna token)
![login](prints/10-login-retorna-token.png)

### Helm: Prometheus e Grafana instalados
![helm list](prints/11-helm-list-prometheus-grafana.png)

### Stack de observabilidade rodando (Grafana, Loki, Promtail, Tempo, Prometheus)
![monitoring](prints/12-pods-observabilidade-running.png)