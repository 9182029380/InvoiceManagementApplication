Deployment files and quickstart for Kubernetes

Files created:
- Dockerfile: Multi-stage build for the Spring Boot app.
- docker-compose.yml: Local compose with MySQL and the app.
- k8s/namespace.yaml: Namespace to isolate resources.
- k8s/secret.yaml: Template secret (contains DB and mail credentials). Prefer creating via kubectl create secret.
- k8s/deployment.yaml: Deployment for the app (uses image invoice-management:latest). Update image to your registry/tag.
- k8s/service.yaml: ClusterIP service for the app and a service for MySQL.
- k8s/mysql-deployment.yaml: MySQL deployment (for local clusters). For production, use managed DB or StatefulSet + PVC.
- k8s/ingress.yaml: Ingress template (NGINX) — replace host and ensure an ingress controller is installed.

Quick local Docker + Compose steps
1. Build and run with docker-compose (local dev):

```powershell
# build and start
docker-compose build --pull
docker-compose up -d

# tail logs
docker-compose logs -f app
```

2. If you prefer to build a local image and run manually:

```powershell
# Build image
docker build -t invoice-management:latest .
# Run (links to local MySQL or update env)
docker run -p 9091:9091 -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/invoicemanagementdb" -e SPRING_DATASOURCE_USERNAME=root -e SPRING_DATASOURCE_PASSWORD=root invoice-management:latest
```

Kubernetes deployment steps (recommended flow)
1. Build the image and push to a container registry accessible by your cluster (Docker Hub, ECR, GCR, etc.):

```powershell
# Example with Docker Hub
docker build -t yourdockerhubuser/invoice-management:latest .
docker push yourdockerhubuser/invoice-management:latest
```

2. Prepare namespace and secrets. Create namespace and secrets using kubectl (recommended to avoid storing secrets in Git):

```powershell
kubectl apply -f k8s/namespace.yaml
kubectl create secret generic invoice-secrets \
  --namespace=invoice-management \
  --from-literal=datasource.username=root \
  --from-literal=datasource.password=root \
  --from-literal=mail.username=you@example.com \
  --from-literal=mail.password='YOUR_SMTP_PASSWORD'
```

3. Update `k8s/deployment.yaml` to point at your pushed image (e.g. yourdockerhubuser/invoice-management:latest).

4. Apply manifests:

```powershell
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/mysql-deployment.yaml
kubectl apply -f k8s/ingress.yaml   # optional
```

5. Verify:

```powershell
kubectl get pods -n invoice-management
kubectl logs -n invoice-management deploy/invoice-app
kubectl port-forward -n invoice-management svc/invoice-app-service 8080:80
# then open http://localhost:8080
```

Notes and recommendations
- For production use:
  - Use a managed DB (RDS, Cloud SQL) or a StatefulSet + PersistentVolumeClaim for MySQL.
  - Don't store sensitive values in YAML files; use Vault or Kubernetes Secrets created at deploy time.
  - Use resource requests/limits and configure readiness/liveness probes (templates provided).
  - Use an image scanning pipeline and CI to build and push images.

If you'd like, I can:
- Add a Helm chart to templatize the deployment and secrets.
- Add a PVC and a StatefulSet for MySQL.
- Wire an ingress TLS certificate (cert-manager) example.

