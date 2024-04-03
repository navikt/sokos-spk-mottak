#!/bin/bash

# Ensure user is authenicated, and run login if not.
gcloud auth print-identity-token &> /dev/null
if [ $? -gt 0 ]; then
    gcloud auth login
fi
kubectl config use-context dev-fss
kubectl config set-context --current --namespace=okonomi

# Get AZURE system variables
envValue=$(kubectl exec -it $(kubectl get pods | grep sokos-spk-mottak | cut -f1 -d' ') -c sokos-spk-mottak -- env | egrep "^AZURE|^DATABASE|^SFTP|SPK_SFTP_PASSWORD|SPK_SFTP_USERNAME")
PRIVATE_KEY=$(kubectl get secret spk-sftp-private-key -o jsonpath='{.data.spk-sftp-private-key}' | base64 --decode)

# Set AZURE as local environment variables
rm -f defaults.properties
echo "$envValue" > defaults.properties
echo "AZURE stores as defaults.properties"
rm -f privateKey
echo "$PRIVATE_KEY" > privateKey

sed -i '' '/^SFTP_SERVER=/ s/=.*/=155.55.161.44/' defaults.properties
sed -i '' '/^SFTP_PRIVATE_KEY_FILE_PATH=/ s/=.*/=privateKey/' defaults.properties