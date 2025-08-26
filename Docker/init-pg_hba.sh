#!/bin/bash
set -e

# Modify postgresql.conf to set listen_addresses to '*'
echo "listen_addresses = '*'" >> /var/lib/postgresql/data/postgresql.conf

# Modify pg_hba.conf to allow connections from any host with trust authentication
# Remove any existing conflicting lines
sed -i '/host all all all/d' /var/lib/postgresql/data/pg_hba.conf

# Add our trust authentication rules at the end
echo "host all all 0.0.0.0/0 trust" >> /var/lib/postgresql/data/pg_hba.conf
echo "host all all 172.18.0.0/16 trust" >> /var/lib/postgresql/data/pg_hba.conf

# Reload PostgreSQL configuration
pg_ctl reload -D /var/lib/postgresql/data