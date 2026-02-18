#!/bin/bash

# Configuración
BACKUP_DIR="./backups"
CONTAINER_NAME="tears-db"
DB_USER="postgres"
DB_NAME="tearsmind"
DATE=$(date +"%Y%m%d_%H%M%S")
FILENAME="$BACKUP_DIR/tearsmind_backup_$DATE.sql.gz"

# Crear directorio si no existe
mkdir -p $BACKUP_DIR

echo "🔒 Iniciando Backup de TearsDeepMind ($DATE)..."

# Ejecutar pg_dump dentro del contenedor y comprimir
docker exec -t $CONTAINER_NAME pg_dump -U $DB_USER $DB_NAME | gzip > $FILENAME

# Verificar éxito
if [ ${PIPESTATUS[0]} -eq 0 ]; then
    echo "✅ Backup exitoso: $FILENAME"
    echo "📦 Tamaño: $(du -h $FILENAME | cut -f1)"
    
    # Limpieza: Borrar backups locales mayores a 7 días
    find $BACKUP_DIR -name "tearsmind_backup_*.sql.gz" -mtime +7 -delete
    echo "🧹 Backups antiguos (>7 días) eliminados."
else
    echo "❌ Error al generar el backup."
    rm -f $FILENAME
    exit 1
fi
