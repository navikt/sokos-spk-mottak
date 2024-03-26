#!/bin/bash
export VAULT_ADDR=https://vault.adeo.no

# Ensure user is authenicated, and run login if not.
gcloud auth print-identity-token &> /dev/null
if [ $? -gt 0 ]; then
    gcloud auth login
fi
kubectl config use-context dev-fss
kubectl config set-context --current --namespace=okonomi

# Get env from vault
[[ "$(vault token lookup -format=json | jq '.data.display_name' -r; exit ${PIPESTATUS[0]})" =~ "nav.no" ]] &>/dev/null || vault login -method=oidc -no-print
PRIVATE_KEY=$(vault read -field=privateKey kv/preprod/fss/sokos-spk-mottak/okonomi/sftp)
HOST_KEY=$(vault read -field=localDevHostKey kv/preprod/fss/sokos-spk-mottak/okonomi/sftp)
# Get AZURE system variables
envValue=$(kubectl exec -it $(kubectl get pods | grep sokos-spk-mottak | cut -f1 -d' ') -c sokos-spk-mottak -- env | egrep "^AZURE|^DATABASE|^SFTP|SPK_SFTP_PASSWORD|SPK_SFTP_USERNAME")

# Set AZURE as local environment variables
rm -f defaults.properties
echo "$envValue" > defaults.properties
echo "AZURE stores as defaults.properties"
rm -f privKey
echo "$PRIVATE_KEY" > privKey

sed -i '' '/^SFTP_SERVER=/ s/=.*/=155.55.161.44/' defaults.properties
sed -i '' '/^SFTP_PRIVATE_KEY_FILE_PATH=/ s/=.*/=privKey/' defaults.properties