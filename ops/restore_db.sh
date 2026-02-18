#!/bin/bash

BACKUP_FILE=$1
CONTAINER_NAME="tears-db"
DB_USER="postgres"
DB_NAME="tearsmind"

if [ -z "$BACKUP_FILE" ]; then
    echo "❌ Uso: ./ops/restore_db.sh <archivo_backup.sql.gz>"
    exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo "❌ Archivo no encontrado: $BACKUP_FILE"
    exit 1
fi

echo "⚠️  ADVERTENCIA: ESTO BORRARÁ LA BASE DE DATOS ACTUAL Y RESTAURARÁ DESDE '$BACKUP_FILE'."
read -p "¿Estás seguro? (s/N) " confirm

if [[ $confirm != "s" && $confirm != "S" ]]; then
    echo "Cancelado."
    exit 0
fi

echo "🔻 Deteniendo la aplicación (tears-app)..."
docker stop tears-app

echo "♻️  Restaurando base de datos..."
# Borrar y recrear BD para asegurar limpieza
docker exec -i $CONTAINER_NAME psql -U $DB_USER -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;"
docker exec -i $CONTAINER_NAME psql -U $DB_USER -d postgres -c "CREATE DATABASE $DB_NAME;"

# Restaurar desde el backup
gunzip -c "$BACKUP_FILE" | docker exec -i $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME

if [ $? -eq 0 ]; then
    echo "✅ Restauración completada."
else
    echo "❌ Error en la restauración."
    # Reiniciar app de todos modos
    docker start tears-app
    exit 1
fi

echo "🔺 Reiniciando la aplicación (tears-app)..."
docker start tears-app
echo "✅ Sistema operativo."
