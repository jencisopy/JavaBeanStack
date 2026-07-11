#!/usr/bin/env bash
# Instala Java 25 LTS (Eclipse Temurin) vía el repositorio oficial de Adoptium.
# Compatible con Jakarta EE 11 (que exige JDK 17+, con soporte extendido desde JDK 21).
#
# Uso: correr en una terminal real (no a través de un chat/asistente), ya que
# pedirá la contraseña de sudo de forma interactiva.
#
#   bash instalar-jdk25.sh

set -euo pipefail

echo "== 1. Dependencias para agregar el repo por HTTPS =="
sudo apt-get update
sudo apt-get install -y wget apt-transport-https gpg

echo "== 2. Clave GPG y repositorio de Adoptium =="
sudo mkdir -p /etc/apt/keyrings
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list

echo "== 3. Instalar Temurin JDK 25 =="
sudo apt-get update
sudo apt-get install -y temurin-25-jdk

echo "== 4. Verificar =="
/usr/lib/jvm/temurin-25-jdk-amd64/bin/java -version

echo "== 5. Registrar como alternativa (opcional, permite elegirla con update-alternatives) =="
sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/temurin-25-jdk-amd64/bin/java 2500
sudo update-alternatives --config java

echo
echo "Instalación completa. Para que Maven use este JDK en JavaBeanStack, exportar:"
echo "  export JAVA_HOME=/usr/lib/jvm/temurin-25-jdk-amd64"
