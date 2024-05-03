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
envValue=$(kubectl exec -it $(kubectl get pods | grep sokos-spk-mottak | cut -f1 -d' ') -c sokos-spk-mottak -- env | egrep "^AZURE|^DATABASE|^SFTP|SPK_SFTP_PASSWORD|SPK_SFTP_USERNAME|^PENSJON|VAULT_MOUNTPATH")
PRIVATE_KEY=$(kubectl get secret spk-sftp-private-key -o jsonpath='{.data.spk-sftp-private-key}' | base64 --decode)
POSTGRES_ADMIN_USERNAME=$(vault kv get -field=username postgresql/preprod-fss/creds/sokos-spk-mottak-admin)
POSTGRES_ADMIN_PASSWORD=$(vault kv get -field=password postgresql/preprod-fss/creds/sokos-spk-mottak-admin)
POSTGRES_USER_USERNAME=$(vault kv get -field=username postgresql/preprod-fss/creds/sokos-spk-mottak-user)
POSTGRES_USER_PASSWORD=$(vault kv get -field=password postgresql/preprod-fss/creds/sokos-spk-mottak-user)

# Set AZURE as local environment variables
rm -f defaults.properties
echo "$envValue" > defaults.properties
echo "AZURE stores as defaults.properties"
echo "POSTGRES_ADMIN_USERNAME=$POSTGRES_ADMIN_USERNAME" >> defaults.properties
echo "POSTGRES_ADMIN_PASSWORD=$POSTGRES_ADMIN_PASSWORD" >> defaults.properties
echo "POSTGRES_USER_USERNAME=$POSTGRES_USER_USERNAME" >> defaults.properties
echo "POSTGRES_USER_PASSWORD=$POSTGRES_USER_PASSWORD" >> defaults.properties
rm -f privateKey
echo "$PRIVATE_KEY" > privateKey

sed -i '' '/^SFTP_SERVER=/ s/=.*/=155.55.161.44/' defaults.properties
sed -i '' '/^SFTP_PRIVATE_KEY_FILE_PATH=/ s/=.*/=privateKey/' defaults.properties


