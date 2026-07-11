# Instalar Java 25 LTS (compatible con Jakarta EE 11)

Java 25 LTS (liberada en septiembre 2025) es la última versión LTS y es compatible con Jakarta EE 11, que exige como mínimo JDK 17 (con soporte extendido desde JDK 21).

Comandos para Ubuntu/Debian, usando el repositorio oficial de Eclipse Adoptium:

```bash
# 1. Dependencias para agregar el repo por HTTPS
sudo apt-get update
sudo apt-get install -y wget apt-transport-https gpg

# 2. Clave GPG y repositorio de Adoptium
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list

# 3. Instalar Temurin JDK 25
sudo apt-get update
sudo apt-get install -y temurin-25-jdk

# 4. Verificar
/usr/lib/jvm/temurin-25-jdk-amd64/bin/java -version

# 5. (Opcional) registrarla como alternativa para poder elegirla con update-alternatives
sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/temurin-25-jdk-amd64/bin/java 2500
sudo update-alternatives --config java
```

Para que Maven la use en este proyecto, exportar `JAVA_HOME` antes de compilar:

```bash
export JAVA_HOME=/usr/lib/jvm/temurin-25-jdk-amd64
```

## Fuentes

- [What Is the Latest Version of Java? (2026) | Java With Us](https://javawithus.com/faq/latest-version-of-java/)
- [JDK 25](https://openjdk.org/projects/jdk/25/)
- [Oracle Java SE Support Roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)
