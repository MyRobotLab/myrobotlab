#!/bin/bash

# ====================== VERSION ======================
VERSION="1.1.1555"

# ====================== BUILD ======================
mvn -DskipTests -Dversion=$VERSION clean package

# Define variables
BUCKET_NAME="myrobotlab-repo"
REGION="us-east-1"
JAR_FILE="target/myrobotlab.jar"
ZIP_FILE="target/myrobotlab.zip"
S3_PATH="s3://$BUCKET_NAME/artifactory/myrobotlab/org/myrobotlab/myrobotlab/$VERSION/"

# ====================== DEPLOY ======================
# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo "AWS CLI is not installed. Install it and try again."
    exit 1
fi

# Upload the JAR file without ACL
# echo "Uploading $JAR_FILE to $S3_PATH"
# aws s3 cp "$JAR_FILE" "$S3_PATH" --region "$REGION" --no-progress

# Upload the ZIP file without ACL
echo "Uploading $ZIP_FILE to $S3_PATH"
aws s3 cp "$ZIP_FILE" "$S3_PATH" --region "$REGION" --no-progress

curl -X POST \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: token $GITHUB_TOKEN" \
  -H "Content-Type: application/json" \
  https://api.github.com/repos/MyRobotLab/myrobotlab/releases \
  -d '{
    "tag_name": "'"$VERSION"'",
    "target_commitish": "develop",
    "name": "'"$VERSION Nixie"'",
    "body": "## MyRobotLab Nixie Release\r\n\r\nOpen Source Framework for Robotics and Creative Machine Control\r\n*You know, for robots!*\r\n\r\n* Project Website http://myrobotlab.org \r\n* Project Discord https://discord.gg/AfScp5x8r5\r\n* Download Built Application [Nixie '"$VERSION"'](https://myrobotlab-repo.s3.amazonaws.com/artifactory/myrobotlab/org/myrobotlab/myrobotlab/'"$VERSION"'/myrobotlab.zip)\r\n* [JavDocs](https://build.myrobotlab.org:8443/job/myrobotlab/job/develop/$build/artifact/target/site/apidocs/org/myrobotlab/service/package-summary.html)\r\n## Base Requirements\r\n\r\nYou will need Java 11 or newer. If you are only running MyRobotLab, you need the JRE (Java Runtime Environment.)  If you are going to be building from source, you will need the JDK (Java Development Kit) Oracle or OpenJDK will work.\r\n",
    "draft": false,
    "prerelease": false,
    "generate_release_notes": true
  }'

# Confirmation message
echo "Deployment complete!"
