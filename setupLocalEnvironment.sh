#!/bin/bash
export VAULT_ADDR=https://vault.adeo.no

# Ensure user is authenicated, and run login if not.
gcloud auth print-identity-token &> /dev/null
if [ $? -gt 0 ]; then
    gcloud auth login
fi
kubectl config use-context dev-fss
kubectl config set-context --current --namespace=okonomi

# Get database username and password secret from Vault
[[ "$(vault token lookup -format=json | jq '.data.display_name' -r; exit ${PIPESTATUS[0]})" =~ "nav.no" ]] &>/dev/null || vault login -method=oidc -no-print

# Get AZURE system variables
envValue=$(kubectl exec -it $(kubectl get pods | grep sokos-spk-mottak | cut -f1 -d' ') -c sokos-spk-mottak -- env | egrep "^AZURE|^DATABASE|^SFTP|SPK_SFTP_PASSWORD|SPK_SFTP_USERNAME|VAULT_MOUNTPATH")
PRIVATE_KEY=$(kubectl get secret spk-sftp-private-key -o jsonpath='{.data.spk-sftp-private-key}' | base64 --decode)

POSTGRES_USER=$(vault kv get -field=data postgresql/preprod-fss/creds/sokos-spk-mottak-user)
POSTGRES_ADMIN=$(vault kv get -field=data postgresql/preprod-fss/creds/sokos-spk-mottak-admin)

MQ_SERVICE_PASSWORD=$(vault kv get -field=password serviceuser/dev/srvmotmq)

# Set AZURE as local environment variables
rm -f defaults.properties
echo "$envValue" > defaults.properties
echo "AZURE stores as defaults.properties"

username=$(echo "$POSTGRES_USER" | awk -F 'username:' '{print $2}' | awk '{print $1}' | sed 's/]$//')
password=$(echo "$POSTGRES_USER" | awk -F 'password:' '{print $2}' | awk '{print $1}' | sed 's/]$//')
echo "POSTGRES_USER_USERNAME=$username" >> defaults.properties
echo "POSTGRES_USER_PASSWORD=$password" >> defaults.properties

username=$(echo "$POSTGRES_ADMIN" | awk -F 'username:' '{print $2}' | awk '{print $1}' | sed 's/]$//')
password=$(echo "$POSTGRES_ADMIN" | awk -F 'password:' '{print $2}' | awk '{print $1}' | sed 's/]$//')
echo "POSTGRES_ADMIN_USERNAME=$username" >> defaults.properties
echo "POSTGRES_ADMIN_PASSWORD=$password" >> defaults.properties

echo "MQ_SERVICE_PASSWORD=$MQ_SERVICE_PASSWORD" >> defaults.properties


rm -f privateKey
echo "$PRIVATE_KEY" > privateKey

sed -i '' '/^SFTP_SERVER=/ s/=.*/=155.55.161.44/' defaults.properties
sed -i '' '/^SFTP_PRIVATE_KEY_FILE_PATH=/ s/=.*/=privateKey/' defaults.properties


